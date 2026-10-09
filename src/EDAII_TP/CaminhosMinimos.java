package EDAII_TP;

import java.util.List;

/**
 * Distâncias mínimas entre TODOS os pares de cidades (Dijkstra executado a partir de cada vértice).
 *
 * É o "fecho métrico" da rede: dist(a, b) = custo do caminho mais barato de a até b, e
 * caminho(a, b) devolve esse caminho. As rotas de abastecimento usam-no para saltar de
 * cidade em cidade sem repetir cálculos.
 * Custo de construção: O(V (V + E) log V).
 */
public class CaminhosMinimos {

    private final int n;
    private final int[][] dist;
    private final int[][] prev;

    public CaminhosMinimos(GrafoFerroviarioUcraniano g) {
        n = g.getN();
        dist = new int[n][n];
        prev = new int[n][n];
        for (int s = 0; s < n; s++) CaminhoDijkstra.calcular(g, s, dist[s], prev[s], null);
    }

    public boolean alcancavel(int a, int b) {
        return dist[a][b] < CaminhoDijkstra.INF;
    }

    /** Distância mínima a -> b (CaminhoDijkstra.INF se inalcançável). */
    public int dist(int a, int b) {
        return dist[a][b];
    }

    /** Caminho mínimo a -> b incluindo os dois extremos; [a] se a == b; vazio se inalcançável. */
    public List<Integer> caminho(int a, int b) {
        return CaminhoDijkstra.reconstruir(prev[a], a, b);
    }

    public int tamanho() {
        return n;
    }
}
