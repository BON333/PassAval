package EDAII_TP;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

/**
 * VISUALIZADOR 2D DO GRAFO (JPanel customizado com paintComponent).
 *
 * Desenho:
 *  - arestas com o peso num "selo" a meio; vértices como círculos com o índice e o nome por baixo;
 *  - o resultado do algoritmo aparece a cores, com a aresta/cidade ATUAL realçada a amarelo;
 *  - números junto das cidades mostram a ORDEM DE VISITA (1, 2, 3...); numa rota que atravessa
 *    a mesma cidade mais do que uma vez aparecem vários números (ex.: 3·9);
 *  - arestas rejeitadas (ciclos no Kruskal) aparecem a tracejado;
 *  - faixa vermelha quando a rede está desconexa.
 *
 * Animação passo a passo: o painel guarda a lista de Passo e uma posição; mostra os passos
 * 0..posição. Pode-se reproduzir, pausar, avançar, recuar, ir ao início/fim e mudar a velocidade.
 *
 * Interação com o rato:
 *  - ARRASTAR uma cidade -> move-a;          - CLIQUE numa cidade -> define a origem (anel verde);
 *  - BOTÃO DIREITO numa cidade -> destino (anel vermelho);
 *  - DUPLO CLIQUE numa aresta -> editar peso / remover;
 *  - DUPLO CLIQUE em zona vazia -> nova cidade nesse ponto.
 */
public class PainelGrafo extends JPanel {

    /** O painel avisa a janela principal de tudo o que o utilizador fizer com o rato. */
    public interface Ouvinte {
        void origemEscolhida(int idx);
        void destinoEscolhido(int idx);
        void editarAresta(int u, int v);
        void adicionarCidadeEm(double rx, double ry);
        void redeMovida();
        void reproducaoAlterada();
    }

    private static final int RAIO = 22;
    private static final int MARGEM = 64;
    private static final Color COR_ARESTA = new Color(140, 150, 168);
    private static final Color COR_NO = new Color(45, 85, 150);
    private static final Color COR_ATUAL = new Color(255, 205, 0);

    private final GrafoFerroviarioUcraniano grafo;
    private Ouvinte ouvinte;

    // --- animação ---
    private final List<Passo> passos = new ArrayList<Passo>();
    private int pos = 0;                                  // nº de passos já mostrados
    private Color corDest = Color.RED;
    private String titulo = "";
    private boolean numerar = true;
    private final Timer timer;

    // --- estado derivado dos passos 0..pos (recalculado por recalcular()) ---
    private final Set<String> arestasDest = new HashSet<String>();
    private final Set<String> rejeitadas = new HashSet<String>();
    private final Set<Integer> nosDest = new HashSet<Integer>();
    private final Map<Integer, List<Integer>> ordem = new HashMap<Integer, List<Integer>>();
    private Passo ultimo = null;

    // --- interação ---
    private int origem = -1, destino = -1;
    private int arrastando = -1;
    private boolean moveu = false;
    private int pressX, pressY;
    private GrafoFerroviarioUcraniano.Estado estadoAntes;

