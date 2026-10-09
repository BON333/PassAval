package EDAII_TP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ROTA DE ABASTECIMENTO ÓTIMA - PROGRAMAÇÃO DINÂMICA DE HELD-KARP.
 *
 * Mesmo problema da RotaHeuristica (partir de A, passar por TODAS as cidades, terminar em B),
 * mas aqui o resultado é o MELHOR possível. É exato, ao contrário da heurística.
 *
 * Como funciona:
 *  1) Usa as distâncias mínimas entre todos os pares (CaminhosMinimos). Assim pode-se passar
 *     por uma cidade mais do que uma vez, se compensar - como num caminho ferroviário real.
 *  2) Seja M o conjunto das cidades intermédias (todas, exceto origem e destino).
 *     dp[S][j] = menor custo para sair da origem, visitar exatamente as cidades do subconjunto S
 *     e ficar em j (j pertence a S). Transição:  dp[S ∪ {k}][k] = min( dp[S][j] + dist(j, k) ).
 *  3) Resposta = min sobre j de dp[M][j] + dist(j, destino). Reconstrói-se a ordem com "pai[][]".
 *  4) Cada salto entre cidades é expandido para o caminho mínimo real (cidades de passagem).
 *
 * Complexidade: O(2^m * m^2) tempo e O(2^m * m) memória, com m = nº de cidades intermédias.
 * Por isso há um LIMITE (m <= 16): com 12 cidades são ~100 mil operações; com 30 já seria inviável.
 *
 * Se origem == destino, a solução é um circuito fechado (caixeiro viajante clássico).
 */
public class RotaOtima implements Algoritmo {

    /** Máximo de cidades intermédias suportado (2^16 * 16 * 16 ≈ 16 milhões de operações). */
    public static final int LIMITE = 16;

    @Override public String nome() { return "Rota de abastecimento (ótima)"; }

    @Override public String descricao() {
        return "Programação dinâmica de Held-Karp. Exata, O(2^m * m^2), até " + LIMITE + " cidades intermédias.";
    }

    @Override public boolean usaDestino() { return true; }

    @Override
    public Resultado executar(GrafoFerroviarioUcraniano g, int origem, int destino) {
        Resultado r = new Resultado(nome());
        long t0 = System.nanoTime();
        int n = g.getN();
        CaminhosMinimos cm = new CaminhosMinimos(g);

        if (!cm.alcancavel(origem, destino)) {
            r.encontrado = false;
            r.mensagemErro = "O destino " + g.getNome(destino) + " não é alcançável a partir de " + g.getNome(origem) + ".";
            r.log.add(r.mensagemErro);
            r.tempoNs = System.nanoTime() - t0;
            return r;
        }

        // Cidades intermédias: todas as alcançáveis a partir da origem, exceto origem e destino
        List<Integer> inter = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            if (i != origem && i != destino && cm.alcancavel(origem, i)) inter.add(i);
        }
        int m = inter.size();
        if (m > LIMITE) {
            r.encontrado = false;
            r.mensagemErro = "Rede demasiado grande para a solução exata: " + m + " cidades intermédias (limite " + LIMITE
                    + "). Use a heurística.";
            r.log.add(r.mensagemErro);
            r.tempoNs = System.nanoTime() - t0;
            return r;
        }
        for (int i = 0; i < n; i++) {
            if (i != origem && i != destino && !cm.alcancavel(origem, i)) {
                r.log.add("Aviso: " + g.getNome(i) + " é inalcançável (rede desconexa) e não será abastecida.");
            }
        }
        r.log.add("Cidades intermédias a visitar: " + m + "  ->  " + (1L << m) + " subconjuntos x " + Math.max(m, 1) + " finais");

        // ---------------- Held-Karp ----------------
        int[] ordem = new int[m];                        // ordem ótima das cidades intermédias
        if (m > 0) {
            int total = 1 << m;
            int full = total - 1;
            int[][] dp = new int[total][m];
            int[][] pai = new int[total][m];
            for (int[] linha : dp) Arrays.fill(linha, CaminhoDijkstra.INF);
            for (int j = 0; j < m; j++) {                // subconjuntos de 1 cidade: origem -> j
                dp[1 << j][j] = cm.dist(origem, inter.get(j));
                pai[1 << j][j] = -1;
            }
            for (int mask = 1; mask <= full; mask++) {
                for (int j = 0; j < m; j++) {
                    if ((mask & (1 << j)) == 0 || dp[mask][j] >= CaminhoDijkstra.INF) continue;
                    for (int k = 0; k < m; k++) {
                        if ((mask & (1 << k)) != 0) continue;                    // k já foi visitada
                        int nova = mask | (1 << k);
                        int c = dp[mask][j] + cm.dist(inter.get(j), inter.get(k));
                        if (c < dp[nova][k]) {
                            dp[nova][k] = c;
                            pai[nova][k] = j;
                        }
                    }
                }
            }
            int melhor = CaminhoDijkstra.INF, ultimo = -1;
            for (int j = 0; j < m; j++) {                // fecha o percurso: último intermédio -> destino
                int c = dp[full][j] + cm.dist(inter.get(j), destino);
                if (c < melhor) { melhor = c; ultimo = j; }
            }
            int mask = full, j = ultimo;                 // reconstrução de trás para a frente
            for (int idx = m - 1; idx >= 0; idx--) {
                ordem[idx] = inter.get(j);
                int anterior = pai[mask][j];
                mask ^= (1 << j);
                j = anterior;
            }
        }

        // ---------------- Expansão em caminhos reais ----------------
        List<Integer> paragens = new ArrayList<Integer>();
        paragens.add(origem);
        for (int c : ordem) paragens.add(c);
        paragens.add(destino);
        StringBuilder ord = new StringBuilder();
        for (int k = 0; k < paragens.size(); k++) {
            ord.append(g.getNome(paragens.get(k))).append(k < paragens.size() - 1 ? " => " : "");
        }
        r.log.add("Ordem ótima das paragens: " + ord);

        boolean[] visitada = new boolean[n];
        visitada[origem] = true;
        r.caminho.add(origem);
        r.passos.add(Passo.no(origem, "Partida: " + g.getNome(origem)));
        for (int k = 1; k < paragens.size(); k++) {
            String rotulo = (k == paragens.size() - 1) ? "Etapa final" : "Etapa " + k;
            RotaHeuristica.percorrer(g, cm, r, visitada, paragens.get(k - 1), paragens.get(k), rotulo);
        }

        int cidadesAbastecidas = 0;
        for (boolean b : visitada) if (b) cidadesAbastecidas++;
        r.detalhe = (r.caminho.size() - 1) + " movimentos, " + cidadesAbastecidas + " cidades abastecidas";

        // Comparação com a heurística (o que a solução exata "ganha")
        Resultado h = new RotaHeuristica().executar(g, origem, destino);
        if (h.encontrado) {
            int dif = h.custoTotal - r.custoTotal;
            double pct = r.custoTotal > 0 ? dif * 100.0 / r.custoTotal : 0.0;
            r.log.add(String.format("Comparação: heurística = %d %s | ótima = %d %s | diferença = %d (%.1f%% acima do ótimo)",
                    h.custoTotal, g.getUnidade(), r.custoTotal, g.getUnidade(), dif, pct));
        }
        r.tempoNs = System.nanoTime() - t0;
        return r;
    }
}
