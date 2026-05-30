package motor.catalogo;

import java.util.*;

/**
 * Esquema de un espacio de almacenamiento.
 * Puede ser fijo (relacional) o libre (no relacional sin esquema).
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class Esquema {

    private String nombreEspacio;
    private boolean esquemaLibre;
    private List<Campo> campos;
    private String campoClave;

    /** Constructor para esquema definido (relacional). */
    public Esquema(String nombreEspacio, List<Campo> campos) {
        this.nombreEspacio = nombreEspacio.trim().toLowerCase();
        this.esquemaLibre  = false;
        this.campos        = campos;
        // Detectar clave primaria
        for (Campo c : campos) {
            if (c.isClavePrimaria()) { this.campoClave = c.getNombre(); break; }
        }
        if (this.campoClave == null && !campos.isEmpty()) {
            this.campoClave = campos.get(0).getNombre();
        }
    }

    /** Constructor para espacio sin esquema fijo (no relacional). */
    public Esquema(String nombreEspacio) {
        this.nombreEspacio = nombreEspacio.trim().toLowerCase();
        this.esquemaLibre  = true;
        this.campos        = new ArrayList<>();
        this.campoClave    = "id";
    }

    public String getNombreEspacio() { return nombreEspacio; }
    public boolean isEsquemaLibre()  { return esquemaLibre; }
    public List<Campo> getCampos()   { return Collections.unmodifiableList(campos); }
    public String getCampoClave()    { return campoClave; }

    public Campo getCampo(String nombre) {
        for (Campo c : campos) if (c.getNombre().equals(nombre.toLowerCase())) return c;
        return null;
    }

    /** Valida que un registro tenga la clave primaria y tipos correctos. */
    public void validar(Map<String, Object> registro) {
        if (esquemaLibre) {
            if (!registro.containsKey(campoClave))
                throw new IllegalArgumentException("El registro debe tener el campo '" + campoClave + "'");
            return;
        }

        for (String campo : registro.keySet()) {
            if (getCampo(campo) == null)
                throw new IllegalArgumentException("El campo '" + campo + "' no existe en el esquema '" + nombreEspacio + "'");
        }
        if (!registro.containsKey(campoClave))
            throw new IllegalArgumentException("Falta la clave primaria: " + campoClave);
        for (Campo c : campos) {
            if (!registro.containsKey(c.getNombre()))
                throw new IllegalArgumentException("Falta el campo requerido '" + c.getNombre() + "' en el registro");
        }
    }

    @Override
    public String toString() {
        if (esquemaLibre) return nombreEspacio + " [libre]";
        StringBuilder sb = new StringBuilder(nombreEspacio + " (");
        for (int i = 0; i < campos.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(campos.get(i));
        }
        return sb.append(")").toString();
    }
}
