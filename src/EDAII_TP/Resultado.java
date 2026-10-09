package EDAII_TP;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de executar um algoritmo sobre o grafo.
 * Os algoritmos NÃO desenham nada: só preenchem este objeto. O PainelGrafo anima os
 * "passos" e o MainFrame imprime o "log" no console (separação lógica / interface).
 */
public class Resultado {

    public final String nome;                                 // nome do algoritmo
    public final List<Passo> passos = new ArrayList<Passo>();   // animação, passo a passo
    public final List<String> log = new ArrayList<String>();    // explicação textual no console
    public final List<Integer> caminho = new ArrayList<Integer>(); // percurso / ordem de visita
    public int custoTotal = 0;
    public boolean encontrado = true;      // false = não foi possível cumprir o pedido
    public String mensagemErro = "";       // explicação quando encontrado == false
    public boolean numerar = true;         // o mapa mostra a ordem de visita (1, 2, 3...)?
    public String detalhe = "";            // resumo curto (ex.: "11 arestas") para a tabela comparativa
    public long tempoNs = 0;

    public Resultado(String nome) {
        this.nome = nome;
    }
}