    public PainelGrafo(GrafoFerroviarioUcraniano grafo) {
        this.grafo = grafo;
        setBackground(new Color(238, 243, 250));
        setPreferredSize(new Dimension(800, 520));
        ToolTipManager.sharedInstance().registerComponent(this);

        timer = new Timer(500, e -> tique());
        timer.setRepeats(true);

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                pressX = e.getX();
                pressY = e.getY();
                moveu = false;
                arrastando = noEm(e.getX(), e.getY());
                estadoAntes = arrastando >= 0 ? grafo.capturar() : null;
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (arrastando < 0 || !SwingUtilities.isLeftMouseButton(e)) return;
                if (!moveu && Point2D.distance(pressX, pressY, e.getX(), e.getY()) < 4) return;   // tolerância de clique
                moveu = true;
                grafo.setPosicao(arrastando, relX(e.getX()), relY(e.getY()));
                repaint();
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {                       // botão direito: destino
                    int i = noEm(e.getX(), e.getY());
                    if (i >= 0 && ouvinte != null) ouvinte.destinoEscolhido(i);
                    return;
                }
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (arrastando >= 0 && moveu) {                                   // fim de um arrasto
                    grafo.registarEstado(estadoAntes);                            // ponto de retorno (desfazer)
                    if (ouvinte != null) ouvinte.redeMovida();
                } else if (ouvinte != null) {                                     // clique simples / duplo
                    int i = noEm(e.getX(), e.getY());
                    if (e.getClickCount() >= 2) {
                        if (i < 0) {
                            Aresta a = arestaEm(e.getX(), e.getY());
                            if (a != null) ouvinte.editarAresta(a.u, a.v);
                            else ouvinte.adicionarCidadeEm(relX(e.getX()), relY(e.getY()));
                        }
                    } else if (i >= 0) {
                        ouvinte.origemEscolhida(i);
                    }
                }
                arrastando = -1;
                estadoAntes = null;
            }

