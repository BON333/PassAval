package EDAII_TP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * MODELO DO GRAFO (só dados, sem algoritmos e sem desenho).
 *
 * Fonte de verdade: a lista de cidades, as suas posições (x,y relativos, 0..1) e a lista de
 * arestas. A Matriz de Adjacências e as Listas de Adjacências são RECONSTRUÍDAS a partir das
 * arestas sempre que a rede muda, por isso nunca ficam inconsistentes entre si.
 *
 * O grafo é NÃO DIRIGIDO e PONDERADO (peso = distância ferroviária, inteiro positivo).
 *
 * Funcionalidades do modelo:
 *  - gestão dinâmica: adicionar/remover vértices e arestas;
 *  - desfazer / refazer (guarda "fotografias" do estado antes de cada alteração);
 *  - guardar / carregar a rede num ficheiro de texto;
 *  - componentes conexas (para avisar quando a rede fica desconexa).
 */
public class GrafoFerroviarioUcraniano {

    /** Número máximo de alterações que se podem desfazer. */
    private static final int LIMITE_DESFAZER = 200;
    private static final int PESO_MAXIMO = 1000000;
    private static final int NOME_MAXIMO = 30;

    /** Fotografia imutável da rede num instante (usada em desfazer/refazer). */
    public static final class Estado {
        private final List<String> cidades;
        private final List<double[]> posicoes;
        private final List<Aresta> arestas;
        private final String unidade;

        private Estado(List<String> cidades, List<double[]> posicoes, List<Aresta> arestas, String unidade) {
            this.cidades = new ArrayList<String>(cidades);
            this.posicoes = new ArrayList<double[]>();
            for (double[] p : posicoes) this.posicoes.add(p.clone());
            this.arestas = new ArrayList<Aresta>(arestas);     // Aresta é imutável: cópia rasa chega
            this.unidade = unidade;
        }
    }

    // ------------------------------------------------------------------
    // Estado do grafo
    // ------------------------------------------------------------------
    private final List<String> cidades = new ArrayList<String>();
    private final List<double[]> posicoes = new ArrayList<double[]>();
    private final List<Aresta> arestas = new ArrayList<Aresta>();
    private String unidade = "km";

    // Estruturas derivadas (reconstruídas por reconstruir())
    private int[][] matriz;
    private List<List<Vizinho>> lista;
    private int[] componente;
    private int numComponentes;

    private final Deque<Estado> pilhaDesfazer = new ArrayDeque<Estado>();
    private final Deque<Estado> pilhaRefazer = new ArrayDeque<Estado>();

    /** Cria o grafo já com a rede ferroviária ucraniana predefinida (v2 - 16 cidades). */
    public GrafoFerroviarioUcraniano() {
        this(true);
    }

    /** @param redePadrao true = carrega a rede predefinida; false = rede vazia. */
    public GrafoFerroviarioUcraniano(boolean redePadrao) {
        if (redePadrao) carregarRedePadrao();
        reconstruir();
    }

    /** Cria uma nova instância com a rede original do enunciado (12 cidades, 17 conexões). */
    public static GrafoFerroviarioUcraniano criarRedeV1() {
        GrafoFerroviarioUcraniano g = new GrafoFerroviarioUcraniano(false);
        g.carregarRedeV1();
        g.reconstruir();
        return g;
    }

    /** Cria uma nova instância com a rede expandida do mapa (16 cidades, 23 conexões). */
    public static GrafoFerroviarioUcraniano criarRedeV2() {
        GrafoFerroviarioUcraniano g = new GrafoFerroviarioUcraniano(false);
        g.carregarRedeV2();
        g.reconstruir();
        return g;
    }

    // ------------------------------------------------------------------
    // Redes pré-configuradas (v1 = enunciado teórico; v2 = mapa completo)
    // ------------------------------------------------------------------

