package EDAII_TP;

import java.util.List;

/**
 * ROTA DE ABASTECIMENTO - HEURÍSTICA DO VIZINHO MAIS PRÓXIMO.
 *
 * Objetivo (alínea f): sair da cidade de partida, passar por TODAS as cidades e terminar na
 * cidade de chegada, gastando o menor custo possível.
 *
 * Isto é o Problema do Caixeiro Viajante, que é NP-difícil: não se conhece algoritmo rápido
 * que garanta o ótimo. A heurística é simples e rápida:
 *   1) estando na cidade atual, vai (pelo caminho mínimo de Dijkstra) para a cidade AINDA
 *      NÃO VISITADA mais próxima;
 *   2) as cidades atravessadas pelo caminho também ficam visitadas;
 *   3) repete até não haver mais cidades por visitar; por fim segue para o destino.
 * Se origem == destino, a rota é um circuito fechado.
 *
 * Dá uma boa solução mas NÃO garante a melhor. Para ver quão longe ficou do ótimo, compare
 * com RotaOtima (Held-Karp).  Complexidade: O(V^2) escolhas sobre distâncias já calculadas.
 */
public class RotaHeuristica implements Algoritmo {

    @Override public String nome() { return "Rota de abastecimento (heurística)"; }

    @Override public String descricao() {
        return "Vizinho mais próximo sobre caminhos mínimos. Rápida, mas pode ficar acima do ótimo (problema NP-difícil).";
    }

    @Override public boolean usaDestino() { return true; }

    @Override
    public Resultado executar(GrafoFerroviarioUcraniano g, int origem, int destino) {
        Resultado r = new Resultado(nome());
        long t0 = System.nanoTime();
        int n = g.getN();
        CaminhosMinimos cm = new CaminhosMinimos(g);
        boolean[] visitada = new boolean[n];

        visitada[origem] = true;
        r.caminho.add(origem);
        r.passos.add(Passo.no(origem, "Partida: " + g.getNome(origem)));
        int atual = origem;
        int etapa = 1;

        while (true) {
            int melhor = -1;                       // cidade não visitada mais próxima (exceto o destino)
            for (int i = 0; i < n; i++) {
                if (visitada[i] || i == destino || !cm.alcancavel(atual, i)) continue;
                if (melhor == -1 || cm.dist(atual, i) < cm.dist(atual, melhor)) melhor = i;
            }
            if (melhor == -1) break;               // já não há cidades alcançáveis por abastecer
            percorrer(g, cm, r, visitada, atual, melhor, "Etapa " + etapa++);
            atual = melhor;
        }

        if (atual != destino) {                    // última etapa: seguir para a cidade de chegada
            if (cm.alcancavel(atual, destino)) {
                percorrer(g, cm, r, visitada, atual, destino, "Etapa final");
            } else {
                r.encontrado = false;
                r.mensagemErro = "O destino " + g.getNome(destino) + " não é alcançável a partir de " + g.getNome(atual) + ".";
                r.log.add(r.mensagemErro);
            }
        }
        for (int i = 0; i < n; i++) {
            if (!visitada[i] && i != destino) {
                r.log.add("Aviso: " + g.getNome(i) + " é inalcançável (rede desconexa) e não foi abastecida.");
            }
        }
        r.detalhe = (r.caminho.size() - 1) + " movimentos, " + contarVisitadas(visitada) + " cidades abastecidas";
        r.tempoNs = System.nanoTime() - t0;
        return r;
    }

    /** Anexa ao resultado o caminho mínimo src -> dst, marcando as cidades atravessadas como visitadas. */
    static void percorrer(GrafoFerroviarioUcraniano g, CaminhosMinimos cm, Resultado r, boolean[] visitada,
                          int src, int dst, String rotulo) {
        List<Integer> cam = cm.caminho(src, dst);
        StringBuilder sb = new StringBuilder();
        int custoEtapa = 0;
        for (int k = 0; k < cam.size(); k++) {
            sb.append(g.getNome(cam.get(k))).append(k < cam.size() - 1 ? " -> " : "");
            if (k > 0) {
                int a = cam.get(k - 1), b = cam.get(k), peso = g.getPeso(a, b);
                custoEtapa += peso;
                r.custoTotal += peso;
                r.caminho.add(b);
                r.passos.add(Passo.mover(a, b, rotulo + ": " + g.getNome(a) + " -> " + g.getNome(b)
                        + " (+" + peso + ", total " + r.custoTotal + " " + g.getUnidade() + ")"));
            }
            visitada[cam.get(k)] = true;
        }
        r.log.add(rotulo + ": " + sb + "   (custo da etapa " + custoEtapa + ", total " + r.custoTotal + ")");
    }

    private static int contarVisitadas(boolean[] v) {
        int c = 0;
        for (boolean b : v) if (b) c++;
        return c;
    }
}
