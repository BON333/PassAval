package EDAII_TP;

/**
 * Estrutura União-Busca (Union-Find / conjuntos disjuntos), usada pelo Kruskal.
 *
 * Cada cidade começa num conjunto só dela. find(x) diz a que conjunto x pertence;
 * unir(a, b) junta dois conjuntos. Duas cidades estão no mesmo conjunto se e só se já estão
 * ligadas na floresta em construção - ligar outra aresta entre elas criaria um CICLO.
 * Otimizações: compressão de caminho + união por altura (rank) -> quase O(1) por operação.
 */
public class UniaoBusca {

    private final int[] pai;
    private final int[] altura;

    public UniaoBusca(int n) {
        pai = new int[n];
        altura = new int[n];
        for (int i = 0; i < n; i++) pai[i] = i;     // cada elemento é o seu próprio representante
    }

    /** Representante do conjunto de x (com compressão de caminho, iterativa). */
    public int find(int x) {
        while (pai[x] != x) {
            pai[x] = pai[pai[x]];                    // aponta x diretamente para o avô
            x = pai[x];
        }
        return x;
    }

    /** Une os conjuntos de a e b. Devolve false se já estavam no mesmo conjunto (ciclo). */
    public boolean unir(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (altura[ra] < altura[rb]) { int t = ra; ra = rb; rb = t; }
        pai[rb] = ra;                                // a árvore mais baixa pendura-se na mais alta
        if (altura[ra] == altura[rb]) altura[ra]++;
        return true;
    }
}