    /** Carrega a rede original do enunciado (12 cidades, 17 conexões). */
    public void carregarRedeV1() {
        cidades.clear();
        posicoes.clear();
        arestas.clear();

        Object[][] c = {
            {"Chernihiv", 0.50, 0.07},   {"Zhitomyr", 0.27, 0.30},   {"Ternopil", 0.06, 0.36},
            {"Kiev", 0.47, 0.27},        {"Kremenchuck", 0.55, 0.50}, {"Luhanski", 0.94, 0.36},
            {"Donetsk", 0.86, 0.52},     {"Mariupol", 0.92, 0.70},    {"Zaporizhia", 0.68, 0.62},
            {"Mykolaiv", 0.38, 0.76},    {"Kherson", 0.52, 0.85},     {"Crimeia", 0.70, 0.95}
        };
        for (Object[] x : c) {
            cidades.add((String) x[0]);
            posicoes.add(new double[]{(Double) x[1], (Double) x[2]});
        }

        int[][] e = {
            {0, 3, 18}, {0, 5, 25}, {1, 2, 12}, {1, 3, 9},  {1, 4, 15},  {3, 4, 11},
            {4, 8, 8},  {4, 9, 14}, {5, 6, 7},  {6, 7, 10}, {6, 8, 9},   {7, 8, 13},
            {7, 10, 16}, {8, 9, 11}, {9, 10, 6}, {10, 11, 20}, {2, 4, 22}
        };
        for (int[] a : e) arestas.add(new Aresta(a[0], a[1], a[2]));
        unidade = "km";
    }

    /** Carrega a rede expandida do mapa da Ucrânia (16 cidades, 23 conexões). */
    public void carregarRedeV2() {
        cidades.clear();
        posicoes.clear();
        arestas.clear();

        Object[][] c = {
            {"Chernihiv", 0.50, 0.07},
            {"Summy", 0.65, 0.12},
            {"Zhitomyr", 0.35, 0.22},
            {"Kyiv", 0.44, 0.24},
            {"Poltava", 0.58, 0.28},
            {"Kharkiv", 0.72, 0.28},
            {"Luhansk", 0.88, 0.32},
            {"Vinnytsa", 0.38, 0.38},
            {"Kropyvnytskyi", 0.52, 0.43},
            {"Dinipro", 0.68, 0.43},
            {"Donetsk", 0.82, 0.47},
            {"Mykolaiv", 0.48, 0.55},
            {"Zaporizhya", 0.68, 0.57},
            {"Kherson", 0.55, 0.65},
            {"Odessa", 0.41, 0.67},
            {"Crimeia", 0.58, 0.80}
        };
        for (Object[] x : c) {
            cidades.add((String) x[0]);
            posicoes.add(new double[]{(Double) x[1], (Double) x[2]});
        }

        int[][] e = {
            // Norte e Centro (Kyiv, Chernihiv, Zhitomyr, Summy, Poltava)
            {0, 3, 14},  // Chernihiv - Kyiv
            {0, 1, 21},  // Chernihiv - Summy
            {2, 3, 14},  // Zhitomyr - Kyiv
            {3, 4, 34},  // Kyiv - Poltava
            {1, 4, 18},  // Summy - Poltava
            {1, 5, 18},  // Summy - Kharkiv

            // Este / Leste (Kharkiv, Luhansk, Dinipro, Donetsk, Zaporizhya)
            {5, 6, 33},  // Kharkiv - Luhansk
            {5, 9, 21},  // Kharkiv - Dinipro
            {4, 5, 14},  // Poltava - Kharkiv
            {4, 9, 20},  // Poltava - Dinipro
            {6, 10, 15}, // Luhansk - Donetsk
            {9, 10, 25}, // Dinipro - Donetsk
            {9, 12, 8},  // Dinipro - Zaporizhya
            {10, 12, 22},// Donetsk - Zaporizhya

            // Sul (Kropyvnytskyi, Vinnytsa, Mykolaiv, Kherson, Odessa, Crimeia)
            {3, 7, 26},  // Kyiv - Vinnytsa
            {7, 8, 18},  // Vinnytsa - Kropyvnytskyi
            {4, 8, 24},  // Poltava - Kropyvnytskyi
            {8, 9, 18},  // Kropyvnytskyi - Dinipro
            {8, 11, 18}, // Kropyvnytskyi - Mykolaiv
            {11, 13, 7},  // Mykolaiv - Kherson
            {11, 14, 13}, // Mykolaiv - Odessa
            {12, 13, 30}, // Zaporizhya - Kherson
            {13, 15, 23}  // Kherson - Crimeia
        };
        for (int[] a : e) arestas.add(new Aresta(a[0], a[1], a[2]));
        unidade = "km";
    }

