package motor.catalogo;

/**
 * Define un campo del esquema de un espacio de almacenamiento.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class Campo {
    private String nombre;
    private TipoDato tipo;
    private boolean clavePrimaria;
    private boolean indexado;

    public Campo(String nombre, TipoDato tipo, boolean clavePrimaria) {
        this.nombre        = nombre.trim().toLowerCase();
        this.tipo          = tipo;
        this.clavePrimaria = clavePrimaria;
        this.indexado      = clavePrimaria;
    }

    public String getNombre()        { return nombre; }
    public TipoDato getTipo()        { return tipo; }
    public boolean isClavePrimaria() { return clavePrimaria; }
    public boolean isIndexado()      { return indexado; }
    public void setIndexado(boolean v) { this.indexado = v; }

    /** Valida y convierte un valor String al tipo del campo. */
    public Object parsearValor(String valor) {
        if (valor == null || valor.equalsIgnoreCase("null")) return null;
        String v = quitarComillas(valor);
        try {
            switch (tipo) {
                case ENTERO:  return Integer.parseInt(v);
                case REAL:    return Double.parseDouble(v);
                case BOOLEAN: return Boolean.parseBoolean(v);
                default:      return v;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Valor '" + v + "' no es valido para tipo " + tipo + " en campo " + nombre);
        }
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

    @Override
    public String toString() {
        return nombre + ":" + tipo + (clavePrimaria ? " PK" : "");
    }
}
