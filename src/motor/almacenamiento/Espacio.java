package motor.almacenamiento;

import motor.arbol.ArbolAVL;
import motor.catalogo.Esquema;

import java.util.*;

/**
 * Espacio de almacenamiento. Contiene los registros indexados por un ArbolAVL.
 * Soporta INSERT, SELECT, UPDATE, DELETE y busqueda por rango.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class Espacio {

    private Esquema esquema;
    private ArbolAVL<String, Registro> indice;

    public Espacio(Esquema esquema) {
        this.esquema = esquema;
        this.indice  = new ArbolAVL<>();
    }

    // ── INSERT ────────────────────────────────────────────

    /**
     * Inserta un registro. Lanza excepcion si la clave ya existe.
     * O(log n).
     */
    public void insertar(Registro r) {
        esquema.validar(r.getCampos());
        String clave = obtenerClave(r);
        if (indice.contiene(clave))
            throw new IllegalStateException("Clave duplicada: " + clave);
        indice.insertar(clave, r);
    }

    // ── SELECT ────────────────────────────────────────────

    /** Busqueda por clave primaria. O(log n). */
    public Registro buscarPorClave(String clave) {
        return indice.buscar(normalizar(clave));
    }

    /** Todos los registros en orden por clave. O(n). */
    public List<Registro> todos() {
        return indice.inorden();
    }

    /**
     * Busqueda con condicion simple campo operador valor.
     * Si el campo es la clave primaria usa el arbol; si no, escaneo lineal.
     */
    public List<Registro> buscarPorCondicion(String campo, String op, String valorStr) {
        List<Registro> resultado = new ArrayList<>();
        // Si es busqueda por clave primaria y es igualdad → O(log n)
        if (campo.equals(esquema.getCampoClave()) && (op.equals("=") || op.equals("=="))) {
            Registro r = buscarPorClave(valorStr);
            if (r != null) resultado.add(r);
            return resultado;
        }
        // Escaneo lineal O(n)
        Object valorObj = parsearValorCondicion(valorStr);
        for (Registro r : indice.inorden()) {
            if (r.cumple(campo, op, valorObj)) resultado.add(r);
        }
        return resultado;
    }

    /**
     * Busqueda por rango sobre la clave primaria.
     * O(log n + k).
     */
    public List<Registro> buscarRango(String desde, String hasta) {
        return indice.rango(normalizar(desde), normalizar(hasta));
    }

    /**
     * Busqueda por rango sobre cualquier campo.
     * Si el campo es la clave primaria usa el arbol; si no, escaneo lineal.
     */
    public List<Registro> buscarRango(String campo, String desde, String hasta) {
        if (campo.equals(esquema.getCampoClave())) return buscarRango(desde, hasta);

        List<Registro> resultado = new ArrayList<>();
        Object desdeObj = parsearValorCondicion(desde);
        Object hastaObj = parsearValorCondicion(hasta);
        for (Registro r : indice.inorden()) {
            if (r.enRango(campo, desdeObj, hastaObj)) resultado.add(r);
        }
        return resultado;
    }

    // ── UPDATE ────────────────────────────────────────────

    /**
     * Actualiza registros que cumplan la condicion.
     * Retorna numero de registros afectados.
     */
    public int actualizar(String campoCond, String op, String valorCond,
                          Map<String, Object> nuevosValores) {
        List<Registro> afectados = buscarPorCondicion(campoCond, op, valorCond);
        for (Registro r : afectados) {
            r.actualizar(nuevosValores);
        }
        return afectados.size();
    }

    /** Actualiza por clave primaria directamente. O(log n). */
    public boolean actualizarPorClave(String clave, Map<String, Object> nuevosValores) {
        Registro r = indice.buscar(normalizar(clave));
        if (r == null) return false;
        r.actualizar(nuevosValores);
        return true;
    }

    // ── DELETE ────────────────────────────────────────────

    /** Elimina por clave primaria. O(log n). */
    public boolean eliminarPorClave(String clave) {
        return indice.eliminar(normalizar(clave));
    }

    /**
     * Elimina todos los registros que cumplan la condicion.
     * Retorna numero de registros eliminados.
     */
    public int eliminarPorCondicion(String campo, String op, String valorStr) {
        List<Registro> afectados = buscarPorCondicion(campo, op, valorStr);
        int count = 0;
        for (Registro r : afectados) {
            String clave = obtenerClave(r);
            if (indice.eliminar(clave)) count++;
        }
        return count;
    }

    // ── UTILIDADES ────────────────────────────────────────

    private String obtenerClave(Registro r) {
        Object v = r.get(esquema.getCampoClave());
        if (v == null) throw new IllegalArgumentException("Falta clave primaria en el registro");
        return normalizar(v.toString());
    }

    private String normalizar(String s) {
        return quitarComillas(s).toLowerCase();
    }

    private Object parsearValorCondicion(String v) {
        v = quitarComillas(v);
        try { return Integer.parseInt(v); } catch (NumberFormatException e) {}
        try { return Double.parseDouble(v); } catch (NumberFormatException e) {}
        if (v.equalsIgnoreCase("true"))  return Boolean.TRUE;
        if (v.equalsIgnoreCase("false")) return Boolean.FALSE;
        return v;
    }

    private String quitarComillas(String s) {
        s = s.trim();
        if (s.length() >= 2) {
            char primero = s.charAt(0);
            char ultimo = s.charAt(s.length() - 1);
            if ((primero == '\'' || primero == '"') && primero == ultimo) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    public Esquema getEsquema() { return esquema; }
    public int getTamano()       { return indice.getTamano(); }
    public int getAltura()       { return indice.getAltura(); }
    public String visualizarArbol() { return indice.visualizar(); }
}