    private void carregarRedePadrao() {
        carregarRedeV2();
    }

    /** Reconstrói a Matriz, as Listas de Adjacências e as componentes a partir das arestas. */
    private void reconstruir() {
        int n = cidades.size();
        matriz = new int[n][n];
        lista = new ArrayList<List<Vizinho>>();
        for (int i = 0; i < n; i++) lista.add(new ArrayList<Vizinho>());
        for (Aresta a : arestas) {
            matriz[a.u][a.v] = a.peso;                 // grafo não dirigido: matriz simétrica
            matriz[a.v][a.u] = a.peso;
            lista.get(a.u).add(new Vizinho(a.v, a.peso));
            lista.get(a.v).add(new Vizinho(a.u, a.peso));
        }
        // Vizinhos ordenados por índice -> DFS/BFS determinísticos (fácil de explicar na defesa)
        for (List<Vizinho> l : lista) {
            Collections.sort(l, (x, y) -> Integer.compare(x.destino, y.destino));
        }
        calcularComponentes();
    }

    /** Componentes conexas por pesquisa em profundidade iterativa. */
    private void calcularComponentes() {
        int n = cidades.size();
        componente = new int[n];
        java.util.Arrays.fill(componente, -1);
        numComponentes = 0;
        for (int s = 0; s < n; s++) {
            if (componente[s] != -1) continue;
            Deque<Integer> pilha = new ArrayDeque<Integer>();
            pilha.push(s);
            componente[s] = numComponentes;
            while (!pilha.isEmpty()) {
                int u = pilha.pop();
                for (Vizinho v : lista.get(u)) {
                    if (componente[v.destino] == -1) {
                        componente[v.destino] = numComponentes;
                        pilha.push(v.destino);
                    }
                }
            }
            numComponentes++;
        }
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------
    public int getN() { return cidades.size(); }
    public String getNome(int i) { return cidades.get(i); }
    public double[] getPosicao(int i) { return posicoes.get(i); }
    public List<Aresta> getArestas() { return Collections.unmodifiableList(arestas); }
    public List<Vizinho> getVizinhos(int u) { return Collections.unmodifiableList(lista.get(u)); }
    public int getGrau(int u) { return lista.get(u).size(); }
    /** Matriz de adjacências (0 = sem ligação). Não deve ser alterada por quem a consulta. */
    public int[][] getMatriz() { return matriz; }
    /** Peso da ligação u-v, ou 0 se não existir. */
    public int getPeso(int u, int v) { return matriz[u][v]; }
    public boolean temAresta(int u, int v) { return u != v && matriz[u][v] > 0; }
    public String getUnidade() { return unidade; }

    public int indiceDe(String nome) {
        for (int i = 0; i < cidades.size(); i++) if (cidades.get(i).equalsIgnoreCase(nome.trim())) return i;
        return -1;
    }

    public boolean existeCidade(String nome) { return indiceDe(nome) >= 0; }

    public int numeroComponentes() { return numComponentes; }
    public boolean ehConexo() { return getN() <= 1 || numComponentes == 1; }
    /** Índice da componente conexa a que o vértice pertence. */
    public int getComponente(int i) { return componente[i]; }

    /** Lista de componentes conexas (cada uma é uma lista de índices de cidades). */
    public List<List<Integer>> listaComponentes() {
        List<List<Integer>> r = new ArrayList<List<Integer>>();
        for (int c = 0; c < numComponentes; c++) r.add(new ArrayList<Integer>());
        for (int i = 0; i < componente.length; i++) r.get(componente[i]).add(i);
        return r;
    }

    // ------------------------------------------------------------------
    // Desfazer / refazer
    // ------------------------------------------------------------------
    /** Tira uma fotografia do estado atual. */
    public Estado capturar() {
        return new Estado(cidades, posicoes, arestas, unidade);
    }

    /** Regista um estado "anterior" como ponto de retorno (e invalida o "refazer"). */
    public void registarEstado(Estado antes) {
        pilhaDesfazer.push(antes);
        if (pilhaDesfazer.size() > LIMITE_DESFAZER) pilhaDesfazer.removeLast();
        pilhaRefazer.clear();
    }

    private void antesDeAlterar() {
        registarEstado(capturar());
    }

    private void restaurar(Estado e) {
        cidades.clear();
        cidades.addAll(e.cidades);
        posicoes.clear();
        for (double[] p : e.posicoes) posicoes.add(p.clone());
        arestas.clear();
        arestas.addAll(e.arestas);
        unidade = e.unidade;
        reconstruir();
    }

    public boolean podeDesfazer() { return !pilhaDesfazer.isEmpty(); }
    public boolean podeRefazer() { return !pilhaRefazer.isEmpty(); }

    public boolean desfazer() {
        if (pilhaDesfazer.isEmpty()) return false;
        pilhaRefazer.push(capturar());
        restaurar(pilhaDesfazer.pop());
        return true;
    }

    public boolean refazer() {
        if (pilhaRefazer.isEmpty()) return false;
        pilhaDesfazer.push(capturar());
        restaurar(pilhaRefazer.pop());
        return true;
    }

    // ------------------------------------------------------------------
    // Gestão dinâmica da rede (requisito h)
    // ------------------------------------------------------------------
    private static String validarNome(String nome) {
        if (nome == null) throw new IllegalArgumentException("O nome não pode estar vazio.");
        String n = nome.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("O nome não pode estar vazio.");
        if (n.length() > NOME_MAXIMO) throw new IllegalArgumentException("O nome pode ter no máximo " + NOME_MAXIMO + " caracteres.");
        if (n.indexOf(';') >= 0 || n.indexOf('\n') >= 0 || n.indexOf('\r') >= 0)
            throw new IllegalArgumentException("O nome não pode conter ';' nem mudanças de linha.");
        return n;
    }

    private static double limitar01(double x) {
        return Math.min(1.0, Math.max(0.0, x));
    }

    /** Acrescenta uma cidade. Devolve o índice do novo vértice. */
    public int adicionarVertice(String nome, double x, double y) {
        String n = validarNome(nome);
        if (existeCidade(n)) throw new IllegalArgumentException("Já existe uma cidade chamada \"" + n + "\".");
        antesDeAlterar();
        cidades.add(n);
        posicoes.add(new double[]{limitar01(x), limitar01(y)});
        reconstruir();
        return cidades.size() - 1;
    }

    /** Remove a cidade e todas as suas ligações; os vértices seguintes são reindexados (-1). */
    public void removerVertice(int idx) {
        if (idx < 0 || idx >= cidades.size()) throw new IllegalArgumentException("Cidade inválida.");
        antesDeAlterar();
        List<Aresta> nova = new ArrayList<Aresta>();
        for (Aresta a : arestas) {
            if (a.u == idx || a.v == idx) continue;               // aresta incidente: desaparece
            nova.add(new Aresta(a.u > idx ? a.u - 1 : a.u, a.v > idx ? a.v - 1 : a.v, a.peso));
        }
        arestas.clear();
        arestas.addAll(nova);
        cidades.remove(idx);
        posicoes.remove(idx);
        reconstruir();
    }

    /**
     * Adiciona a ligação u-v; se já existir, atualiza o peso.
     * @return true se a ligação já existia (foi atualizada), false se foi criada.
     */
    public boolean adicionarAresta(int u, int v, int peso) {
        int n = cidades.size();
        if (u < 0 || v < 0 || u >= n || v >= n) throw new IllegalArgumentException("Cidade inválida.");
        if (u == v) throw new IllegalArgumentException("Não é possível ligar uma cidade a ela mesma.");
        if (peso <= 0) throw new IllegalArgumentException("O peso deve ser um inteiro positivo.");
        if (peso > PESO_MAXIMO) throw new IllegalArgumentException("O peso máximo é " + PESO_MAXIMO + ".");
        antesDeAlterar();
        for (int i = 0; i < arestas.size(); i++) {
            if (arestas.get(i).liga(u, v)) {
                arestas.set(i, new Aresta(u, v, peso));
                reconstruir();
                return true;
            }
        }
        arestas.add(new Aresta(u, v, peso));
        reconstruir();
        return false;
    }

    /** Remove a ligação u-v. Devolve false (sem alterar nada) se ela não existia. */
    public boolean removerAresta(int u, int v) {
        for (int i = 0; i < arestas.size(); i++) {
            if (arestas.get(i).liga(u, v)) {
                antesDeAlterar();
                arestas.remove(i);
                reconstruir();
                return true;
            }
        }
        return false;
    }

    /** Move uma cidade no mapa. Não é registado em "desfazer" (o painel regista no fim do arrasto). */
    public void setPosicao(int i, double x, double y) {
        posicoes.get(i)[0] = limitar01(x);
        posicoes.get(i)[1] = limitar01(y);
    }

    public void setUnidade(String nova) {
        if (nova == null || nova.trim().isEmpty()) throw new IllegalArgumentException("A unidade não pode estar vazia.");
        String u = nova.trim();
        if (u.length() > 12 || u.indexOf(';') >= 0) throw new IllegalArgumentException("Unidade inválida (máx. 12 caracteres, sem ';').");
        antesDeAlterar();
        unidade = u;
    }

    /** Remove tudo (rede vazia). */
    public void limpar() {
        antesDeAlterar();
        cidades.clear();
        posicoes.clear();
        arestas.clear();
        reconstruir();
    }

    /** Volta à rede ferroviária ucraniana predefinida. */
    public void reporRedePadrao() {
        antesDeAlterar();
        cidades.clear();
        posicoes.clear();
        arestas.clear();
        carregarRedePadrao();
        reconstruir();
    }

    /** Repõe a rede teórica original do enunciado (12 cidades, 17 conexões). */
    public void reporRedeV1() {
        antesDeAlterar();
        cidades.clear();
        posicoes.clear();
        arestas.clear();
        carregarRedeV1();
        reconstruir();
    }

    /** Repõe a rede expandida do mapa (16 cidades, 23 conexões). */
    public void reporRedeV2() {
        antesDeAlterar();
        cidades.clear();
        posicoes.clear();
        arestas.clear();
        carregarRedeV2();
        reconstruir();
    }

    // ------------------------------------------------------------------
    // Persistência (ficheiro de texto UTF-8, linhas separadas por ';')
    //
    //   # comentário
    //   UNIDADE;km
    //   CIDADE;Kiev;0.47;0.27
    //   ARESTA;Kiev;Zhitomyr;9
    //
    // As arestas referem as cidades pelo NOME (e não pelo índice): o ficheiro continua válido
    // mesmo que se removam cidades e os índices mudem.
    // ------------------------------------------------------------------
    public void guardar(File f) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
        try {
            w.write("# Rede ferroviaria - AED II (ISCTEM)");
            w.newLine();
            w.write("# UNIDADE;texto | CIDADE;nome;x;y | ARESTA;cidadeA;cidadeB;peso");
            w.newLine();
            w.write("UNIDADE;" + unidade);
            w.newLine();
            for (int i = 0; i < cidades.size(); i++) {
                w.write("CIDADE;" + cidades.get(i) + ";" + posicoes.get(i)[0] + ";" + posicoes.get(i)[1]);
                w.newLine();
            }
            for (Aresta a : arestas) {
                w.write("ARESTA;" + cidades.get(a.u) + ";" + cidades.get(a.v) + ";" + a.peso);
                w.newLine();
            }
        } finally {
            w.close();
        }
    }

