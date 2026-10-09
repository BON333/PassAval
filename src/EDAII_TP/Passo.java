package EDAII_TP;

/**
 * Um passo da animação de um algoritmo. Há três tipos:
 *
 *  - NÓ        : só o vértice u é "tocado" (ex.: ponto de partida).
 *  - MOVER     : o algoritmo anda de u para v pela aresta u-v (v passa a fazer parte do resultado).
 *  - REJEITADA : o algoritmo analisou a aresta u-v e recusou-a (ex.: Kruskal evitou um ciclo).
 *
 * Cada passo traz um texto explicativo, que o mapa mostra enquanto se avança passo a passo.
 */
public final class Passo {

    public final int u;
    public final int v;            // -1 quando é um passo de NÓ
    public final boolean rejeitado;
    public final String texto;

    private Passo(int u, int v, boolean rejeitado, String texto) {
        this.u = u;
        this.v = v;
        this.rejeitado = rejeitado;
        this.texto = texto;
    }

    public static Passo no(int u, String texto) {
        return new Passo(u, -1, false, texto);
    }

    public static Passo mover(int u, int v, String texto) {
        return new Passo(u, v, false, texto);
    }

    public static Passo rejeitada(int u, int v, String texto) {
        return new Passo(u, v, true, texto);
    }

    public boolean isNo() {
        return v < 0;
    }
}
