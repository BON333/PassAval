package EDAII_TP;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * ÁRVORE GERADORA MÍNIMA - ALGORITMO DE PRIM.
 *
 * Ideia: começa numa cidade e faz crescer UMA única árvore; em cada passo junta a aresta
 * MAIS BARATA que liga a árvore a uma cidade que ainda está fora dela ("fronteira").
 * Estrutura auxiliar: fila de prioridade (min-heap) de arestas, ordenada pelo peso.
 * Complexidade: O(E log V).
 *
 * Se a rede for desconexa, obtém-se uma FLORESTA geradora mínima (uma árvore por componente).
 */
public class MstPrim implements Algoritmo {

    @Override public String nome() { return "MST - Prim"; }

    @Override public String descricao() {
        return "Fila de prioridade de arestas. Complexidade O(E log V). Cresce uma árvore escolhendo sempre a aresta mais barata da fronteira.";
    }

    @Override public boolean usaDestino() { return false; }

    @Override
    public Resultado executar(GrafoFerroviarioUcraniano g, int origem, int destino) {
        Resultado r = new Resultado(nome());
        long t0 = System.nanoTime();
        int n = g.getN();
        boolean[] naArvore = new boolean[n];
        int arestasUsadas = 0;

        // Começa em 'origem'; se a rede for desconexa, recomeça em cada componente que sobrar.
        List<Integer> inicios = new ArrayList<Integer>();
        inicios.add(origem);
        for (int i = 0; i < n; i++) if (i != origem) inicios.add(i);

        for (int s : inicios) {
            if (naArvore[s]) continue;
            if (s != origem) r.log.add("Rede desconexa: nova árvore começa em " + g.getNome(s));
            naArvore[s] = true;
            r.caminho.add(s);
            r.passos.add(Passo.no(s, "Raiz da árvore: " + g.getNome(s)));
            r.log.add("Raiz: " + g.getNome(s));

            // Cada elemento da fila: {cidadeNaArvore, cidadeFora, peso}
            PriorityQueue<int[]> fila = new PriorityQueue<int[]>((a, b) -> Integer.compare(a[2], b[2]));
            for (Vizinho v : g.getVizinhos(s)) fila.add(new int[]{s, v.destino, v.peso});

            while (!fila.isEmpty()) {
                int[] e = fila.poll();                        // a aresta mais barata da fronteira
                if (naArvore[e[1]]) {                         // destino já está na árvore -> criaria ciclo
                    r.log.add("   ignora " + g.getNome(e[0]) + " - " + g.getNome(e[1]) + " (peso " + e[2]
                            + "): " + g.getNome(e[1]) + " já está na árvore");
                    continue;
                }
                naArvore[e[1]] = true;
                r.custoTotal += e[2];
                arestasUsadas++;
                r.caminho.add(e[1]);
                r.passos.add(Passo.mover(e[0], e[1], "Seleciona " + g.getNome(e[0]) + " - " + g.getNome(e[1])
                        + " (peso " + e[2] + ", custo acumulado " + r.custoTotal + ")"));
                r.log.add("Seleciona " + g.getNome(e[0]) + " - " + g.getNome(e[1]) + " (peso " + e[2]
                        + ")   custo acumulado: " + r.custoTotal);
                for (Vizinho v : g.getVizinhos(e[1])) {       // novas arestas da fronteira
                    if (!naArvore[v.destino]) fila.add(new int[]{e[1], v.destino, v.peso});
                }
            }
        }

        r.detalhe = arestasUsadas + " arestas (V - componentes = " + (n - g.numeroComponentes()) + ")";
        r.tempoNs = System.nanoTime() - t0;
        return r;
    }
}
