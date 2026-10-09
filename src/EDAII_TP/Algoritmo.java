package EDAII_TP;

/**
 * Contrato comum a todos os algoritmos (padrão Strategy).
 * Cada algoritmo vive na sua própria classe e recebe o grafo como parâmetro, por isso
 * a janela principal trata todos da mesma forma: executar(grafo, origem, destino).
 */
public interface Algoritmo {

    /** Nome apresentado ao utilizador. */
    String nome();

    /** Descrição curta com a complexidade (útil para explicar na defesa). */
    String descricao();

    /** O algoritmo precisa de uma cidade de chegada? (Dijkstra e rotas: sim; buscas e MST: não.) */
    boolean usaDestino();

    /**
     * Executa o algoritmo.
     *
     * @param g       grafo (não é modificado)
     * @param origem  índice da cidade de partida
     * @param destino índice da cidade de chegada (ignorado se usaDestino() == false)
     */
    Resultado executar(GrafoFerroviarioUcraniano g, int origem, int destino);
}
