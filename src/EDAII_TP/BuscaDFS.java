package EDAII_TP;

/**
 * BUSCA EM PROFUNDIDADE (DFS - Depth-First Search).
 *
 * Ideia: a partir da cidade atual, desce sempre pelo primeiro vizinho ainda não visitado,
 * até não poder avançar mais; depois RETROCEDE (backtracking) e tenta outro ramo.
 * Estrutura auxiliar: a PILHA de chamadas da recursão.
 * Complexidade: O(V + E) com listas de adjacências.
 *
 * Custo apresentado = soma dos pesos das arestas da árvore DFS (não é um caminho mínimo!).
 */
public class BuscaDFS implements Algoritmo {

    @Override public String nome() { return "DFS (Busca em Profundidade)"; }

    @Override public String descricao() {
        return "Pilha (recursão). Complexidade O(V + E). Visita cada cidade uma vez, explorando um ramo até ao fim antes de retroceder.";
    }

    @Override public boolean usaDestino() { return false; }

    @Override
    public Resultado executar(GrafoFerroviarioUcraniano g, int origem, int destino) {
        Resultado r = new Resultado(nome());
        long t0 = System.nanoTime();
        boolean[] visitado = new boolean[g.getN()];

        r.passos.add(Passo.no(origem, "Início em " + g.getNome(origem)));
        visitar(g, origem, visitado, r, 0);

        int alcancadas = r.caminho.size();
        r.detalhe = alcancadas + " de " + g.getN() + " cidades alcançadas";
        if (alcancadas < g.getN()) {
            StringBuilder sb = new StringBuilder("Não alcançadas a partir de " + g.getNome(origem) + " (rede desconexa): ");
            boolean primeiro = true;
            for (int i = 0; i < g.getN(); i++) {
                if (!visitado[i]) { sb.append(primeiro ? "" : ", ").append(g.getNome(i)); primeiro = false; }
            }
            r.log.add(sb.toString());
        }
        r.tempoNs = System.nanoTime() - t0;
        return r;
    }

    /** Visita recursiva. 'nivel' só serve para indentar o log e mostrar a profundidade. */
    private void visitar(GrafoFerroviarioUcraniano g, int u, boolean[] visitado, Resultado r, int nivel) {
        visitado[u] = true;                       // marca ao ENTRAR no vértice
        r.caminho.add(u);
        String ind = indentar(nivel);
        r.log.add(ind + "Visita " + g.getNome(u) + "  (profundidade " + nivel + ")");
        for (Vizinho viz : g.getVizinhos(u)) {    // vizinhos por ordem de índice
            int v = viz.destino;
            if (!visitado[v]) {
                r.log.add(ind + "  aresta " + g.getNome(u) + " - " + g.getNome(v) + " (peso " + viz.peso + ") -> desce");
                r.passos.add(Passo.mover(u, v, "Desce de " + g.getNome(u) + " para " + g.getNome(v) + " (peso " + viz.peso + ")"));
                r.custoTotal += viz.peso;
                visitar(g, v, visitado, r, nivel + 1);
                r.log.add(ind + "  retrocede para " + g.getNome(u));     // backtracking
            }
        }
    }

    static String indentar(int nivel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivel; i++) sb.append("  ");
        return sb.toString();
    }
}
