package motor.catalogo;

/**
 * Tipos de datos soportados por el motor.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public enum TipoDato {
    ENTERO, TEXTO, REAL, BOOLEAN;

    public static TipoDato parse(String s) {
        switch (s.toUpperCase().trim()) {
            case "ENTERO": case "INT": case "INTEGER": return ENTERO;
            case "TEXTO":  case "TEXT": case "STRING": return TEXTO;
            case "REAL":   case "FLOAT": case "DOUBLE": return REAL;
            case "BOOLEAN": case "BOOL": return BOOLEAN;
            default: throw new IllegalArgumentException("Tipo desconocido: " + s);
        }
    }
}
