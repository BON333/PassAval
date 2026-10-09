package EDAII_TP;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * CAMINHO MÍNIMO - ALGORITMO DE DIJKSTRA.
 *
 * Ideia: mantém dist[v] = melhor distância CONHECIDA da origem até v (começa em infinito).
 * Em cada passo "fecha" a cidade aberta com menor distância (essa distância já é definitiva,
 * porque os pesos são positivos) e RELAXA as suas arestas: se passar por ela encurta o caminho
 * até um vizinho, atualiza dist[] e prev[].
 * prev[v] guarda "de onde vim" e permite reconstruir o caminho de trás para a frente.
 * Complexidade: O((V + E) log V) com fila de prioridade.
 * Requisito: pesos NÃO negativos (aqui são inteiros positivos).
 *
 * A classe também expõe o núcleo (calcular) como método estático, reutilizado pelas rotas.
 */
public class CaminhoDijkstra implements Algoritmo {

    /** "Infinito" seguro: somar dois destes ou um peso não faz overflow de int. */
    public static final int INF = Integer.MAX_VALUE / 2;

    @Override public String nome() { return "Dijkstra (caminho mínimo)"; }

    @Override public String descricao() {
        return "Fila de prioridade + relaxamento de arestas. Complexidade O((V + E) log V). Pesos não negativos.";
    }

    @Override public boolean usaDestino() { return true; }

    @Override
    public Resultado executar(GrafoFerroviarioUcraniano g, int origem, int destino) {
        Resultado r = new Resultado(nome());
        long t0 = System.nanoTime();
        int n = g.getN();
        int[] dist = new int[n];
        int[] prev = new int[n];

        r.log.add("Inicialização: dist[" + g.getNome(origem) + "] = 0 e todas as outras = INF");
        calcular(g, origem, dist, prev, r.log);

        if (dist[destino] >= INF) {
            r.encontrado = false;
            r.mensagemErro = "Não existe caminho entre " + g.getNome(origem) + " e " + g.getNome(destino)
                    + " (estão em componentes diferentes da rede).";
            r.log.add(r.mensagemErro);
        } else {
            List<Integer> cam = reconstruir(prev, origem, destino);
            r.caminho.addAll(cam);
            r.custoTotal = dist[destino];
            r.passos.add(Passo.no(origem, "Partida: " + g.getNome(origem)));
            int acumulado = 0;
            for (int k = 1; k < cam.size(); k++) {
                int a = cam.get(k - 1), b = cam.get(k), peso = g.getPeso(a, b);
                acumulado += peso;
                r.passos.add(Passo.mover(a, b, g.getNome(a) + " -> " + g.getNome(b)
                        + " (+" + peso + ", acumulado " + acumulado + " " + g.getUnidade() + ")"));
            }
            r.detalhe = (cam.size() - 1) + " ligações no percurso";
        }

        // Tabela final de distâncias (útil para explicar o resultado)
        r.log.add("Distâncias mínimas a partir de " + g.getNome(origem) + ":");
        for (int i = 0; i < n; i++) {
            r.log.add(String.format("   %-12s %s", g.getNome(i), dist[i] >= INF ? "inalcançável" : String.valueOf(dist[i])));
        }
        r.tempoNs = System.nanoTime() - t0;
        return r;
    }

    /**
     * Núcleo do algoritmo: preenche dist[] e prev[] para a origem src.
     * @param log lista onde acrescentar o passo a passo; pode ser null (sem registo).
     */
    public static void calcular(GrafoFerroviarioUcraniano g, int src, int[] dist, int[] prev, List<String> log) {
        int n = g.getN();
        Arrays.fill(dist, INF);
        Arrays.fill(prev, -1);
        boolean[] fechado = new boolean[n];
        PriorityQueue<int[]> fila = new PriorityQueue<int[]>((a, b) -> Integer.compare(a[1], b[1]));   // {vértice, dist}
        dist[src] = 0;
        fila.add(new int[]{src, 0});
        while (!fila.isEmpty()) {
            int u = fila.poll()[0];
            if (fechado[u]) continue;               // entrada antiga (já havia uma melhor): ignora
            fechado[u] = true;                      // distância de u é definitiva
            if (log != null) log.add(String.format("Fecha %-12s distância mínima = %d", g.getNome(u), dist[u]));
            for (Vizinho viz : g.getVizinhos(u)) {
                int v = viz.destino;
                if (!fechado[v] && dist[u] + viz.peso < dist[v]) {        // RELAXAMENTO
                    if (log != null) {
                        String antiga = dist[v] >= INF ? "INF" : String.valueOf(dist[v]);
                        log.add("     relaxa " + g.getNome(v) + ": " + antiga + " -> " + (dist[u] + viz.peso)
                                + "  (via " + g.getNome(u) + ")");
                    }
                    dist[v] = dist[u] + viz.peso;
                    prev[v] = u;
                    fila.add(new int[]{v, dist[v]});
                }
            }
        }
    }

    /** Reconstrói src -> dst seguindo prev[] de trás para a frente. Lista vazia se inalcançável. */
    public static List<Integer> reconstruir(int[] prev, int src, int dst) {
        LinkedList<Integer> cam = new LinkedList<Integer>();
        for (int x = dst; x != -1; x = prev[x]) cam.addFirst(x);
        if (cam.isEmpty() || cam.getFirst() != src) return new LinkedList<Integer>();
        return cam;
    }
}
