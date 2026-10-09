package EDAII_TP;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * JANELA PRINCIPAL (camada de interface).
 *
 * Esquerda : escolha de origem/destino e botoes (algoritmos, gestão da rede).
 * Centro   : barra de animacao + PainelGrafo (mapa interativo) e, por baixo, abas:
 *            Console | Matriz de Adjacencia | Listas de Adjacencia | Comparacao.
 * Rodapé   : barra de estado (n de cidades, ligacoes, componentes...).
 *
 
 */
public class MainFrame extends JFrame implements PainelGrafo.Ouvinte 
{

    // Cor de cada algoritmo no mapa
    private static final Color COR_DFS = new Color(220, 50, 47);        // vermelho
    private static final Color COR_BFS = new Color(0, 120, 255);        // azul brilhante
    private static final Color COR_PRIM = new Color(0, 170, 70);        // verde
    private static final Color COR_KRUSKAL = new Color(160, 60, 200);   // roxo
    private static final Color COR_DIJKSTRA = new Color(255, 140, 0);   // laranja
    private static final Color COR_ROTA_H = new Color(0, 160, 160);     // turquesa
    private static final Color COR_ROTA_O = new Color(200, 40, 120);    // magenta

    private static final int ABA_CONSOLE = 0, ABA_MATRIZ = 1, ABA_LISTAS = 2, ABA_COMPARACAO = 3;

    // Modelo, vista e algoritmos
    private final GrafoFerroviarioUcraniano grafo = new GrafoFerroviarioUcraniano();
    private final PainelGrafo painel = new PainelGrafo(grafo);
    private final Algoritmo dfs = new BuscaDFS();
    private final Algoritmo bfs = new BuscaBFS();
    private final Algoritmo prim = new MstPrim();
    private final Algoritmo kruskal = new MstKruskal();
    private final Algoritmo dijkstra = new CaminhoDijkstra();
    private final Algoritmo rotaHeuristica = new RotaHeuristica();
    private final Algoritmo rotaOtima = new RotaOtima();

