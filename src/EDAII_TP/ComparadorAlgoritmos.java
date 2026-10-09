package EDAII_TP;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Executa todos os algoritmos sobre a mesma rede e compara custos e tempos.
 * Também faz verificações cruzadas de correção, que servem de argumento na defesa:
 *   - Prim e Kruskal têm de dar o MESMO custo (a MST é única em custo);
 *   - Dijkstra tem de coincidir com Floyd-Warshall (outro algoritmo, outro método);
 *   - a rota ótima nunca pode ser pior do que a heurística;
 *   - DFS e BFS têm de alcançar exatamente as mesmas cidades.
 */
public class ComparadorAlgoritmos {

    /** Uma linha da tabela comparativa. */
    public static class Linha {
        public final String algoritmo;
        public final Integer custo;          // null = não aplicável
        public final String detalhe;
        public final double tempoMicros;
        public final String observacao;

        Linha(String algoritmo, Integer custo, String detalhe, double tempoMicros, String observacao) {
            this.algoritmo = algoritmo;
            this.custo = custo;
            this.detalhe = detalhe;
            this.tempoMicros = tempoMicros;
            this.observacao = observacao;
        }
    }

    public static class Relatorio {
        public final List<Linha> linhas = new ArrayList<Linha>();
        public final List<String> verificacoes = new ArrayList<String>();
        public boolean tudoOk = true;
    }

    public Relatorio comparar(GrafoFerroviarioUcraniano g, int origem, int destino) {
        Relatorio rel = new Relatorio();
        Algoritmo dfs = new BuscaDFS(), bfs = new BuscaBFS(), prim = new MstPrim(), kruskal = new MstKruskal();
        Algoritmo dijkstra = new CaminhoDijkstra(), rotaH = new RotaHeuristica(), rotaO = new RotaOtima();

        Resultado rDfs = dfs.executar(g, origem, destino);
        Resultado rBfs = bfs.executar(g, origem, destino);
        Resultado rPrim = prim.executar(g, origem, destino);
        Resultado rKru = kruskal.executar(g, origem, destino);
        Resultado rDij = dijkstra.executar(g, origem, destino);
        Resultado rH = rotaH.executar(g, origem, destino);
        Resultado rO = rotaO.executar(g, origem, destino);

        rel.linhas.add(linha(dfs, rDfs, g, origem, destino, "custo = soma das arestas da árvore DFS"));
        rel.linhas.add(linha(bfs, rBfs, g, origem, destino, "custo = soma das arestas da árvore BFS"));
        rel.linhas.add(linha(prim, rPrim, g, origem, destino, "árvore geradora mínima"));
        rel.linhas.add(linha(kruskal, rKru, g, origem, destino, "árvore geradora mínima"));
        if (origem == destino) {
            rel.linhas.add(new Linha(dijkstra.nome(), null, "origem = destino", 0, "escolha cidades diferentes"));
        } else {
            rel.linhas.add(linha(dijkstra, rDij, g, origem, destino, rDij.encontrado ? "caminho mais barato" : "sem caminho"));
        }
        rel.linhas.add(linha(rotaH, rH, g, origem, destino, rH.encontrado ? "aproximação (NP-difícil)" : "destino inalcançável"));
        rel.linhas.add(linha(rotaO, rO, g, origem, destino, rO.encontrado ? "solução exata" : "não calculada"));

        // ---------------- Verificações cruzadas ----------------
        String un = g.getUnidade();
        verificar(rel, rPrim.custoTotal == rKru.custoTotal,
                "Prim = Kruskal: " + rPrim.custoTotal + " " + un + " e " + rKru.custoTotal + " " + un);

        int[][] fw = floydWarshall(g);
        CaminhosMinimos cm = new CaminhosMinimos(g);
        boolean iguais = true;
        int n = g.getN();
        for (int i = 0; i < n && iguais; i++) {
            for (int j = 0; j < n; j++) {
                if (fw[i][j] != cm.dist(i, j)) { iguais = false; break; }
            }
        }
        verificar(rel, iguais, "Dijkstra = Floyd-Warshall em todos os " + (n * n) + " pares de cidades");

        Set<Integer> sd = new HashSet<Integer>(rDfs.caminho), sb = new HashSet<Integer>(rBfs.caminho);
        verificar(rel, sd.equals(sb), "DFS e BFS alcançam as mesmas " + sd.size() + " cidades");

        if (rH.encontrado && rO.encontrado) {
            boolean ok = rO.custoTotal <= rH.custoTotal;
            double pct = rO.custoTotal > 0 ? (rH.custoTotal - rO.custoTotal) * 100.0 / rO.custoTotal : 0.0;
            verificar(rel, ok, String.format("Rota ótima (%d %s) <= heurística (%d %s); a heurística fica %.1f%% acima do ótimo",
                    rO.custoTotal, un, rH.custoTotal, un, pct));
        } else if (!rO.encontrado && rH.encontrado) {
            rel.verificacoes.add("[--] Rota ótima não calculada: " + rO.mensagemErro);
        }
        if (n > 1) {
            int esperado = n - g.numeroComponentes();
            verificar(rel, rPrim.detalhe.startsWith(esperado + " ") && rKru.detalhe.startsWith(esperado + " "),
                    "A MST tem V - componentes = " + esperado + " arestas (Prim e Kruskal)");
        }
        return rel;
    }

    private void verificar(Relatorio rel, boolean ok, String texto) {
        rel.verificacoes.add((ok ? "[OK]   " : "[ERRO] ") + texto);
        if (!ok) rel.tudoOk = false;
    }

    private Linha linha(Algoritmo alg, Resultado r, GrafoFerroviarioUcraniano g, int o, int d, String obs) {
        Integer custo = r.encontrado ? Integer.valueOf(r.custoTotal) : null;
        return new Linha(alg.nome(), custo, r.detalhe, medirMicros(alg, g, o, d, r.tempoNs), obs);
    }

    /**
     * Tempo médio em microssegundos. A primeira execução de código Java é lenta (a JVM ainda
     * está a "aquecer"), por isso repete-se várias vezes e faz-se a média das últimas.
     */
    private double medirMicros(Algoritmo alg, GrafoFerroviarioUcraniano g, int o, int d, long primeiraNs) {
        if (primeiraNs > 50000000L) return primeiraNs / 1000.0;      // > 50 ms: não repetir
        for (int i = 0; i < 5; i++) alg.executar(g, o, d);           // aquecimento
        int reps = 30;
        long t0 = System.nanoTime();
        for (int i = 0; i < reps; i++) alg.executar(g, o, d);
        return (System.nanoTime() - t0) / (double) reps / 1000.0;
    }

    /** Floyd-Warshall: distâncias entre todos os pares, O(V^3). Usado como verificação independente. */
    public static int[][] floydWarshall(GrafoFerroviarioUcraniano g) {
        int n = g.getN();
        int inf = CaminhoDijkstra.INF;
        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = i == j ? 0 : (g.getPeso(i, j) > 0 ? g.getPeso(i, j) : inf);
            }
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (d[i][k] >= inf) continue;
                for (int j = 0; j < n; j++) {
                    if (d[k][j] < inf && d[i][k] + d[k][j] < d[i][j]) d[i][j] = d[i][k] + d[k][j];
                }
            }
        }
        return d;
    }
}
