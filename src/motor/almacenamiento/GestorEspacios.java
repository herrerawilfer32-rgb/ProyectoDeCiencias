package motor.almacenamiento;

import motor.catalogo.Catalogo;
import motor.catalogo.Esquema;

import java.util.*;

/**
 * Gestor central de espacios de almacenamiento.
 * Mantiene en memoria todos los espacios activos.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class GestorEspacios {

    private Map<String, Espacio> espacios = new LinkedHashMap<>();
    private Catalogo catalogo;

    public GestorEspacios(Catalogo catalogo) {
        this.catalogo = catalogo;
    }

    /** Crea un nuevo espacio. Lanza excepcion si ya existe. */
    public Espacio crear(Esquema esquema) {
        String nombre = esquema.getNombreEspacio();
        if (espacios.containsKey(nombre))
            throw new IllegalStateException("El espacio '" + nombre + "' ya existe.");
        Espacio e = new Espacio(esquema);
        espacios.put(nombre, e);
        catalogo.registrar(esquema);
        return e;
    }

    /** Elimina un espacio y su esquema. */
    public boolean eliminar(String nombre) {
        nombre = nombre.toLowerCase();
        if (!espacios.containsKey(nombre)) return false;
        espacios.remove(nombre);
        catalogo.eliminar(nombre);
        return true;
    }

    public Espacio obtener(String nombre) {
        return espacios.get(nombre.toLowerCase());
    }

    public boolean existe(String nombre) {
        return espacios.containsKey(nombre.toLowerCase());
    }

    public Collection<Espacio> todos() {
        return Collections.unmodifiableCollection(espacios.values());
    }

    /** Carga un espacio ya existente (para persistencia). */
    public void cargar(Espacio e) {
        espacios.put(e.getEsquema().getNombreEspacio(), e);
    }

    public void limpiar() { espacios.clear(); }
}
