package EDAII_TP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * TESTES AUTOMÁTICOS (sem bibliotecas externas - não precisa de JUnit).
 *
 * Executar:  Run As > Java Application nesta classe  (ou  java EDAII_TP.TestesAlgoritmos).
 * No fim imprime "TODOS OS TESTES PASSARAM" ou a lista do que falhou.
 *
 * Os valores esperados (MST = 121, Kiev->Crimeia = 51, ordens de DFS/BFS) foram calculados
 * À MÃO a partir das 17 arestas do enunciado, por isso testam a implementação contra a teoria,
 * e não contra ela própria. Além disso, cada algoritmo é conferido com um segundo método:
 * Dijkstra vs Floyd-Warshall, Held-Karp vs força bruta, Prim vs Kruskal.
 */
public class TestesAlgoritmos {

    private static int total = 0;
    private static int falhas = 0;
    private static final List<String> mensagensFalha = new ArrayList<String>();

    // Índices da rede predefinida
    private static final int CHERNIHIV = 0, ZHITOMYR = 1, TERNOPIL = 2, KIEV = 3, KREMENCHUCK = 4, LUHANSKI = 5,
            DONETSK = 6, MARIUPOL = 7, ZAPORIZHIA = 8, MYKOLAIV = 9, KHERSON = 10, CRIMEIA = 11;