            @Override public void mouseMoved(MouseEvent e) {
                boolean sobre = noEm(e.getX(), e.getY()) >= 0 || arestaEm(e.getX(), e.getY()) != null;
                setCursor(Cursor.getPredefinedCursor(sobre ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setOuvinte(Ouvinte o) {
        this.ouvinte = o;
    }

    // =====================================================================
    // Origem / destino
    // =====================================================================
    public void setOrigemDestino(int o, int d) {
        origem = o;
        destino = d;
        repaint();
    }

    // =====================================================================
    // Animação (controlos de reprodução)
    // =====================================================================
    /** Carrega os passos de um resultado e começa a reproduzi-los. */
    public void carregar(Resultado r, Color cor, String titulo) {
        timer.stop();
        passos.clear();
        passos.addAll(r.passos);
        corDest = cor;
        this.titulo = titulo;
        numerar = r.numerar;
        pos = 0;
        if (!passos.isEmpty()) {
            pos = 1;                                   // mostra já o primeiro passo
            recalcular();
            if (passos.size() > 1) timer.start();
        } else {
            recalcular();
        }
        notificar();
    }

    /** Apaga os destaques e pára a animação. */
    public void limparDestaques() {
        timer.stop();
        passos.clear();
        pos = 0;
        titulo = "";
        recalcular();
        notificar();
    }

    public void reproduzir() {
        if (passos.isEmpty()) return;
        if (pos >= passos.size()) { pos = 0; recalcular(); }          // "repetir" desde o início
        timer.start();
        notificar();
    }

    public void pausar() {
        timer.stop();
        notificar();
    }

    public void alternar() {
        if (timer.isRunning()) pausar(); else reproduzir();
    }

    public void proximo() {
        timer.stop();
        if (pos < passos.size()) { pos++; recalcular(); }
        notificar();
    }

    public void anterior() {
        timer.stop();
        if (pos > 1) { pos--; recalcular(); }          // pára no 1.º passo (nunca deixa o mapa em branco)
        notificar();
    }

    public void inicio() {
        timer.stop();
        pos = passos.isEmpty() ? 0 : 1;
        recalcular();
        notificar();
    }

    public void fim() {
        timer.stop();
        pos = passos.size();
        recalcular();
        notificar();
    }

    /** Intervalo entre passos, em milissegundos. */
    public void setAtraso(int ms) {
        timer.setDelay(ms);
        timer.setInitialDelay(ms);
    }

    public boolean isReproduzindo() { return timer.isRunning(); }
    public int getPosicao() { return pos; }
    public int getTotalPassos() { return passos.size(); }

    private void tique() {
        if (pos < passos.size()) { pos++; recalcular(); }
        if (pos >= passos.size()) timer.stop();
        notificar();
    }

    private void notificar() {
        if (ouvinte != null) ouvinte.reproducaoAlterada();
        repaint();
    }

    /** Reconstrói o que está destacado a partir dos passos 0..pos. */
    private void recalcular() {
        arestasDest.clear();
        rejeitadas.clear();
        nosDest.clear();
        ordem.clear();
        ultimo = null;
        int contador = 0;
        for (int i = 0; i < pos && i < passos.size(); i++) {
            Passo p = passos.get(i);
            ultimo = p;
            if (p.isNo()) {
                nosDest.add(p.u);
                if (numerar) acrescentarOrdem(p.u, ++contador);
            } else if (p.rejeitado) {
                rejeitadas.add(Aresta.chave(p.u, p.v));
            } else {
                arestasDest.add(Aresta.chave(p.u, p.v));
                nosDest.add(p.u);
                nosDest.add(p.v);
                if (numerar) acrescentarOrdem(p.v, ++contador);       // chegada a v
            }
        }
        repaint();
    }

    private void acrescentarOrdem(int no, int numero) {
        List<Integer> l = ordem.get(no);
        if (l == null) { l = new ArrayList<Integer>(); ordem.put(no, l); }
        l.add(numero);
    }

    // =====================================================================
    // Coordenadas e deteção (hit-test)
    // =====================================================================
    private int px(double rx) { return MARGEM + (int) Math.round(rx * Math.max(1, getWidth() - 2 * MARGEM)); }
    private int py(double ry) { return MARGEM + (int) Math.round(ry * Math.max(1, getHeight() - 2 * MARGEM)); }
    private double relX(int x) { return Math.min(1, Math.max(0, (x - MARGEM) / Math.max(1.0, getWidth() - 2.0 * MARGEM))); }
    private double relY(int y) { return Math.min(1, Math.max(0, (y - MARGEM) / Math.max(1.0, getHeight() - 2.0 * MARGEM))); }

    private Point ponto(int i) {
        double[] p = grafo.getPosicao(i);
        return new Point(px(p[0]), py(p[1]));
    }

    /** Vértice sob o ponto (x,y), ou -1. Percorre de trás para a frente (o último está por cima). */
    private int noEm(int x, int y) {
        for (int i = grafo.getN() - 1; i >= 0; i--) {
            Point p = ponto(i);
            if (Point2D.distance(x, y, p.x, p.y) <= RAIO) return i;
        }
        return -1;
    }

    /** Aresta mais próxima do ponto (a menos de 7 px), ou null. */
    private Aresta arestaEm(int x, int y) {
        Aresta melhor = null;
        double melhorDist = 7.0;
        for (Aresta a : grafo.getArestas()) {
            Point p1 = ponto(a.u), p2 = ponto(a.v);
            double d = Line2D.ptSegDist(p1.x, p1.y, p2.x, p2.y, x, y);
            if (d < melhorDist) { melhorDist = d; melhor = a; }
        }
        return melhor;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = noEm(e.getX(), e.getY());
        if (i >= 0) return grafo.getNome(i) + " [" + i + "] - " + grafo.getGrau(i) + " ligações";
        Aresta a = arestaEm(e.getX(), e.getY());
        if (a != null) {
            return grafo.getNome(a.u) + " - " + grafo.getNome(a.v) + ": " + a.peso + " " + grafo.getUnidade()
                    + "  (duplo clique para editar)";
        }
        return null;
    }

    // =====================================================================
    // Exportação
    // =====================================================================
    /** Guarda o mapa tal como está no ecrã numa imagem PNG. */
    public void exportarPNG(File ficheiro) throws IOException {
        BufferedImage img = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        paint(g2);
        g2.dispose();
        if (!ImageIO.write(img, "png", ficheiro)) throw new IOException("Formato PNG não suportado.");
    }

    // =====================================================================
    // Desenho
    // =====================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int n = grafo.getN();
        List<Aresta> arestas = grafo.getArestas();

        // 1) arestas "normais" e rejeitadas (por baixo de tudo)
        for (Aresta a : arestas) {
            String k = a.chave();
            if (arestasDest.contains(k)) continue;
            Point p1 = ponto(a.u), p2 = ponto(a.v);
            if (rejeitadas.contains(k)) {
                g2.setColor(new Color(214, 70, 70, 190));
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{8f, 6f}, 0f));
            } else {
                g2.setColor(COR_ARESTA);
                g2.setStroke(new BasicStroke(2f));
            }
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // 2) halo amarelo na aresta do passo atual
        if (ultimo != null && !ultimo.isNo() && ultimo.u < n && ultimo.v < n) {
            Point p1 = ponto(ultimo.u), p2 = ponto(ultimo.v);
            g2.setColor(new Color(255, 205, 0, 150));
            g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // 3) arestas do resultado
        for (Aresta a : arestas) {
            if (!arestasDest.contains(a.chave())) continue;
            Point p1 = ponto(a.u), p2 = ponto(a.v);
            g2.setColor(corDest);
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // 4) pesos no meio de cada aresta
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        for (Aresta a : arestas) {
            Point p1 = ponto(a.u), p2 = ponto(a.v);
            int mx = (p1.x + p2.x) / 2, my = (p1.y + p2.y) / 2;
            String txt = String.valueOf(a.peso);
            int w = fm.stringWidth(txt) + 10, h = fm.getHeight();
            String k = a.chave();
            boolean dest = arestasDest.contains(k);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(mx - w / 2, my - h / 2, w, h, 10, 10);
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(dest ? corDest : (rejeitadas.contains(k) ? new Color(214, 70, 70) : COR_ARESTA));
            g2.drawRoundRect(mx - w / 2, my - h / 2, w, h, 10, 10);
            g2.setColor(new Color(30, 40, 60));
            g2.drawString(txt, mx - fm.stringWidth(txt) / 2, my + fm.getAscent() / 2 - 1);
        }

        // 5) vértices
        int noAtual = ultimo == null ? -1 : (ultimo.isNo() ? ultimo.u : ultimo.v);
        for (int i = 0; i < n; i++) {
            Point p = ponto(i);
            int x = p.x, y = p.y;
            g2.setColor(nosDest.contains(i) ? corDest : COR_NO);
            g2.fill(new Ellipse2D.Double(x - RAIO, y - RAIO, 2 * RAIO, 2 * RAIO));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Double(x - RAIO, y - RAIO, 2 * RAIO, 2 * RAIO));

            if (i == noAtual) {                                           // cidade do passo atual
                g2.setColor(COR_ATUAL);
                g2.setStroke(new BasicStroke(4f));
                g2.draw(new Ellipse2D.Double(x - RAIO - 3, y - RAIO - 3, 2 * RAIO + 6, 2 * RAIO + 6));
            }
            g2.setStroke(new BasicStroke(4f));
            if (i == origem) {                                            // anel verde = origem
                g2.setColor(new Color(0, 190, 80));
                g2.draw(new Ellipse2D.Double(x - RAIO - 8, y - RAIO - 8, 2 * RAIO + 16, 2 * RAIO + 16));
            }
            if (i == destino) {                                           // anel vermelho = destino
                int extra = (i == origem) ? 14 : 8;
                g2.setColor(new Color(225, 45, 45));
                g2.draw(new Ellipse2D.Double(x - RAIO - extra, y - RAIO - extra, 2 * (RAIO + extra), 2 * (RAIO + extra)));
            }

            // índice dentro do círculo
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            fm = g2.getFontMetrics();
            String idx = String.valueOf(i);
            g2.setColor(Color.WHITE);
            g2.drawString(idx, x - fm.stringWidth(idx) / 2, y + fm.getAscent() / 2 - 1);

            // nome por baixo
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            fm = g2.getFontMetrics();
            String nome = grafo.getNome(i);
            int w = fm.stringWidth(nome) + 8;
            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillRoundRect(x - w / 2, y + RAIO + 5, w, fm.getHeight(), 8, 8);
            g2.setColor(new Color(20, 30, 50));
            g2.drawString(nome, x - fm.stringWidth(nome) / 2, y + RAIO + 5 + fm.getAscent());
        }

        // 6) ordem de visita (selos numerados junto às cidades)
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        fm = g2.getFontMetrics();
        for (Map.Entry<Integer, List<Integer>> en : ordem.entrySet()) {
            int i = en.getKey();
            if (i >= n) continue;
            Point p = ponto(i);
            String txt = textoOrdem(en.getValue());
            int w = Math.max(fm.stringWidth(txt) + 10, 20), h = 18;
            int bx = p.x + RAIO - 6, by = p.y - RAIO - 14;
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(bx, by, w, h, 12, 12);
            g2.setColor(corDest);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx, by, w, h, 12, 12);
            g2.setColor(new Color(20, 30, 50));
            g2.drawString(txt, bx + (w - fm.stringWidth(txt)) / 2, by + (h + fm.getAscent()) / 2 - 2);
        }

        desenharTextos(g2);
        g2.dispose();
    }

    /** "3" ou "3·9·14" (no máximo 3 números, depois "..."). */
    private String textoOrdem(List<Integer> numeros) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < numeros.size() && k < 3; k++) {
            if (k > 0) sb.append('·');
            sb.append(numeros.get(k));
        }
        if (numeros.size() > 3) sb.append("...");
        return sb.toString();
    }

    /** Título, legenda do passo, aviso de rede desconexa, dicas e unidade. */
    private void desenharTextos(Graphics2D g2) {
        int larg = getWidth();

        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        boolean temTitulo = !titulo.isEmpty();
        g2.setColor(temTitulo ? corDest.darker() : new Color(110, 120, 140));
        g2.drawString(elipse(temTitulo ? titulo : "Rede Ferroviária Ucraniana", fm, larg - 300), 14, 24);

        if (ultimo != null) {                                             // legenda do passo atual
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            fm = g2.getFontMetrics();
            String t = "Passo " + pos + "/" + passos.size() + ":  " + elipse(ultimo.texto, fm, larg - 150);
            int w = fm.stringWidth(t) + 16;
            g2.setColor(new Color(255, 255, 255, 235));
            g2.fillRoundRect(10, 32, w, 24, 10, 10);
            g2.setColor(COR_ATUAL.darker());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(10, 32, w, 24, 10, 10);
            g2.setColor(new Color(30, 40, 60));
            g2.drawString(t, 18, 49);
        }

        if (!grafo.ehConexo()) {                                          // aviso de rede desconexa
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            fm = g2.getFontMetrics();
            String t = "Rede desconexa: " + grafo.numeroComponentes() + " componentes";
            int w = fm.stringWidth(t) + 18;
            g2.setColor(new Color(200, 40, 40));
            g2.fillRoundRect(larg - w - 12, 8, w, 24, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawString(t, larg - w - 3, 25);
        }

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(100, 110, 130));
        int base = getHeight() - 8;
        g2.drawString("Arraste = mover cidade  |  clique = origem (anel verde)  |  botão direito = destino (anel vermelho)", 14, base - 14);
        g2.drawString("Duplo clique numa aresta = editar peso  |  duplo clique em zona vazia = nova cidade", 14, base);
        String un = "Pesos em " + grafo.getUnidade();
        g2.drawString(un, larg - fm.stringWidth(un) - 14, base);
    }

    /** Corta o texto com "..." para caber em 'max' pixéis. */
    private String elipse(String s, FontMetrics fm, int max) {
        if (max < 40 || fm.stringWidth(s) <= max) return s;
        while (s.length() > 3 && fm.stringWidth(s + "...") > max) s = s.substring(0, s.length() - 1);
        return s + "...";
    }
}
