package EDAII_TP;

/**
 * Elemento de uma Lista de Adjacências: "para onde posso ir, e a que custo".
 * A lista de um vértice u é uma lista de Vizinho (destino, peso).
 */
public final class Vizinho {

    public final int destino;
    public final int peso;

    public Vizinho(int destino, int peso) {
        this.destino = destino;
        this.peso = peso;
    }

    @Override
    public String toString() {
        return destino + ":" + peso;
    }
}