    public static void main(String[] args) throws Exception {
        testarEstruturas();
        testarBuscas();
        testarMst();
        testarDijkstra();
        testarRotas();
        testarRotaOtimaContraForcaBruta();
        testarGestaoDinamica();
        testarDesfazerRefazer();
        testarPersistencia();
        testarRedeDesconexa();
        testarComparador();
        testarRedeExpandidaV2();

        System.out.println();
        System.out.println("Testes executados: " + total + "   Falhas: " + falhas);
        if (falhas == 0) {
            System.out.println("TODOS OS TESTES PASSARAM");
        } else {
            for (String m : mensagensFalha) System.out.println("  FALHOU: " + m);
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ utilitários
    private static void verificar(boolean cond, String descricao) {
        total++;
        if (!cond) {
            falhas++;
            mensagensFalha.add(descricao);
            System.out.println("[FALHOU] " + descricao);
        }
    }

    private static void igual(Object esperado, Object obtido, String descricao) {
        verificar(esperado.equals(obtido), descricao + "  (esperado " + esperado + ", obtido " + obtido + ")");
    }

    private static void titulo(String t) {
        System.out.println("-- " + t);
    }

    private static List<Integer> lista(int... xs) {
        List<Integer> l = new ArrayList<Integer>();
        for (int x : xs) l.add(x);
        return l;
    }

    // ------------------------------------------------------------------ testes
    private static void testarEstruturas() {
        titulo("Matriz e Listas de Adjacências (v1 - enunciado)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();
        igual(12, g.getN(), "nº de cidades");
        igual(17, g.getArestas().size(), "nº de arestas");
        verificarConsistencia(g, "rede predefinida");
        igual(18, g.getPeso(CHERNIHIV, KIEV), "peso Chernihiv-Kiev");
        igual(18, g.getPeso(KIEV, CHERNIHIV), "peso simétrico Kiev-Chernihiv");
        igual(0, g.getPeso(CHERNIHIV, CRIMEIA), "sem ligação direta Chernihiv-Crimeia");
        igual(1, g.numeroComponentes(), "rede predefinida é conexa");
    }

    /** Matriz simétrica, sem laços, e coerente com as Listas de Adjacências e com a lista de arestas. */
    private static void verificarConsistencia(GrafoFerroviarioUcraniano g, String contexto) {
        int n = g.getN();
        boolean simetrica = true, semLacos = true, listaOk = true;
        int somaGraus = 0;
        for (int i = 0; i < n; i++) {
            if (g.getMatriz()[i][i] != 0) semLacos = false;
            for (int j = 0; j < n; j++) {
                if (g.getMatriz()[i][j] != g.getMatriz()[j][i]) simetrica = false;
            }
            somaGraus += g.getGrau(i);
            for (Vizinho v : g.getVizinhos(i)) {
                if (g.getMatriz()[i][v.destino] != v.peso) listaOk = false;
            }
        }
        verificar(simetrica, contexto + ": matriz simétrica");
        verificar(semLacos, contexto + ": diagonal nula");
        verificar(listaOk, contexto + ": listas coerentes com a matriz");
        igual(2 * g.getArestas().size(), somaGraus, contexto + ": soma dos graus = 2 x arestas");
    }

    private static void testarBuscas() {
        titulo("DFS e BFS (v1 - enunciado)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();
        Resultado bfs = new BuscaBFS().executar(g, KIEV, 0);
        // Calculado à mão (vizinhos por ordem de índice)
        igual(lista(3, 0, 1, 4, 5, 2, 8, 9, 6, 7, 10, 11), bfs.caminho, "ordem BFS a partir de Kiev");
        igual(11, bfs.passos.size() - 1, "BFS: 11 arestas na árvore");

        Resultado dfs = new BuscaDFS().executar(g, CHERNIHIV, 0);
        igual(lista(0, 3, 1, 2, 4, 8, 6, 5, 7, 10, 9, 11), dfs.caminho, "ordem DFS a partir de Chernihiv");
        igual(11, dfs.passos.size() - 1, "DFS: 11 arestas na árvore");

        for (int s = 0; s < g.getN(); s++) {
            igual(12, new BuscaBFS().executar(g, s, 0).caminho.size(), "BFS alcança as 12 cidades a partir de " + g.getNome(s));
            igual(12, new BuscaDFS().executar(g, s, 0).caminho.size(), "DFS alcança as 12 cidades a partir de " + g.getNome(s));
        }
    }

    private static void testarMst() {
        titulo("MST: Prim e Kruskal (v1 - enunciado)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();
        Resultado k = new MstKruskal().executar(g, 0, 0);
        igual(121, k.custoTotal, "custo da MST (Kruskal), calculado à mão");
        igual(11, contarAceites(k), "Kruskal aceita V-1 = 11 arestas");
        for (int s = 0; s < g.getN(); s++) {
            Resultado p = new MstPrim().executar(g, s, 0);
            igual(121, p.custoTotal, "custo da MST (Prim) a partir de " + g.getNome(s));
        }
        // Todas as arestas rejeitadas do Kruskal têm de fechar mesmo um ciclo: vemos o grafo aceite
        boolean aciclico = true;
        UniaoBusca uf = new UniaoBusca(g.getN());
        for (Passo p : k.passos) {
            if (!p.rejeitado && !uf.unir(p.u, p.v)) aciclico = false;
        }
        verificar(aciclico, "as arestas aceites pelo Kruskal não formam ciclos");
        // A MST é ligada: todas as cidades no mesmo conjunto
        boolean ligada = true;
        for (int i = 1; i < g.getN(); i++) if (uf.find(i) != uf.find(0)) ligada = false;
        verificar(ligada, "a MST liga todas as cidades");
    }

    private static int contarAceites(Resultado r) {
        int c = 0;
        for (Passo p : r.passos) if (!p.rejeitado && !p.isNo()) c++;
        return c;
    }

    private static void testarDijkstra() {
        titulo("Dijkstra (v1 - enunciado)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();
        Resultado r = new CaminhoDijkstra().executar(g, KIEV, CRIMEIA);
        igual(51, r.custoTotal, "Kiev -> Crimeia custa 51");
        igual(lista(KIEV, KREMENCHUCK, MYKOLAIV, KHERSON, CRIMEIA), r.caminho, "caminho Kiev -> Crimeia");
        igual(0, new CaminhoDijkstra().executar(g, KIEV, KIEV).custoTotal, "Kiev -> Kiev custa 0");

        // Dijkstra vs Floyd-Warshall em todos os pares
        int[][] fw = ComparadorAlgoritmos.floydWarshall(g);
        boolean todos = true;
        for (int a = 0; a < g.getN(); a++) {
            for (int b = 0; b < g.getN(); b++) {
                if (new CaminhoDijkstra().executar(g, a, b).custoTotal != fw[a][b]) todos = false;
            }
        }
        verificar(todos, "Dijkstra = Floyd-Warshall em todos os pares (rede predefinida)");

        // E em redes aleatórias
        Random rnd = new Random(42);
        for (int t = 0; t < 30; t++) {
            GrafoFerroviarioUcraniano rg = redeAleatoria(rnd, 4 + rnd.nextInt(8), true);
            int[][] f = ComparadorAlgoritmos.floydWarshall(rg);
            boolean ok = true;
            for (int a = 0; a < rg.getN(); a++) {
                for (int b = 0; b < rg.getN(); b++) {
                    Resultado d = new CaminhoDijkstra().executar(rg, a, b);
                    int esperado = f[a][b];
                    if (esperado >= CaminhoDijkstra.INF) { if (d.encontrado) ok = false; }
                    else if (!d.encontrado || d.custoTotal != esperado) ok = false;
                    // o caminho devolvido tem de ser válido e custar o que diz
                    if (d.encontrado) ok &= caminhoValido(rg, d.caminho, d.custoTotal);
                }
            }
            verificar(ok, "Dijkstra = Floyd-Warshall e caminhos válidos (rede aleatória #" + t + ")");
        }
    }

    /** Cada par consecutivo tem de ser uma aresta existente e a soma dos pesos tem de bater certo. */
    private static boolean caminhoValido(GrafoFerroviarioUcraniano g, List<Integer> cam, int custo) {
        int soma = 0;
        for (int i = 1; i < cam.size(); i++) {
            if (!g.temAresta(cam.get(i - 1), cam.get(i))) return false;
            soma += g.getPeso(cam.get(i - 1), cam.get(i));
        }
        return soma == custo;
    }

    private static void testarRotas() {
        titulo("Rotas de abastecimento (v1 - enunciado)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();
        int[][] pares = {{KIEV, CRIMEIA}, {CHERNIHIV, CRIMEIA}, {TERNOPIL, LUHANSKI}, {KIEV, KIEV}, {CRIMEIA, TERNOPIL}};
        for (int[] p : pares) {
            String nome = g.getNome(p[0]) + " -> " + g.getNome(p[1]);
            Resultado h = new RotaHeuristica().executar(g, p[0], p[1]);
            Resultado o = new RotaOtima().executar(g, p[0], p[1]);
            verificar(h.encontrado && o.encontrado, "rotas encontradas: " + nome);
            verificar(passaPorTodas(g, h.caminho), "heurística passa por todas as cidades: " + nome);
            verificar(passaPorTodas(g, o.caminho), "ótima passa por todas as cidades: " + nome);
            verificar(caminhoValido(g, h.caminho, h.custoTotal), "heurística: percurso válido e custo coerente: " + nome);
            verificar(caminhoValido(g, o.caminho, o.custoTotal), "ótima: percurso válido e custo coerente: " + nome);
            verificar(o.custoTotal <= h.custoTotal, "ótima <= heurística: " + nome + " (" + o.custoTotal + " <= " + h.custoTotal + ")");
            igual(p[0], h.caminho.get(0), "heurística começa na origem: " + nome);
            igual(p[1], h.caminho.get(h.caminho.size() - 1), "heurística termina no destino: " + nome);
            igual(p[0], o.caminho.get(0), "ótima começa na origem: " + nome);
            igual(p[1], o.caminho.get(o.caminho.size() - 1), "ótima termina no destino: " + nome);
            System.out.println("   " + nome + ": heurística " + h.custoTotal + " | ótima " + o.custoTotal);
        }
    }

    private static boolean passaPorTodas(GrafoFerroviarioUcraniano g, List<Integer> caminho) {
        boolean[] v = new boolean[g.getN()];
        for (int x : caminho) v[x] = true;
        for (boolean b : v) if (!b) return false;
        return true;
    }

    /** Held-Karp tem de coincidir com a força bruta (todas as permutações) em redes pequenas. */
    private static void testarRotaOtimaContraForcaBruta() {
        titulo("Held-Karp vs força bruta");
        Random rnd = new Random(7);
        for (int t = 0; t < 40; t++) {
            int n = 3 + rnd.nextInt(5);                         // 3..7 cidades
            GrafoFerroviarioUcraniano g = redeAleatoria(rnd, n, false);   // só conexas
            int o = rnd.nextInt(n), d = rnd.nextInt(n);
            int esperado = forcaBruta(g, o, d);
            Resultado r = new RotaOtima().executar(g, o, d);
            verificar(r.encontrado && r.custoTotal == esperado,
                    "Held-Karp = força bruta (#" + t + ", n=" + n + ", " + o + "->" + d + "): esperado " + esperado + ", obtido " + r.custoTotal);
        }
    }

    /** Testa todas as ordens possíveis das cidades intermédias, usando distâncias de Floyd-Warshall. */
    private static int forcaBruta(GrafoFerroviarioUcraniano g, int o, int d) {
        int[][] fw = ComparadorAlgoritmos.floydWarshall(g);
        List<Integer> inter = new ArrayList<Integer>();
        for (int i = 0; i < g.getN(); i++) if (i != o && i != d) inter.add(i);
        int[] melhor = {Integer.MAX_VALUE};
        permutar(fw, o, d, inter, new boolean[inter.size()], o, 0, 0, melhor);
        return melhor[0];
    }

    private static void permutar(int[][] fw, int o, int d, List<Integer> inter, boolean[] usada,
                                 int atual, int feitas, int custo, int[] melhor) {
        if (feitas == inter.size()) {
            melhor[0] = Math.min(melhor[0], custo + fw[atual][d]);
            return;
        }
        for (int i = 0; i < inter.size(); i++) {
            if (usada[i]) continue;
            usada[i] = true;
            permutar(fw, o, d, inter, usada, inter.get(i), feitas + 1, custo + fw[atual][inter.get(i)], melhor);
            usada[i] = false;
        }
    }

    private static void testarGestaoDinamica() {
        titulo("Gestão dinâmica da rede (v1 - enunciado)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();

        int novo = g.adicionarVertice("Lviv", 0.1, 0.2);
        igual(12, novo, "índice da cidade nova");
        igual(13, g.getN(), "13 cidades");
        igual(2, g.numeroComponentes(), "cidade nova está isolada (2 componentes)");
        verificarConsistencia(g, "após adicionar cidade");

        igual(false, g.adicionarAresta(novo, TERNOPIL, 5), "criar aresta Lviv-Ternopil");
        igual(1, g.numeroComponentes(), "rede volta a ser conexa");
        igual(true, g.adicionarAresta(TERNOPIL, novo, 7), "atualizar aresta existente (ordem trocada)");
        igual(7, g.getPeso(novo, TERNOPIL), "peso atualizado");
        igual(18, g.getArestas().size(), "18 arestas (não duplicou)");
        verificarConsistencia(g, "após adicionar arestas");

        igual(true, g.removerAresta(novo, TERNOPIL), "remover aresta");
        igual(false, g.removerAresta(novo, TERNOPIL), "remover aresta inexistente devolve false");
        igual(17, g.getArestas().size(), "17 arestas");

        // Remover uma cidade do meio reindexa as seguintes
        g.removerVertice(KIEV);
        igual(12, g.getN(), "12 cidades após remover Kiev");
        igual("Kremenchuck", g.getNome(3), "Kremenchuck passou a índice 3");
        igual(false, g.existeCidade("Kiev"), "Kiev já não existe");
        igual(14, g.getArestas().size(), "14 arestas após remover Kiev (tinha 3 incidentes: Chernihiv, Zhitomyr, Kremenchuck)");
        verificarConsistencia(g, "após remover cidade");
        igual(9, g.getPeso(g.indiceDe("Zhitomyr"), g.indiceDe("Ternopil")) - 3, "Zhitomyr-Ternopil (12) sobrevive à reindexação");

        // Validações
        verificarExcecao(new Runnable() { public void run() { GrafoFerroviarioUcraniano.criarRedeV1().adicionarVertice("kiev", 0, 0); } }, "nome repetido (sem distinguir maiúsculas)");
        verificarExcecao(new Runnable() { public void run() { GrafoFerroviarioUcraniano.criarRedeV1().adicionarVertice("  ", 0, 0); } }, "nome vazio");
        verificarExcecao(new Runnable() { public void run() { GrafoFerroviarioUcraniano.criarRedeV1().adicionarVertice("A;B", 0, 0); } }, "nome com ';'");
        verificarExcecao(new Runnable() { public void run() { GrafoFerroviarioUcraniano.criarRedeV1().adicionarAresta(0, 0, 5); } }, "aresta para a própria cidade");
        verificarExcecao(new Runnable() { public void run() { GrafoFerroviarioUcraniano.criarRedeV1().adicionarAresta(0, 1, 0); } }, "peso zero");
        verificarExcecao(new Runnable() { public void run() { GrafoFerroviarioUcraniano.criarRedeV1().adicionarAresta(0, 99, 5); } }, "cidade inexistente");
    }

    private static void verificarExcecao(Runnable r, String descricao) {
        boolean lancou = false;
        try { r.run(); } catch (IllegalArgumentException e) { lancou = true; }
        verificar(lancou, "rejeita: " + descricao);
    }

    private static void testarDesfazerRefazer() {
        titulo("Desfazer / Refazer (v1 - enunciado)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();
        verificar(!g.podeDesfazer(), "no início não há nada a desfazer");
        g.adicionarAresta(CHERNIHIV, CRIMEIA, 30);
        g.removerVertice(TERNOPIL);
        igual(11, g.getN(), "11 cidades");
        verificar(g.desfazer(), "desfazer remoção da cidade");
        igual(12, g.getN(), "12 cidades de volta");
        igual("Ternopil", g.getNome(TERNOPIL), "Ternopil restaurada no mesmo índice");
        igual(18, g.getArestas().size(), "arestas restauradas (18 com Chernihiv-Crimeia)");
        verificar(g.desfazer(), "desfazer adição da aresta");
        igual(17, g.getArestas().size(), "17 arestas");
        verificar(!g.podeDesfazer(), "nada mais a desfazer");
        verificar(g.refazer(), "refazer adição da aresta");
        igual(30, g.getPeso(CHERNIHIV, CRIMEIA), "aresta refeita");
        g.adicionarVertice("X", 0.5, 0.5);
        verificar(!g.podeRefazer(), "nova alteração apaga o 'refazer'");
        verificarConsistencia(g, "após desfazer/refazer");
    }

    private static void testarPersistencia() throws IOException {
        titulo("Guardar e carregar ficheiro");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();
        g.adicionarVertice("São Paulo", 0.123456789, 0.5);     // acento e decimais longos
        g.adicionarAresta(g.indiceDe("São Paulo"), KIEV, 33);
        g.setUnidade("horas");
        File f = File.createTempFile("rede", ".rede");
        try {
            g.guardar(f);
            GrafoFerroviarioUcraniano h = new GrafoFerroviarioUcraniano(false);
            h.carregar(f);
            igual(g.getN(), h.getN(), "mesmo nº de cidades");
            igual(g.getArestas().size(), h.getArestas().size(), "mesmo nº de arestas");
            igual("horas", h.getUnidade(), "unidade preservada");
            boolean iguais = true;
            for (int i = 0; i < g.getN(); i++) {
                if (!g.getNome(i).equals(h.getNome(i))) iguais = false;
                if (g.getPosicao(i)[0] != h.getPosicao(i)[0] || g.getPosicao(i)[1] != h.getPosicao(i)[1]) iguais = false;
                for (int j = 0; j < g.getN(); j++) if (g.getPeso(i, j) != h.getPeso(i, j)) iguais = false;
            }
            verificar(iguais, "nomes, posições e matriz idênticos após guardar/carregar");
        } finally {
            f.delete();
        }

        // Ficheiros inválidos: a rede atual tem de ficar intacta
        String[] invalidos = {
            "CIDADE;A;0.1;0.1\nARESTA;A;B;5\n",          // cidade B não existe
            "CIDADE;A;0.1;0.1\nCIDADE;a;0.2;0.2\n",      // nome repetido
            "CIDADE;A;0.1;0.1\nCIDADE;B;0.2;0.2\nARESTA;A;B;-3\n",   // peso negativo
            "CIDADE;A;0.1;0.1\nCIDADE;B;0.2;0.2\nARESTA;A;A;3\n",    // laço
            "CIDADE;A;xx;0.1\n",                          // número inválido
            "LIXO;1;2\n",                                 // tipo desconhecido
            "# só comentários\n"                          // sem cidades
        };
        for (String conteudo : invalidos) {
            File t = File.createTempFile("invalida", ".rede");
            try {
                java.nio.file.Files.write(t.toPath(), conteudo.getBytes("UTF-8"));
                GrafoFerroviarioUcraniano h = GrafoFerroviarioUcraniano.criarRedeV1();
                boolean lancou = false;
                try { h.carregar(t); } catch (IOException e) { lancou = true; }
                verificar(lancou, "ficheiro inválido é rejeitado: " + conteudo.replace("\n", " | "));
                igual(12, h.getN(), "rede intacta após ficheiro inválido");
            } finally {
                t.delete();
            }
        }
    }

    private static void testarRedeDesconexa() {
        titulo("Rede desconexa (v1 - enunciado)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();
        g.removerVertice(KHERSON);                       // Crimeia só ligava a Kherson: fica isolada
        int crimeia = g.indiceDe("Crimeia");
        igual(2, g.numeroComponentes(), "2 componentes após remover Kherson");
        verificar(!g.ehConexo(), "rede desconexa");

        Resultado d = new CaminhoDijkstra().executar(g, 0, crimeia);
        verificar(!d.encontrado, "Dijkstra: sem caminho para a cidade isolada");
        Resultado b = new BuscaBFS().executar(g, 0, 0);
        igual(10, b.caminho.size(), "BFS alcança só 10 das 11 cidades");

        Resultado k = new MstKruskal().executar(g, 0, 0);
        Resultado p = new MstPrim().executar(g, 0, 0);
        igual(k.custoTotal, p.custoTotal, "floresta geradora: Prim = Kruskal");
        igual(9, contarAceites(k), "floresta com V - componentes = 9 arestas");

        Resultado rh = new RotaHeuristica().executar(g, 0, 1);
        verificar(rh.encontrado, "heurística funciona na componente de origem");
        verificar(!rh.caminho.contains(crimeia), "heurística não inclui a cidade isolada");
        Resultado ro = new RotaOtima().executar(g, 0, 1);
        verificar(ro.encontrado && ro.custoTotal <= rh.custoTotal, "ótima funciona e é <= heurística");
        Resultado rx = new RotaOtima().executar(g, 0, crimeia);
        verificar(!rx.encontrado, "rota ótima para destino isolado: não encontrada");
        Resultado ry = new RotaHeuristica().executar(g, 0, crimeia);
        verificar(!ry.encontrado, "rota heurística para destino isolado: não encontrada");

        // Rede vazia e rede de 1 cidade não rebentam
        GrafoFerroviarioUcraniano vazio = new GrafoFerroviarioUcraniano(false);
        igual(0, vazio.getN(), "rede vazia");
        igual(0, vazio.numeroComponentes(), "rede vazia: 0 componentes");
        GrafoFerroviarioUcraniano uma = new GrafoFerroviarioUcraniano(false);
        uma.adicionarVertice("Sozinha", 0.5, 0.5);
        igual(0, new RotaOtima().executar(uma, 0, 0).custoTotal, "rota ótima numa rede de 1 cidade");
        igual(0, new MstPrim().executar(uma, 0, 0).custoTotal, "Prim numa rede de 1 cidade");
        igual(0, new MstKruskal().executar(uma, 0, 0).custoTotal, "Kruskal numa rede de 1 cidade");
    }

    private static void testarComparador() {
        titulo("Comparador (v1 - enunciado)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV1();
        ComparadorAlgoritmos.Relatorio rel = new ComparadorAlgoritmos().comparar(g, KIEV, CRIMEIA);
        igual(7, rel.linhas.size(), "7 linhas na tabela comparativa");
        verificar(rel.tudoOk, "todas as verificações cruzadas passam: " + rel.verificacoes);
        for (String v : rel.verificacoes) System.out.println("   " + v);
        ComparadorAlgoritmos.Relatorio rel2 = new ComparadorAlgoritmos().comparar(g, KIEV, KIEV);
        verificar(rel2.tudoOk, "comparador com origem = destino");
    }

    // ------------------------------------------------------------------ geração de redes
    /**
     * Rede aleatória. Começa sempre por uma árvore (logo conexa) e acrescenta arestas extra.
     * Se 'permitirDesconexa', 1 em cada 4 redes perde depois uma aresta e pode ficar desconexa
     * (para testar também esse caso). A força bruta de referência só entende redes conexas.
     */
    private static GrafoFerroviarioUcraniano redeAleatoria(Random rnd, int n, boolean permitirDesconexa) {
        GrafoFerroviarioUcraniano g = new GrafoFerroviarioUcraniano(false);
        for (int i = 0; i < n; i++) g.adicionarVertice("C" + i, rnd.nextDouble(), rnd.nextDouble());
        for (int i = 1; i < n; i++) g.adicionarAresta(i, rnd.nextInt(i), 1 + rnd.nextInt(20));
        int extra = rnd.nextInt(n + 1);
        for (int e = 0; e < extra; e++) {
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            if (a != b) g.adicionarAresta(a, b, 1 + rnd.nextInt(20));
        }
        if (permitirDesconexa && rnd.nextInt(4) == 0 && !g.getArestas().isEmpty()) {
            Aresta a = g.getArestas().get(rnd.nextInt(g.getArestas().size()));
            g.removerAresta(a.u, a.v);
        }
        return g;
    }

    private static void testarRedeExpandidaV2() {
        titulo("Rede expandida (v2 - 16 cidades)");
        GrafoFerroviarioUcraniano g = GrafoFerroviarioUcraniano.criarRedeV2();
        igual(16, g.getN(), "v2: 16 cidades");
        igual(23, g.getArestas().size(), "v2: 23 conexões");
        verificar(g.ehConexo(), "v2 é conexa");
        verificarConsistencia(g, "rede expandida v2");

        // MST: Prim e Kruskal têm de coincidir no custo (243)
        Resultado k = new MstKruskal().executar(g, 0, 0);
        igual(243, k.custoTotal, "v2: custo MST Kruskal = 243");
        igual(15, contarAceites(k), "v2: Kruskal aceita V-1 = 15 arestas");
        for (int s = 0; s < g.getN(); s++) {
            Resultado p = new MstPrim().executar(g, s, 0);
            igual(243, p.custoTotal, "v2: custo MST Prim a partir de " + g.getNome(s));
        }

        // Buscas DFS e BFS alcançam todas as 16 cidades
        for (int s = 0; s < g.getN(); s++) {
            igual(16, new BuscaBFS().executar(g, s, 0).caminho.size(), "v2: BFS alcança 16 cidades a partir de " + g.getNome(s));
            igual(16, new BuscaDFS().executar(g, s, 0).caminho.size(), "v2: DFS alcança 16 cidades a partir de " + g.getNome(s));
        }

        // Dijkstra vs Floyd-Warshall em todos os 256 pares
        int[][] fw = ComparadorAlgoritmos.floydWarshall(g);
        boolean todosDijkstra = true;
        for (int a = 0; a < g.getN(); a++) {
            for (int b = 0; b < g.getN(); b++) {
                Resultado d = new CaminhoDijkstra().executar(g, a, b);
                if (d.custoTotal != fw[a][b] || !caminhoValido(g, d.caminho, d.custoTotal)) {
                    todosDijkstra = false;
                }
            }
        }
        verificar(todosDijkstra, "v2: Dijkstra = Floyd-Warshall em todos os 256 pares de cidades");

        // Rotas de abastecimento no grafo expandido
        int kyiv = g.indiceDe("Kyiv");
        int crimeia = g.indiceDe("Crimeia");
        Resultado h = new RotaHeuristica().executar(g, kyiv, crimeia);
        Resultado o = new RotaOtima().executar(g, kyiv, crimeia);
        verificar(h.encontrado && o.encontrado, "v2: rotas encontradas Kyiv -> Crimeia");
        verificar(passaPorTodas(g, h.caminho), "v2: heurística passa por todas as cidades");
        verificar(passaPorTodas(g, o.caminho), "v2: ótima passa por todas as cidades");
        verificar(o.custoTotal <= h.custoTotal, "v2: ótima <= heurística (" + o.custoTotal + " <= " + h.custoTotal + ")");

        // Comparador completo na rede v2
        ComparadorAlgoritmos.Relatorio rel = new ComparadorAlgoritmos().comparar(g, kyiv, crimeia);
        verificar(rel.tudoOk, "v2: todas as verificações cruzadas do comparador passam");
    }
}