    /**
     * Carrega uma rede de ficheiro. Valida tudo ANTES de alterar o grafo: se o ficheiro estiver
     * errado, lança IOException com a linha do problema e a rede atual fica intacta.
     */
    public void carregar(File f) throws IOException {
        List<String> nomes = new ArrayList<String>();
        List<double[]> pos = new ArrayList<double[]>();
        List<Aresta> novas = new ArrayList<Aresta>();
        String novaUnidade = "km";

        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
        try {
            String linha;
            int numLinha = 0;
            while ((linha = r.readLine()) != null) {
                numLinha++;
                linha = linha.trim();
                if (numLinha == 1 && linha.startsWith("﻿")) linha = linha.substring(1).trim();   // BOM
                if (linha.isEmpty() || linha.startsWith("#")) continue;
                String[] p = linha.split(";", -1);
                String tipo = p[0].trim().toUpperCase();
                try {
                    if (tipo.equals("UNIDADE")) {
                        if (p.length != 2 || p[1].trim().isEmpty()) throw new IOException("UNIDADE precisa de 1 valor.");
                        novaUnidade = p[1].trim();
                    } else if (tipo.equals("CIDADE")) {
                        if (p.length != 4) throw new IOException("CIDADE precisa de: nome;x;y.");
                        String nome = validarNome(p[1]);
                        for (String ja : nomes) if (ja.equalsIgnoreCase(nome)) throw new IOException("Cidade repetida: " + nome);
                        nomes.add(nome);
                        pos.add(new double[]{limitar01(Double.parseDouble(p[2].trim())), limitar01(Double.parseDouble(p[3].trim()))});
                    } else if (tipo.equals("ARESTA")) {
                        if (p.length != 4) throw new IOException("ARESTA precisa de: cidadeA;cidadeB;peso.");
                        int a = indice(nomes, p[1]), b = indice(nomes, p[2]);
                        if (a < 0) throw new IOException("Cidade desconhecida: " + p[1].trim() + " (declare as CIDADE antes das ARESTA).");
                        if (b < 0) throw new IOException("Cidade desconhecida: " + p[2].trim() + " (declare as CIDADE antes das ARESTA).");
                        if (a == b) throw new IOException("Uma aresta não pode ligar a cidade a ela própria.");
                        int peso = Integer.parseInt(p[3].trim());
                        if (peso <= 0 || peso > PESO_MAXIMO) throw new IOException("Peso inválido: " + peso);
                        for (Aresta e : novas) if (e.liga(a, b)) throw new IOException("Aresta repetida: " + p[1].trim() + " - " + p[2].trim());
                        novas.add(new Aresta(a, b, peso));
                    } else {
                        throw new IOException("Tipo de linha desconhecido: " + p[0].trim());
                    }
                } catch (NumberFormatException e) {
                    throw new IOException("Linha " + numLinha + ": número inválido.");
                } catch (IllegalArgumentException e) {
                    throw new IOException("Linha " + numLinha + ": " + e.getMessage());
                } catch (IOException e) {
                    throw new IOException("Linha " + numLinha + ": " + e.getMessage());
                }
            }
        } finally {
            r.close();
        }
        if (nomes.isEmpty()) throw new IOException("O ficheiro não contém nenhuma cidade.");

        // Tudo válido: agora sim, substitui a rede (com ponto de retorno para "desfazer")
        antesDeAlterar();
        cidades.clear();
        cidades.addAll(nomes);
        posicoes.clear();
        posicoes.addAll(pos);
        arestas.clear();
        arestas.addAll(novas);
        unidade = novaUnidade;
        reconstruir();
    }

