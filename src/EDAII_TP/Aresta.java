package EDAII_TP;

/**
 * Aresta (ligação ferroviária) NÃO dirigida e ponderada.
 *
 * Guardamos sempre u < v, por isso a aresta A-B e a aresta B-A são a mesma.
 * A classe é imutável: para mudar o peso cria-se uma nova Aresta.
 */
public final class Aresta {

    public final int u;      // extremo com o menor índice
    public final int v;      // extremo com o maior índice
    public final int peso;   // distância / custo da ligação (inteiro positivo)

    public Aresta(int a, int b, int peso) {
        this.u = Math.min(a, b);
        this.v = Math.max(a, b);
        this.peso = peso;
    }

    /** Dado um dos extremos, devolve o outro. */
    public int outro(int x) {
        return x == u ? v : u;
    }

    /** Esta aresta liga os vértices a e b (em qualquer ordem)? */
    public boolean liga(int a, int b) {
        return (u == a && v == b) || (u == b && v == a);
    }

    /** Chave textual única da aresta (útil em conjuntos/mapas): "u-v" com u < v. */
    public String chave() {
        return u + "-" + v;
    }

    /** Chave textual para um par qualquer de vértices. */
    public static String chave(int a, int b) {
        return Math.min(a, b) + "-" + Math.max(a, b);
    }

    @Override
    public String toString() {
        return "(" + u + " - " + v + ", peso " + peso + ")";
    }
}
