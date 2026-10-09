package EDAII_TP;

import java.util.LinkedList;
import java.util.Queue;

/**
 * BUSCA EM LARGURA (BFS - Breadth-First Search).
 *
 * Ideia: visita primeiro todas as cidades a 1 ligação da origem, depois as que estão a 2, etc.
 * (anda "por camadas"). Estrutura auxiliar: FILA (FIFO).
 * Complexidade: O(V + E) com listas de adjacências.
 *
 * Propriedade importante: num grafo sem pesos, a BFS dá o caminho com MENOS ligações.
 * Atenção: isso NÃO é o caminho mais barato quando as arestas têm pesos (para isso há o Dijkstra).
 * Custo apresentado = soma dos pesos das arestas da árvore BFS.
 */
public class BuscaBFS implements Algoritmo {

    @Override public String nome() { return "BFS (Busca em Largura)"; }

    @Override public String descricao() {
        return "Fila (FIFO). Complexidade O(V + E). Explora por camadas: primeiro as cidades a 1 ligação, depois a 2, etc.";
    }

    @Override public boolean usaDestino() { return false; }

    @Override
    public Resultado executar(GrafoFerroviarioUcraniano g, int origem, int destino) {
        Resultado r = new Resultado(nome());
        long t0 = System.nanoTime();
        int n = g.getN();
        boolean[] visitado = new boolean[n];
        int[] camada = new int[n];
        Queue<Integer> fila = new LinkedList<Integer>();

        visitado[origem] = true;                  // BFS marca ao DESCOBRIR (evita duplicados na fila)
        fila.add(origem);
        r.passos.add(Passo.no(origem, "Início em " + g.getNome(origem)));

        while (!fila.isEmpty()) {
            int u = fila.poll();
            r.caminho.add(u);
            r.log.add("Retira da fila: " + g.getNome(u) + "  (camada " + camada[u] + ")");
            for (Vizinho viz : g.getVizinhos(u)) {
                int v = viz.destino;
                if (!visitado[v]) {
                    visitado[v] = true;
                    camada[v] = camada[u] + 1;
                    fila.add(v);
                    r.custoTotal += viz.peso;
                    r.passos.add(Passo.mover(u, v, "Descobre " + g.getNome(v) + " a partir de " + g.getNome(u)
                            + " (peso " + viz.peso + ", camada " + camada[v] + ")"));
                    r.log.add("   descobre " + g.getNome(v) + " via " + g.getNome(u) + " (peso " + viz.peso
                            + ")   fila: " + filaTexto(g, fila));
                }
            }
        }

        r.detalhe = r.caminho.size() + " de " + n + " cidades alcançadas";
        if (r.caminho.size() < n) {
            StringBuilder sb = new StringBuilder("Não alcançadas a partir de " + g.getNome(origem) + " (rede desconexa): ");
            boolean primeiro = true;
            for (int i = 0; i < n; i++) {
                if (!visitado[i]) { sb.append(primeiro ? "" : ", ").append(g.getNome(i)); primeiro = false; }
            }
            r.log.add(sb.toString());
        }
        r.tempoNs = System.nanoTime() - t0;
        return r;
    }

    private String filaTexto(GrafoFerroviarioUcraniano g, Queue<Integer> q) {
        StringBuilder sb = new StringBuilder("[");
        boolean primeiro = true;
        for (int x : q) {
            sb.append(primeiro ? "" : ", ").append(g.getNome(x));
            primeiro = false;
        }
        return sb.append("]").toString();
    }
}