    private static int indice(List<String> nomes, String nome) {
        for (int i = 0; i < nomes.size(); i++) if (nomes.get(i).equalsIgnoreCase(nome.trim())) return i;
        return -1;
    }

    // ------------------------------------------------------------------
    // Representações em texto (para o console e para o ficheiro de log)
    // ------------------------------------------------------------------
    public String matrizComoTexto() {
        int n = getN();
        StringBuilder sb = new StringBuilder("MATRIZ DE ADJACÊNCIAS (0 = sem ligação; valores em " + unidade + ")\nLegenda:\n");
        for (int i = 0; i < n; i++) sb.append(String.format("  [%2d] %s\n", i, cidades.get(i)));
        sb.append("\n     ");
        for (int j = 0; j < n; j++) sb.append(String.format("%4d", j));
        sb.append("\n     ");
        for (int j = 0; j < n; j++) sb.append("----");
        sb.append("\n");
        for (int i = 0; i < n; i++) {
            sb.append(String.format("%2d | ", i));
            for (int j = 0; j < n; j++) sb.append(String.format("%4d", matriz[i][j]));
            sb.append("\n");
        }
        return sb.toString();
    }

    public String listaComoTexto() {
        StringBuilder sb = new StringBuilder("LISTAS DE ADJACÊNCIAS  (cidade -> vizinho:peso em " + unidade + ")\n");
        for (int i = 0; i < getN(); i++) {
            sb.append(String.format("%-12s[%2d] -> ", cidades.get(i), i));
            List<Vizinho> l = lista.get(i);
            if (l.isEmpty()) sb.append("(vazia)");
            for (int k = 0; k < l.size(); k++) {
                sb.append(cidades.get(l.get(k).destino)).append(":").append(l.get(k).peso);
                if (k < l.size() - 1) sb.append(" -> ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