    // Componentes
    private final JComboBox<String> cbOrigem = new JComboBox<String>();
    private final JComboBox<String> cbDestino = new JComboBox<String>();
    private final JTextArea console = new JTextArea();
    private final JTextArea areaListas = new JTextArea();
    private final DefaultTableModel modeloMatriz = new DefaultTableModel();
    private final JTable tabelaMatriz = new JTable(modeloMatriz);
    private final DefaultTableModel modeloComparacao = new DefaultTableModel(
            new Object[]{"Algoritmo", "Custo total", "Resultado", "Tempo médio (µs)", "Observação"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTextArea areaVerificacoes = new JTextArea();
    private final JLabel lblComparacao = new JLabel(" ");
    private final JLabel lblEstado = new JLabel(" ");
    private final JLabel lblPasso = new JLabel("Sem animação");
    private JTabbedPane abas;
    private JSplitPane split;
    private JSlider sliderVelocidade;
    private BotaoModerno btnInicio, btnAnterior, btnPlay, btnProximo, btnFim;
    private BotaoModerno btnDesfazer, btnRefazer;
    private JMenuItem miDesfazer, miRefazer;

    private boolean atualizandoCombos = false;
    private File ultimaPasta = null;
    private final Random aleatorio = new Random();

    public MainFrame() 
    {
        super("Rede Ferroviária Ucraniana - Algoritmos e Estruturas de Dados II (ISCTEM)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        setJMenuBar(criarMenu());
        add(criarPainelControlo(), BorderLayout.WEST);
        add(criarAreaCentral(), BorderLayout.CENTER);
        add(criarBarraEstado(), BorderLayout.SOUTH);

        // Sincroniza os anéis origem/destino do mapa com as ComboBox
        java.awt.event.ItemListener il = e -> {
            if (!atualizandoCombos && e.getStateChange() == ItemEvent.SELECTED) {
                painel.setOrigemDestino(cbOrigem.getSelectedIndex(), cbDestino.getSelectedIndex());
                atualizarEstado();
            }
        };
        cbOrigem.addItemListener(il);
        cbDestino.addItemListener(il);

        painel.setAtraso(2000 / sliderVelocidade.getValue());
        painel.setOuvinte(this);                       // só agora que a barra de animação já existe
        refrescar(null, null);

        setSize(1440, 920);
        setLocationRelativeTo(null);
        split.setDividerLocation(540);
        log("Bem-vindo! Escolha origem e destino (ComboBox ou cliques no mapa) e execute um algoritmo.");
        log("Clique = origem | botão direito = destino | arraste = mover | duplo clique = editar aresta / nova cidade.");
    }

    // =====================================================================
    // Construção da interface
    // =====================================================================
    private JMenuBar criarMenu() 
    {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        JMenuBar barra = new JMenuBar();

        JMenu ficheiro = new JMenu("Ficheiro");
        ficheiro.add(item("Nova rede vazia", null, e -> novaRedeVazia()));
        ficheiro.add(item("Abrir rede...", KeyStroke.getKeyStroke(KeyEvent.VK_O, mask), e -> abrirRede()));
        ficheiro.add(item("Guardar rede...", KeyStroke.getKeyStroke(KeyEvent.VK_S, mask), e -> guardarRede()));
        ficheiro.add(item("Repor rede predefinida (Ucrânia)", null, e -> reporRede()));
        ficheiro.addSeparator();
        ficheiro.add(item("Exportar mapa (PNG)...", null, e -> exportarMapa()));
        ficheiro.add(item("Exportar console (TXT)...", null, e -> exportarConsole()));
        ficheiro.addSeparator();
        ficheiro.add(item("Sair", null, e -> dispose()));
        barra.add(ficheiro);

        JMenu editar = new JMenu("Editar");
        miDesfazer = item("Desfazer", KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), e -> desfazer());
        miRefazer = item("Refazer", KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), e -> refazer());
        editar.add(miDesfazer);
        editar.add(miRefazer);
        barra.add(editar);

        JMenu rede = new JMenu("Rede");
        rede.add(item("Adicionar cidade...", null, e -> novaCidade(null, null)));
        rede.add(item("Remover cidade...", null, e -> removerCidade()));
        rede.add(item("Adicionar / atualizar / remover conexão...", null,
                e -> dialogoConexao(cbOrigem.getSelectedIndex(), cbDestino.getSelectedIndex())));
        rede.addSeparator();
        rede.add(item("Unidade do custo...", null, e -> mudarUnidade()));
        barra.add(rede);

        JMenu anim = new JMenu("Animação");
        anim.add(item("Reproduzir / Pausar", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), e -> painel.alternar()));
        anim.add(item("Passo anterior", KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), e -> painel.anterior()));
        anim.add(item("Passo seguinte", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), e -> painel.proximo()));
        anim.addSeparator();
        anim.add(item("Limpar destaques", null, e -> painel.limparDestaques()));
        barra.add(anim);

        JMenu ajuda = new JMenu("Ajuda");
        ajuda.add(item("Como usar...", null, e -> mostrarAjuda()));
        barra.add(ajuda);
        return barra;
    }

    private JMenuItem item(String texto, KeyStroke atalho, ActionListener acao) 
    {
        JMenuItem mi = new JMenuItem(texto);
        if (atalho != null) mi.setAccelerator(atalho);
        mi.addActionListener(acao);
        return mi;
    }

    private javax.swing.JComponent criarPainelControlo() 
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.setBackground(new Color(245, 247, 250));

        p.add(secao("CIDADES"));
        p.add(rotulo("Cidade de Partida (origem)"));
        p.add(combo(cbOrigem));
        p.add(Box.createVerticalStrut(6));
        p.add(rotulo("Cidade de Chegada (destino)"));
        p.add(combo(cbDestino));
        p.add(Box.createVerticalStrut(10));

        Color info = new Color(60, 80, 120);
        Color gestao = new Color(90, 100, 110);
        p.add(secao("REPRESENTAÇÕES"));
        p.add(botao("Exibir Matriz de Adjacência", info, e -> exibirMatriz()));
        p.add(botao("Exibir Listas de Adjacência", info, e -> exibirLista()));
        p.add(Box.createVerticalStrut(8));

        p.add(secao("ALGORITMOS"));
        p.add(botao("Executar DFS", COR_DFS, e -> executar(dfs, COR_DFS)));
        p.add(botao("Executar BFS", COR_BFS, e -> executar(bfs, COR_BFS)));
        p.add(botao("Gerar MST (Prim)", COR_PRIM, e -> executar(prim, COR_PRIM)));
        p.add(botao("Gerar MST (Kruskal)", COR_KRUSKAL, e -> executar(kruskal, COR_KRUSKAL)));
        p.add(botao("Calcular Menor Rota (Dijkstra)", COR_DIJKSTRA, e -> executar(dijkstra, COR_DIJKSTRA)));
        p.add(botao("Rota de Abastecimento (heurística)", COR_ROTA_H, e -> executar(rotaHeuristica, COR_ROTA_H)));
        p.add(botao("Rota de Abastecimento (ótima)", COR_ROTA_O, e -> executar(rotaOtima, COR_ROTA_O)));
        p.add(botao("Comparar Algoritmos", new Color(40, 60, 90), e -> compararAlgoritmos()));
        p.add(Box.createVerticalStrut(8));

        p.add(secao("GESTÃO DA REDE"));
        p.add(botao("Adicionar/Remover Conexão", gestao,
                e -> dialogoConexao(cbOrigem.getSelectedIndex(), cbDestino.getSelectedIndex())));
        p.add(botao("Adicionar Cidade", gestao, e -> novaCidade(null, null)));
        p.add(botao("Remover Cidade", gestao, e -> removerCidade()));
        btnDesfazer = botao("Desfazer", gestao, e -> desfazer());
        btnRefazer = botao("Refazer", gestao, e -> refazer());
        p.add(par(btnDesfazer, btnRefazer));
        p.add(Box.createVerticalStrut(8));

        p.add(par(botao("Limpar Mapa", gestao, e -> painel.limparDestaques()),
                botao("Limpar Console", gestao, e -> console.setText(""))));

        JScrollPane sp = new JScrollPane(p, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setPreferredSize(new Dimension(300, 100));
        return sp;
    }

    private javax.swing.JComponent criarAreaCentral() 
    {
        JPanel topo = new JPanel(new BorderLayout());
        topo.add(criarBarraAnimacao(), BorderLayout.NORTH);
        topo.add(painel, BorderLayout.CENTER);

        // Aba 1: console
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        console.setBackground(new Color(30, 30, 36));
        console.setForeground(new Color(200, 235, 200));
        console.setCaretColor(Color.WHITE);
        JScrollPane spConsole = new JScrollPane(console);
        spConsole.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(120, 130, 150)),
                "Console de Saída / Logs (passo a passo, custos e tempos)", TitledBorder.LEFT, TitledBorder.TOP));

        // Aba 2: matriz (tabela)
        tabelaMatriz.setRowHeight(26);
        tabelaMatriz.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabelaMatriz.getTableHeader().setReorderingAllowed(false);
        tabelaMatriz.setDefaultRenderer(Object.class, new MatrizRenderer());
        tabelaMatriz.setGridColor(new Color(215, 220, 230));

        // Aba 3: listas
        areaListas.setEditable(false);
        areaListas.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        areaListas.setMargin(new java.awt.Insets(6, 8, 6, 8));

        // Aba 4: comparação
        JTable tabelaComp = new JTable(modeloComparacao);
        tabelaComp.setRowHeight(24);
        tabelaComp.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabelaComp.getColumnModel().getColumn(0).setPreferredWidth(230);
        tabelaComp.getColumnModel().getColumn(1).setPreferredWidth(90);
        tabelaComp.getColumnModel().getColumn(2).setPreferredWidth(230);
        tabelaComp.getColumnModel().getColumn(3).setPreferredWidth(110);
        tabelaComp.getColumnModel().getColumn(4).setPreferredWidth(260);
        areaVerificacoes.setEditable(false);
        areaVerificacoes.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaVerificacoes.setRows(5);
        areaVerificacoes.setBorder(BorderFactory.createTitledBorder("Verificações cruzadas de correção"));
        lblComparacao.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel comp = new JPanel(new BorderLayout());
        comp.add(lblComparacao, BorderLayout.NORTH);
        comp.add(new JScrollPane(tabelaComp), BorderLayout.CENTER);
        comp.add(new JScrollPane(areaVerificacoes), BorderLayout.SOUTH);

        abas = new JTabbedPane();
        abas.addTab("Console", spConsole);
        abas.addTab("Matriz de Adjacência", new JScrollPane(tabelaMatriz));
        abas.addTab("Listas de Adjacência", new JScrollPane(areaListas));
        abas.addTab("Comparação de Algoritmos", comp);

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topo, abas);
        split.setResizeWeight(0.7);
        split.setOneTouchExpandable(true);
        split.setContinuousLayout(true);
        return split;
    }

    private JPanel criarBarraAnimacao() 
    {
        JPanel b = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 4));
        b.setBackground(new Color(228, 233, 242));
        Color c = new Color(70, 90, 130);
        btnInicio = botaoPequeno("|< Início", c, e -> painel.inicio());
        btnAnterior = botaoPequeno("< Anterior", c, e -> painel.anterior());
        btnPlay = botaoPequeno("Reproduzir", new Color(0, 150, 80), e -> painel.alternar());
        btnProximo = botaoPequeno("Próximo >", c, e -> painel.proximo());
        btnFim = botaoPequeno("Fim >|", c, e -> painel.fim());
        b.add(btnInicio);
        b.add(btnAnterior);
        b.add(btnPlay);
        b.add(btnProximo);
        b.add(btnFim);
        b.add(Box.createHorizontalStrut(10));
        b.add(new JLabel("Velocidade:"));
        sliderVelocidade = new JSlider(1, 10, 4);          // 1 = lento (2 s por passo) ... 10 = rápido (0,2 s)
        sliderVelocidade.setPreferredSize(new Dimension(130, 24));
        sliderVelocidade.setOpaque(false);
        sliderVelocidade.setToolTipText("Tempo entre passos: de 2 s (esquerda) a 0,2 s (direita)");
        sliderVelocidade.addChangeListener(e -> painel.setAtraso(2000 / sliderVelocidade.getValue()));
        b.add(sliderVelocidade);
        b.add(Box.createHorizontalStrut(10));
        lblPasso.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.add(lblPasso);
        return b;
    }

    private JPanel criarBarraEstado() 
    {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(190, 195, 205)),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        lblEstado.setFont(new Font("SansSerif", Font.PLAIN, 12));
        p.add(lblEstado, BorderLayout.WEST);
        return p;
    }

    private JLabel secao(String t) 
    {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(new Color(120, 130, 150));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        return l;
    }

    private JLabel rotulo(String t) 
    {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        return l;
    }

    private JComboBox<String> combo(JComboBox<String> cb) 
    {
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return cb;
    }

    private BotaoModerno botao(String texto, Color cor, ActionListener acao) 
    {
        BotaoModerno b = new BotaoModerno(texto, cor);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setPreferredSize(new Dimension(250, 33));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 33));
        b.addActionListener(acao);
        return b;
    }

    private BotaoModerno botaoPequeno(String texto, Color cor, ActionListener acao) 
    {
        BotaoModerno b = new BotaoModerno(texto, cor);
        b.setPreferredSize(new Dimension(texto.length() > 9 ? 112 : 100, 28));
        b.addActionListener(acao);
        return b;
    }

    /** Dois botões lado a lado. */
    private JPanel par(JButton a, JButton b) 
    {
        JPanel p = new JPanel(new GridLayout(1, 2, 6, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 33));
        // Cada botão do par ocupa metade da largura da coluna (senão o par ficaria com o dobro e cortaria os outros botões)
        a.setPreferredSize(new Dimension(120, 33));
        b.setPreferredSize(new Dimension(120, 33));
        p.add(a);
        p.add(b);
        return p;
    }

    /** Botão com estilo moderno: cantos redondos, cor de fundo, efeito ao passar o rato. */
    static class BotaoModerno extends JButton 
    {
        private final Color base;
        private boolean sobre = false;

        BotaoModerno(String texto, Color base) 
        {
            super(texto);
            this.base = base;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { sobre = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { sobre = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) 
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled() ? new Color(175, 180, 190)
                    : getModel().isPressed() ? base.darker().darker()
                    : sobre ? base.brighter() : base;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);                  // desenha o texto por cima
        }
    }

    /** Pinta a Matriz de Adjacências: ligações a azul, cabeçalhos a cinzento, zeros esbatidos. */
    private static class MatrizRenderer extends DefaultTableCellRenderer 
    {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setHorizontalAlignment(col == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            boolean ligado = v instanceof Integer && ((Integer) v) > 0;
            if (col == 0) {
                setBackground(new Color(225, 232, 245));
                setForeground(Color.BLACK);
                setFont(getFont().deriveFont(Font.BOLD));
            } else if (ligado) {
                setBackground(new Color(180, 220, 255));
                setForeground(Color.BLACK);
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                setBackground(row == col - 1 ? new Color(238, 238, 242) : Color.WHITE);
                setForeground(new Color(165, 165, 170));
                setFont(getFont().deriveFont(Font.PLAIN));
            }
            return this;
        }
    }

    // =====================================================================
    // Utilitários
    // =====================================================================
    private void log(String s) 
    {
        console.append(s + "\n");
        console.setCaretPosition(console.getDocument().getLength());
    }

    private void aviso(String msg) 
    {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void erro(String msg) 
    {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private String[] nomes() 
    {
        String[] a = new String[grafo.getN()];
        for (int i = 0; i < a.length; i++) a[i] = grafo.getNome(i);
        return a;
    }

    private String nomeSel(JComboBox<String> cb) 
    {
        Object o = cb.getSelectedItem();
        return o == null ? null : o.toString();
    }

    private static String html(String s) 
    {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private boolean redeValida() 
    {
        if (grafo.getN() == 0 || cbOrigem.getSelectedIndex() < 0 || cbDestino.getSelectedIndex() < 0) 
        {
            aviso("A rede está vazia. Adicione cidades primeiro (menu Rede ou duplo clique no mapa).");
            return false;
        }
        return true;
    }

    private String juntar(List<Integer> ids) 
    {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < ids.size(); k++) sb.append(grafo.getNome(ids.get(k))).append(k < ids.size() - 1 ? " -> " : "");
        return sb.toString();
    }

    /** Dá espaço ao painel inferior se ele estiver demasiado pequeno para ler a tabela/lista. */
    private void garantirAlturaInferior(int h) 
    {
        int total = split.getHeight();
        int atual = total - split.getDividerLocation() - split.getDividerSize();
        if (atual < h && total > h + 200) split.setDividerLocation(total - h - split.getDividerSize());
    }

    // =====================================================================
    // Atualização da interface após alterações à rede
    // =====================================================================
    /** Atualiza tudo o que depende da rede. Tenta manter a origem/destino pelo NOME (os índices mudam). */
    private void refrescar(String nomeOrigem, String nomeDestino) 
    {
        recarregarCombos(nomeOrigem, nomeDestino);
        painel.limparDestaques();
        atualizarMatriz();
        atualizarListas();
        atualizarEstado();
        atualizarMenuEdicao();
        modeloComparacao.setRowCount(0);
        areaVerificacoes.setText("");
        lblComparacao.setText("A rede mudou. Carregue em \"Comparar Algoritmos\" para voltar a comparar.");
        painel.repaint();
    }

    private void recarregarCombos(String nomeO, String nomeD) 
    {
        atualizandoCombos = true;
        String[] ns = nomes();
        cbOrigem.setModel(new DefaultComboBoxModel<String>(ns));
        cbDestino.setModel(new DefaultComboBoxModel<String>(ns));
        if (ns.length > 0) {
            int io = nomeO == null ? -1 : grafo.indiceDe(nomeO);
            int id = nomeD == null ? -1 : grafo.indiceDe(nomeD);
            cbOrigem.setSelectedIndex(io >= 0 ? io : 0);
            cbDestino.setSelectedIndex(id >= 0 ? id : ns.length - 1);
        }
        atualizandoCombos = false;
        painel.setOrigemDestino(cbOrigem.getSelectedIndex(), cbDestino.getSelectedIndex());
    }

    private void atualizarMatriz() 
    {
        int n = grafo.getN();
        int[][] m = grafo.getMatriz();
        Object[] cols = new Object[n + 1];
        Object[][] dados = new Object[n][n + 1];
        cols[0] = "";
        for (int i = 0; i < n; i++) 
        {
            cols[i + 1] = "<html><center>" + i + "<br>" + html(grafo.getNome(i)) + "</center></html>";
            dados[i][0] = i + "  " + grafo.getNome(i);
            for (int j = 0; j < n; j++) dados[i][j + 1] = m[i][j];
        }
        modeloMatriz.setDataVector(dados, cols);
        for (int c = 0; c <= n; c++) tabelaMatriz.getColumnModel().getColumn(c).setPreferredWidth(c == 0 ? 130 : 82);
        tabelaMatriz.getTableHeader().setPreferredSize(new Dimension(tabelaMatriz.getColumnModel().getTotalColumnWidth(), 40));
    }

    private void atualizarListas() 
    {
        areaListas.setText(grafo.getN() == 0 ? "(rede vazia)" : grafo.listaComoTexto());
        areaListas.setCaretPosition(0);
    }

    private void atualizarEstado() 
    {
        int c = grafo.numeroComponentes();
        String s = "Cidades: " + grafo.getN() + "   |   Ligações: " + grafo.getArestas().size()
                + "   |   Componentes: " + c + (c > 1 ? " (REDE DESCONEXA)" : "")
                + "   |   Custo em: " + grafo.getUnidade();
        if (grafo.getN() > 0 && cbOrigem.getSelectedIndex() >= 0 && cbDestino.getSelectedIndex() >= 0) 
        {
            s += "   |   Origem: " + nomeSel(cbOrigem) + "   Destino: " + nomeSel(cbDestino);
        }
        lblEstado.setText(s);
        lblEstado.setForeground(c > 1 ? new Color(190, 30, 30) : Color.BLACK);
    }

    private void atualizarMenuEdicao() 
    {
        miDesfazer.setEnabled(grafo.podeDesfazer());
        miRefazer.setEnabled(grafo.podeRefazer());
        btnDesfazer.setEnabled(grafo.podeDesfazer());
        btnRefazer.setEnabled(grafo.podeRefazer());
    }

    // =====================================================================
    // PainelGrafo.Ouvinte - eventos do rato no mapa
    // =====================================================================
    @Override public void origemEscolhida(int idx) 
    {
        if (idx >= 0 && idx < cbOrigem.getItemCount()) cbOrigem.setSelectedIndex(idx);
    }

    @Override public void destinoEscolhido(int idx) 
    {
        if (idx >= 0 && idx < cbDestino.getItemCount()) cbDestino.setSelectedIndex(idx);
    }

    @Override public void editarAresta(int u, int v) 
    {
        dialogoConexao(u, v);
    }

    @Override public void adicionarCidadeEm(double rx, double ry) 
    {
        novaCidade(rx, ry);
    }

    @Override public void redeMovida() 
    {
        atualizarMenuEdicao();
    }

    @Override public void reproducaoAlterada() 
    {
        int total = painel.getTotalPassos(), pos = painel.getPosicao();
        boolean tem = total > 0;
        btnInicio.setEnabled(tem && pos > 1);
        btnAnterior.setEnabled(tem && pos > 1);
        btnProximo.setEnabled(tem && pos < total);
        btnFim.setEnabled(tem && pos < total);
        btnPlay.setEnabled(tem);
        btnPlay.setText(painel.isReproduzindo() ? "Pausar" : (tem && pos >= total ? "Repetir" : "Reproduzir"));
        lblPasso.setText(tem ? "Passo " + pos + " / " + total : "Sem animação");
    }

    // =====================================================================
    // a) / b) Matriz e Listas de Adjacências
    // =====================================================================
    private void exibirMatriz() 
    {
        if (grafo.getN() == 0) { aviso("A rede está vazia."); return; }
        log("\n" + grafo.matrizComoTexto());
        abas.setSelectedIndex(ABA_MATRIZ);
        garantirAlturaInferior(360);
    }

    private void exibirLista() 
    {
        if (grafo.getN() == 0) { aviso("A rede está vazia."); return; }
        log("\n" + grafo.listaComoTexto());
        abas.setSelectedIndex(ABA_LISTAS);
        garantirAlturaInferior(320);
    }

    // =====================================================================
    // c) .. f) Algoritmos
    // =====================================================================
    private void executar(Algoritmo alg, Color cor) 
    {
        if (!redeValida()) return;
        int o = cbOrigem.getSelectedIndex(), d = cbDestino.getSelectedIndex();
        if (alg instanceof CaminhoDijkstra && o == d) 
        {
            aviso("Origem e destino são a mesma cidade. Escolha cidades diferentes para o Dijkstra.");
            return;
        }
        Resultado r = alg.executar(grafo, o, d);

        String titulo;
        if (alg instanceof MstKruskal) titulo = alg.nome();
        else if (alg.usaDestino()) titulo = alg.nome() + ": " + grafo.getNome(o) + (o == d ? " (circuito fechado)" : " -> " + grafo.getNome(d));
        else titulo = alg.nome() + " a partir de " + grafo.getNome(o);

        imprimir(titulo, alg, r);
        painel.carregar(r, cor, titulo);
        abas.setSelectedIndex(ABA_CONSOLE);
        if (!r.encontrado) aviso(r.mensagemErro.isEmpty() ? "Não foi possível calcular o resultado." : r.mensagemErro);
    }

    /** Escreve o passo a passo, o percurso, o custo e o tempo no console. */
    private void imprimir(String titulo, Algoritmo alg, Resultado r) 
    {
        log("\n======== " + titulo + " ========");
        log("Algoritmo: " + alg.descricao());
        for (String l : r.log) log(l);
        if (!r.caminho.isEmpty()) 
        {
            String rotulo = (alg instanceof BuscaDFS || alg instanceof BuscaBFS) ? "Ordem de visita"
                    : (alg instanceof MstPrim) ? "Ordem de inclusão na árvore" : "Percurso";
            log(rotulo + ": " + juntar(r.caminho));
        }
        log("Custo total: " + (r.encontrado ? r.custoTotal + " " + grafo.getUnidade() : "não calculado"));
        if (!r.detalhe.isEmpty()) log("Resumo: " + r.detalhe);
        log(String.format("Tempo de execução: %.4f ms (inclui a geração deste log)", r.tempoNs / 1e6));
    }

    /** Executa tudo e mostra a tabela comparativa (aba "Comparação"). */
    private void compararAlgoritmos() 
    {
        if (!redeValida()) return;
        int o = cbOrigem.getSelectedIndex(), d = cbDestino.getSelectedIndex();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ComparadorAlgoritmos.Relatorio rel;
        try {
            rel = new ComparadorAlgoritmos().comparar(grafo, o, d);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
        String un = grafo.getUnidade();
        modeloComparacao.setRowCount(0);
        for (ComparadorAlgoritmos.Linha l : rel.linhas) {
            modeloComparacao.addRow(new Object[]{
                l.algoritmo,
                l.custo == null ? "-" : l.custo + " " + un,
                l.detalhe,
                String.format("%.1f", l.tempoMicros),
                l.observacao});
        }
        StringBuilder sb = new StringBuilder();
        for (String v : rel.verificacoes) sb.append(v).append("\n");
        areaVerificacoes.setText(sb.toString());
        areaVerificacoes.setCaretPosition(0);
        lblComparacao.setText("Origem: " + grafo.getNome(o) + "   Destino: " + grafo.getNome(d)
                + "   |   tempos médios de várias execuções (incluem a geração do log)   |   "
                + (rel.tudoOk ? "todas as verificações OK" : "ATENÇÃO: há verificações com ERRO"));

        log("\n======== Comparação de algoritmos (" + grafo.getNome(o) + " -> " + grafo.getNome(d) + ") ========");
        for (String v : rel.verificacoes) log(v);
        abas.setSelectedIndex(ABA_COMPARACAO);
        garantirAlturaInferior(330);
    }

    // =====================================================================
    // h) Gestão dinâmica da rede
    // =====================================================================
    /** Diálogo para adicionar, atualizar ou remover a conexão entre duas cidades. */
    private void dialogoConexao(int preA, int preB) {
        if (grafo.getN() < 2) { aviso("São necessárias pelo menos 2 cidades."); return; }
        final JComboBox<String> a = new JComboBox<String>(nomes());
        final JComboBox<String> b = new JComboBox<String>(nomes());
        a.setSelectedIndex(Math.max(0, Math.min(preA, grafo.getN() - 1)));
        b.setSelectedIndex(Math.max(0, Math.min(preB, grafo.getN() - 1)));
        final JTextField peso = new JTextField("10", 8);
        final JLabel dica = new JLabel(" ");
        ActionListener atualiza = e -> {
            int i = a.getSelectedIndex(), j = b.getSelectedIndex();
            if (i == j) {
                dica.setText("Escolha duas cidades diferentes.");
            } else if (grafo.temAresta(i, j)) {
                peso.setText(String.valueOf(grafo.getPeso(i, j)));
                dica.setText("Já existe: pode atualizar o peso ou remover.");
            } else {
                dica.setText("Ainda não existe: será criada.");
            }
        };
        a.addActionListener(atualiza);
        b.addActionListener(atualiza);
        atualiza.actionPerformed(null);

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Cidade A:"));
        p.add(a);
        p.add(new JLabel("Cidade B:"));
        p.add(b);
        p.add(new JLabel("Distância (" + grafo.getUnidade() + "):"));
        p.add(peso);
        p.add(new JLabel(""));
        p.add(dica);

        Object[] op = {"Adicionar/Atualizar", "Remover", "Cancelar"};
        int r = JOptionPane.showOptionDialog(this, p, "Adicionar/Remover Conexão",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, op, op[0]);
        int i = a.getSelectedIndex(), j = b.getSelectedIndex();
        String nO = nomeSel(cbOrigem), nD = nomeSel(cbDestino);

        if (r == 0) {
            try {
                int w = Integer.parseInt(peso.getText().trim());
                boolean atualizou = grafo.adicionarAresta(i, j, w);
                log("\n" + (atualizou ? "Conexão atualizada: " : "Conexão adicionada: ")
                        + grafo.getNome(i) + " - " + grafo.getNome(j) + " (" + w + " " + grafo.getUnidade() + ")");
                refrescar(nO, nD);
            } catch (NumberFormatException ex) {
                aviso("Distância inválida. Insira um número inteiro positivo.");
            } catch (IllegalArgumentException ex) {
                aviso(ex.getMessage());
            }
        } else if (r == 1) {
            if (i == j || !grafo.temAresta(i, j)) { aviso("Não existe conexão entre essas cidades."); return; }
            boolean eraConexa = grafo.ehConexo();
            String ligacao = grafo.getNome(i) + " - " + grafo.getNome(j);
            grafo.removerAresta(i, j);
            log("\nConexão removida: " + ligacao);
            refrescar(nO, nD);
            avisarSeDesconexa(eraConexa);
        }
    }

    /** Pede o nome e acrescenta a cidade (em (rx, ry) se vier do clique no mapa; senão, perto do centro). */
    private void novaCidade(Double rx, Double ry) {
        String nome = JOptionPane.showInputDialog(this, "Nome da nova cidade:", "Adicionar Cidade", JOptionPane.QUESTION_MESSAGE);
        if (nome == null) return;
        double x = rx != null ? rx : 0.3 + 0.4 * aleatorio.nextDouble();
        double y = ry != null ? ry : 0.3 + 0.4 * aleatorio.nextDouble();
        try {
            String nO = nomeSel(cbOrigem), nD = nomeSel(cbDestino);
            int idx = grafo.adicionarVertice(nome, x, y);
            refrescar(nO, nD);
            log("\nCidade adicionada: " + grafo.getNome(idx) + " (índice " + idx + "). Arraste-a no mapa e ligue-a com 'Adicionar/Remover Conexão'.");
        } catch (IllegalArgumentException ex) {
            aviso(ex.getMessage());
        }
    }

    private void removerCidade() {
        if (grafo.getN() == 0) { aviso("A rede está vazia."); return; }
        String[] ns = nomes();
        Object esc = JOptionPane.showInputDialog(this, "Cidade a remover (as suas conexões também serão removidas):",
                "Remover Cidade", JOptionPane.QUESTION_MESSAGE, null, ns, ns[Math.max(0, cbOrigem.getSelectedIndex())]);
        if (esc == null) return;
        int idx = grafo.indiceDe(esc.toString());
        boolean eraConexa = grafo.ehConexo();
        int ligacoes = grafo.getGrau(idx);
        String nO = nomeSel(cbOrigem), nD = nomeSel(cbDestino);
        grafo.removerVertice(idx);
        refrescar(nO, nD);
        log("\nCidade removida: " + esc + " (e " + ligacoes + " conexões). Os índices dos vértices foram reajustados.");
        avisarSeDesconexa(eraConexa);
    }

    private void avisarSeDesconexa(boolean eraConexa) {
        if (!eraConexa || grafo.ehConexo()) return;
        StringBuilder sb = new StringBuilder("A rede ficou DESCONEXA (" + grafo.numeroComponentes() + " componentes):\n");
        int c = 1;
        for (List<Integer> comp : grafo.listaComponentes()) {
            sb.append("  ").append(c++).append(") ");
            for (int k = 0; k < comp.size(); k++) sb.append(grafo.getNome(comp.get(k))).append(k < comp.size() - 1 ? ", " : "");
            sb.append("\n");
        }
        log("\n!! " + sb);
        JOptionPane.showMessageDialog(this,
                sb + "\nDijkstra e as rotas não encontram caminho entre componentes diferentes,\n"
                        + "e Prim/Kruskal passam a produzir uma floresta.",
                "Rede desconexa", JOptionPane.WARNING_MESSAGE);
    }

    private void mudarUnidade() {
        Object r = JOptionPane.showInputDialog(this, "Unidade do custo / distância (ex.: km, h, EUR):",
                "Unidade do custo", JOptionPane.QUESTION_MESSAGE, null, null, grafo.getUnidade());
        if (r == null) return;
        try {
            String nO = nomeSel(cbOrigem), nD = nomeSel(cbDestino);
            grafo.setUnidade(r.toString());
            refrescar(nO, nD);
            log("\nUnidade do custo alterada para: " + grafo.getUnidade());
        } catch (IllegalArgumentException ex) {
            aviso(ex.getMessage());
        }
    }

    private void desfazer() {
        String nO = nomeSel(cbOrigem), nD = nomeSel(cbDestino);
        if (grafo.desfazer()) {
            refrescar(nO, nD);
            log("\nAlteração desfeita.");
        }
    }

    private void refazer() {
        String nO = nomeSel(cbOrigem), nD = nomeSel(cbDestino);
        if (grafo.refazer()) {
            refrescar(nO, nD);
            log("\nAlteração refeita.");
        }
    }

    // =====================================================================
    // Ficheiros
    // =====================================================================
    private File escolherFicheiro(boolean guardar, String descricao, String sugestao, String... extensoes) {
        JFileChooser fc = new JFileChooser(ultimaPasta);
        fc.setFileFilter(new FileNameExtensionFilter(descricao, extensoes));
        if (guardar) fc.setSelectedFile(new File(sugestao));
        int r = guardar ? fc.showSaveDialog(this) : fc.showOpenDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        ultimaPasta = f.getParentFile();
        if (guardar && !f.getName().contains(".")) f = new File(f.getParentFile(), f.getName() + "." + extensoes[0]);
        if (guardar && f.exists()) {
            int c = JOptionPane.showConfirmDialog(this, "O ficheiro \"" + f.getName() + "\" já existe. Substituir?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return null;
        }
        return f;
    }

    private void abrirRede() {
        File f = escolherFicheiro(false, "Rede ferroviária (*.rede, *.txt, *.csv)", null, "rede", "txt", "csv");
        if (f == null) return;
        try {
            grafo.carregar(f);
            refrescar(null, null);
            log("\nRede carregada de \"" + f.getName() + "\": " + grafo.getN() + " cidades, " + grafo.getArestas().size() + " conexões.");
        } catch (IOException ex) {
            erro("Não foi possível carregar a rede:\n" + ex.getMessage());
        }
    }

    private void guardarRede() {
        File f = escolherFicheiro(true, "Rede ferroviária (*.rede)", "rede_ferroviaria.rede", "rede");
        if (f == null) return;
        try {
            grafo.guardar(f);
            log("\nRede guardada em \"" + f.getAbsolutePath() + "\".");
        } catch (IOException ex) {
            erro("Não foi possível guardar a rede:\n" + ex.getMessage());
        }
    }

    private void exportarMapa() {
        File f = escolherFicheiro(true, "Imagem PNG (*.png)", "mapa_rede.png", "png");
        if (f == null) return;
        try {
            painel.exportarPNG(f);
            log("\nMapa exportado para \"" + f.getAbsolutePath() + "\".");
        } catch (IOException ex) {
            erro("Não foi possível exportar o mapa:\n" + ex.getMessage());
        }
    }

    private void exportarConsole() {
        File f = escolherFicheiro(true, "Texto (*.txt)", "log_algoritmos.txt", "txt");
        if (f == null) return;
        try {
            Files.write(f.toPath(), console.getText().getBytes("UTF-8"));
            log("\nConsole exportado para \"" + f.getAbsolutePath() + "\".");
        } catch (IOException ex) {
            erro("Não foi possível exportar o console:\n" + ex.getMessage());
        }
    }

    private void novaRedeVazia() {
        int c = JOptionPane.showConfirmDialog(this, "Apagar todas as cidades e conexões? (pode desfazer em Editar > Desfazer)",
                "Nova rede vazia", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        grafo.limpar();
        refrescar(null, null);
        log("\nRede apagada. Duplo clique no mapa para criar cidades.");
    }

    private void reporRede() {
        int c = JOptionPane.showConfirmDialog(this, "Repor a rede ferroviária ucraniana original? (pode desfazer)",
                "Repor rede", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        grafo.reporRedePadrao();
        refrescar(null, null);
        log("\nRede predefinida reposta (12 cidades, 17 conexões).");
    }

    private void mostrarAjuda() {
        JTextArea t = new JTextArea(
                "MAPA\n"
              + "  Clique numa cidade ............ define a ORIGEM (anel verde)\n"
              + "  Botão direito numa cidade ..... define o DESTINO (anel vermelho)\n"
              + "  Arrastar uma cidade ........... move-a (pode desfazer)\n"
              + "  Duplo clique numa aresta ...... editar o peso ou remover a conexão\n"
              + "  Duplo clique em zona vazia .... criar uma cidade nesse ponto\n\n"
              + "ANIMAÇÃO (barra por cima do mapa)\n"
              + "  Reproduzir/Pausar (F5), Anterior (F4), Próximo (F6), Início, Fim e Velocidade.\n"
              + "  Pause num passo para o explicar: a legenda amarela descreve o que o algoritmo fez.\n"
              + "  Os números junto das cidades mostram a ordem de visita. Tracejado = aresta rejeitada (ciclo).\n\n"
              + "ALGORITMOS\n"
              + "  Rota de Abastecimento (heurística) é rápida mas aproximada;\n"
              + "  Rota de Abastecimento (ótima) é exata (Held-Karp) até " + RotaOtima.LIMITE + " cidades intermédias.\n"
              + "  'Comparar Algoritmos' mostra custos, tempos e verificações de correção.\n\n"
              + "FICHEIROS\n"
              + "  Guardar/abrir a rede (.rede), exportar o mapa (PNG) e o console (TXT).\n"
              + "  Ctrl+Z / Ctrl+Y desfazem e refazem alterações à rede.");
        t.setEditable(false);
        t.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        t.setBackground(new Color(250, 250, 252));
        t.setMargin(new java.awt.Insets(8, 10, 8, 10));
        JOptionPane.showMessageDialog(this, t, "Como usar", JOptionPane.PLAIN_MESSAGE);
    }

    // =====================================================================
    // main
    // =====================================================================
    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignorada) {
                // usa o aspeto por omissão
            }
            new MainFrame().setVisible(true);
        });
    }
}
