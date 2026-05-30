package motor.arbol;

import java.util.*;

/**
 * Arbol AVL generico autobalanceado.
 * Soporta insercion, eliminacion, busqueda puntual, busqueda por rango
 * y recorrido inorden.
 * Complejidad: O(log n) en todas las operaciones sobre el indice.
 * Implementado sin librerias externas.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class ArbolAVL<K extends Comparable<K>, V> {

    private NodoAVL<K, V> raiz;
    private int tamano;

    public ArbolAVL() { this.raiz = null; this.tamano = 0; }

    // ── Utilidades ────────────────────────────────────────

    private int altura(NodoAVL<K,V> n)       { return n == null ? 0 : n.altura; }
    private int fb(NodoAVL<K,V> n)           { return n == null ? 0 : altura(n.izq) - altura(n.der); }
    private void actualizarAltura(NodoAVL<K,V> n) {
        n.altura = 1 + Math.max(altura(n.izq), altura(n.der));
    }

    // ── Rotaciones ────────────────────────────────────────

    private NodoAVL<K,V> rotDer(NodoAVL<K,V> y) {
        NodoAVL<K,V> x = y.izq, t = x.der;
        x.der = y; y.izq = t;
        actualizarAltura(y); actualizarAltura(x);
        return x;
    }

    private NodoAVL<K,V> rotIzq(NodoAVL<K,V> x) {
        NodoAVL<K,V> y = x.der, t = y.izq;
        y.izq = x; x.der = t;
        actualizarAltura(x); actualizarAltura(y);
        return y;
    }

    private NodoAVL<K,V> balancear(NodoAVL<K,V> n) {
        actualizarAltura(n);
        int b = fb(n);
        if (b >  1 && fb(n.izq) >= 0) return rotDer(n);
        if (b >  1 && fb(n.izq) <  0) { n.izq = rotIzq(n.izq); return rotDer(n); }
        if (b < -1 && fb(n.der) <= 0) return rotIzq(n);
        if (b < -1 && fb(n.der) >  0) { n.der = rotDer(n.der); return rotIzq(n); }
        return n;
    }

    // ── Insertar ──────────────────────────────────────────

    /** Inserta o actualiza una clave. O(log n). */
    public void insertar(K clave, V valor) {
        boolean[] nuevo = {false};
        raiz = insertar(raiz, clave, valor, nuevo);
        if (nuevo[0]) tamano++;
    }

    private NodoAVL<K,V> insertar(NodoAVL<K,V> n, K clave, V valor, boolean[] nuevo) {
        if (n == null) { nuevo[0] = true; return new NodoAVL<>(clave, valor); }
        int c = clave.compareTo(n.clave);
        if      (c < 0) n.izq = insertar(n.izq, clave, valor, nuevo);
        else if (c > 0) n.der = insertar(n.der, clave, valor, nuevo);
        else            n.valor = valor;   // actualizar
        return balancear(n);
    }

    // ── Buscar ────────────────────────────────────────────

    /** Busqueda puntual por clave. O(log n). */
    public V buscar(K clave) {
        NodoAVL<K,V> n = buscarNodo(raiz, clave);
        return n != null ? n.valor : null;
    }

    private NodoAVL<K,V> buscarNodo(NodoAVL<K,V> n, K clave) {
        if (n == null) return null;
        int c = clave.compareTo(n.clave);
        if (c < 0) return buscarNodo(n.izq, clave);
        if (c > 0) return buscarNodo(n.der, clave);
        return n;
    }

    public boolean contiene(K clave) { return buscar(clave) != null; }

    // ── Busqueda por rango ────────────────────────────────

    /**
     * Retorna todos los valores cuya clave esta en [desde, hasta].
     * O(log n + k) donde k es el numero de resultados.
     */
    public List<V> rango(K desde, K hasta) {
        List<V> res = new ArrayList<>();
        rango(raiz, desde, hasta, res);
        return res;
    }

    private void rango(NodoAVL<K,V> n, K desde, K hasta, List<V> res) {
        if (n == null) return;
        int cDesde = desde.compareTo(n.clave);
        int cHasta = hasta.compareTo(n.clave);
        if (cDesde < 0) rango(n.izq, desde, hasta, res);
        if (cDesde <= 0 && cHasta >= 0) res.add(n.valor);
        if (cHasta > 0) rango(n.der, desde, hasta, res);
    }

    // ── Eliminar ──────────────────────────────────────────

    /** Elimina una clave. O(log n). */
    public boolean eliminar(K clave) {
        if (!contiene(clave)) return false;
        raiz = eliminar(raiz, clave);
        tamano--;
        return true;
    }

    private NodoAVL<K,V> eliminar(NodoAVL<K,V> n, K clave) {
        if (n == null) return null;
        int c = clave.compareTo(n.clave);
        if      (c < 0) n.izq = eliminar(n.izq, clave);
        else if (c > 0) n.der = eliminar(n.der, clave);
        else {
            if (n.izq == null) return n.der;
            if (n.der == null) return n.izq;
            NodoAVL<K,V> suc = minimo(n.der);
            n.clave = suc.clave; n.valor = suc.valor;
            n.der = eliminar(n.der, suc.clave);
        }
        return balancear(n);
    }

    private NodoAVL<K,V> minimo(NodoAVL<K,V> n) {
        while (n.izq != null) n = n.izq;
        return n;
    }

    // ── Recorrido inorden ─────────────────────────────────

    /** Todos los valores en orden ascendente de clave. O(n). */
    public List<V> inorden() {
        List<V> lista = new ArrayList<>();
        inorden(raiz, lista);
        return lista;
    }

    private void inorden(NodoAVL<K,V> n, List<V> lista) {
        if (n == null) return;
        inorden(n.izq, lista);
        lista.add(n.valor);
        inorden(n.der, lista);
    }

    /** Pares clave-valor en orden ascendente. */
    public List<Map.Entry<K,V>> inordenEntradas() {
        List<Map.Entry<K,V>> lista = new ArrayList<>();
        inordenEntradas(raiz, lista);
        return lista;
    }

    private void inordenEntradas(NodoAVL<K,V> n, List<Map.Entry<K,V>> lista) {
        if (n == null) return;
        inordenEntradas(n.izq, lista);
        lista.add(new AbstractMap.SimpleEntry<>(n.clave, n.valor));
        inordenEntradas(n.der, lista);
    }

    // ── Visualizacion ASCII ───────────────────────────────

    /** Representacion ASCII del arbol para la CLI. */
    public String visualizar() {
        if (raiz == null) return "(arbol vacio)";
        StringBuilder sb = new StringBuilder();
        visualizar(raiz, sb, "", "");
        return sb.toString();
    }

    private void visualizar(NodoAVL<K,V> n, StringBuilder sb, String prefix, String childPrefix) {
        if (n == null) return;
        sb.append(prefix).append("[").append(n.clave).append("] h=").append(n.altura)
          .append(" fb=").append(fb(n)).append("\n");
        if (n.der != null || n.izq != null) {
            visualizar(n.der, sb, childPrefix + "├── R: ", childPrefix + "│   ");
            visualizar(n.izq, sb, childPrefix + "└── L: ", childPrefix + "    ");
        }
    }

    // ── Info ──────────────────────────────────────────────

    public int getTamano()  { return tamano; }
    public int getAltura()  { return altura(raiz); }
    public boolean estaVacio() { return raiz == null; }
}
