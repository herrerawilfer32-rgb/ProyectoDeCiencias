package motor.catalogo;

import java.util.*;

/**
 * Catalogo global del motor: registra todos los espacios existentes.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class Catalogo {

    private Map<String, Esquema> espacios = new LinkedHashMap<>();

    public void registrar(Esquema e) {
        espacios.put(e.getNombreEspacio(), e);
    }

    public void eliminar(String nombre) {
        espacios.remove(nombre.toLowerCase());
    }

    public Esquema obtener(String nombre) {
        return espacios.get(nombre.toLowerCase());
    }

    public boolean existe(String nombre) {
        return espacios.containsKey(nombre.toLowerCase());
    }

    public Collection<Esquema> todos() {
        return Collections.unmodifiableCollection(espacios.values());
    }

    public void limpiar() { espacios.clear(); }
}
