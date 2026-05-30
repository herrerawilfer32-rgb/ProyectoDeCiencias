package motor.almacenamiento;

import java.util.*;

/**
 * Representa un registro almacenado en un espacio.
 * Internamente es un mapa campo -> valor.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class Registro {

    private Map<String, Object> campos;

    public Registro() {
        this.campos = new LinkedHashMap<>();
    }

    public Registro(Map<String, Object> campos) {
        this.campos = new LinkedHashMap<>(campos);
    }

    public void set(String campo, Object valor) {
        campos.put(campo.toLowerCase(), valor);
    }

    public Object get(String campo) {
        return campos.get(campo.toLowerCase());
    }

    public boolean tiene(String campo) {
        return campos.containsKey(campo.toLowerCase());
    }

    public Map<String, Object> getCampos() {
        return Collections.unmodifiableMap(campos);
    }

    public void actualizar(Map<String, Object> nuevos) {
        for (Map.Entry<String, Object> e : nuevos.entrySet()) {
            campos.put(e.getKey().toLowerCase(), e.getValue());
        }
    }

    /** Serializa a JSON simple (una linea). */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        boolean primero = true;
        for (Map.Entry<String, Object> e : campos.entrySet()) {
            if (!primero) sb.append(",");
            primero = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null)             sb.append("null");
            else if (v instanceof String) sb.append("\"").append(((String)v).replace("\"","\\\"")).append("\"");
            else                       sb.append(v.toString());
        }
        return sb.append("}").toString();
    }

    /** Parsea desde JSON simple (una linea). */
    public static Registro fromJson(String json) {
        Registro r = new Registro();
        String contenido = json.trim();
        if (contenido.startsWith("{")) contenido = contenido.substring(1);
        if (contenido.endsWith("}"))   contenido = contenido.substring(0, contenido.length()-1);
        if (contenido.trim().isEmpty()) return r;

        // Parser simple de pares clave:valor
        List<String> tokens = splitJson(contenido);
        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty()) continue;
            int sep = token.indexOf("\":");
            if (sep == -1) continue;
            String clave = token.substring(1, sep).trim();
            String valor = token.substring(sep+2).trim();
            r.set(clave, parseValor(valor));
        }
        return r;
    }

    private static List<String> splitJson(String s) {
        List<String> partes = new ArrayList<>();
        int depth = 0; boolean inStr = false; int inicio = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i-1) != '\\')) inStr = !inStr;
            if (!inStr) {
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;
                if (c == ',' && depth == 0) {
                    partes.add(s.substring(inicio, i));
                    inicio = i + 1;
                }
            }
        }
        partes.add(s.substring(inicio));
        return partes;
    }

    private static Object parseValor(String v) {
        v = v.trim();
        if (v.equals("null"))  return null;
        if (v.equals("true"))  return Boolean.TRUE;
        if (v.equals("false")) return Boolean.FALSE;
        if (v.startsWith("\"") && v.endsWith("\""))
            return v.substring(1, v.length()-1).replace("\\\"","\"");
        try { return Integer.parseInt(v); } catch (NumberFormatException e) {}
        try { return Double.parseDouble(v); } catch (NumberFormatException e) {}
        return v;
    }

    /** Verifica si el registro cumple una condicion simple campo=valor. */
    public boolean cumple(String campo, String operador, Object valorBuscado) {
        Object actual = get(campo);
        if (actual == null) return false;
        int cmp = comparar(actual, valorBuscado);
        switch (operador) {
            case "=":  case "==": return cmp == 0;
            case "!=": case "<>": return cmp != 0;
            case "<":             return cmp <  0;
            case "<=":            return cmp <= 0;
            case ">":             return cmp >  0;
            case ">=":            return cmp >= 0;
            default:              return false;
        }
    }

    /** Verifica si el valor de un campo esta dentro del rango inclusivo [desde, hasta]. */
    public boolean enRango(String campo, Object desde, Object hasta) {
        Object actual = get(campo);
        if (actual == null) return false;
        return comparar(actual, desde) >= 0 && comparar(actual, hasta) <= 0;
    }

    private int comparar(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                @SuppressWarnings("unchecked")
                Comparable<Object> ca = (Comparable<Object>) a;
                return ca.compareTo(b);
            } catch (ClassCastException e) {
                return a.toString().compareTo(b.toString());
            }
        }
        return a.toString().compareTo(b.toString());
    }

    @Override
    public String toString() { return toJson(); }
}
