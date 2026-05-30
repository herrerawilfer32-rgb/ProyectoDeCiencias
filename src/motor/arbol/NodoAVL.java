package motor.arbol;

/**
 * Nodo del arbol AVL generico.
 * La clave es Comparable para soportar String, Integer, Double.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class NodoAVL<K extends Comparable<K>, V> {

    K clave;
    V valor;
    NodoAVL<K, V> izq, der;
    int altura;

    public NodoAVL(K clave, V valor) {
        this.clave  = clave;
        this.valor  = valor;
        this.altura = 1;
    }
}
