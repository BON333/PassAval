package EDAII_TP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ÁRVORE GERADORA MÍNIMA - ALGORITMO DE KRUSKAL.
 *
 * Ideia: ordena TODAS as arestas por peso crescente e vai aceitando uma a uma, desde que não
 * feche um ciclo (verificado com União-Busca). Pára quando tem V-1 arestas (ou as acaba).
 * Complexidade: O(E log E), dominada pela ordenação.
 *
 * Diferença para o Prim: o Kruskal pode ter, a meio, várias árvores separadas (uma floresta)
 * que só se juntam no fim; o Prim tem sempre uma única árvore a crescer.
 * No mapa, as arestas REJEITADAS (que formariam ciclo) aparecem a tracejado.
 */
public class MstKruskal implements Algoritmo 
{

    @Override public String nome() { return "MST - Kruskal"; }

    @Override public String descricao() 
    {
        return "Ordena as arestas e usa União-Busca para evitar ciclos. Complexidade O(E log E).";
    }

    @Override public boolean usaDestino() { return false; }

    @Override
    public Resultado executar(GrafoFerroviarioUcraniano g, int origem, int destino) 
    {
        Resultado r = new Resultado(nome());
        r.numerar = false;                            // aqui interessa a aresta, não a ordem dos vértices
        long t0 = System.nanoTime();
        int n = g.getN();

        List<Aresta> ordenadas = new ArrayList<Aresta>(g.getArestas());
        Collections.sort(ordenadas, (a, b) -> Integer.compare(a.peso, b.peso));   // sort estável
        r.log.add("Arestas ordenadas por peso: " + ordenadas.size());

        UniaoBusca uf = new UniaoBusca(n);
        int aceites = 0;
        int alvo = n - g.numeroComponentes();         // nº de arestas de uma floresta geradora
        for (Aresta e : ordenadas) 
        {
            String desc = g.getNome(e.u) + " - " + g.getNome(e.v) + " (peso " + e.peso + ")";
            if (uf.unir(e.u, e.v)) 
            {                  // componentes diferentes -> aceita
                aceites++;
                r.custoTotal += e.peso;
                r.passos.add(Passo.mover(e.u, e.v, "ACEITA " + desc + "  custo acumulado " + r.custoTotal));
                r.log.add("ACEITA    " + desc + "   custo acumulado: " + r.custoTotal);
            } else 
            {                                  // mesmo componente -> fecharia um ciclo
                r.passos.add(Passo.rejeitada(e.u, e.v, "REJEITA " + desc + ": formaria um ciclo"));
                r.log.add("rejeitada " + desc + "   -> formaria um ciclo");
            }
            if (aceites == alvo) 
            {
                r.log.add("Já há " + alvo + " arestas aceites: a árvore está completa.");
                break;                                // as restantes só poderiam formar ciclos
            }
        }

        r.detalhe = aceites + " arestas (V - componentes = " + alvo + ")";
        r.tempoNs = System.nanoTime() - t0;
        return r;
    }
}
