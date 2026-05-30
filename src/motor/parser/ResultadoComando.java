package motor.parser;

import motor.almacenamiento.Registro;
import java.util.List;

/**
 * Resultado de ejecutar un comando en el motor.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class ResultadoComando {

    public enum Tipo { OK, FILAS, ERROR, SALIR }

    private Tipo tipo;
    private String mensaje;
    private List<Registro> filas;
    private int afectados;

    private ResultadoComando(Tipo tipo, String mensaje, List<Registro> filas, int afectados) {
        this.tipo      = tipo;
        this.mensaje   = mensaje;
        this.filas     = filas;
        this.afectados = afectados;
    }

    public static ResultadoComando ok(String msg)                { return new ResultadoComando(Tipo.OK,    msg,  null,  0); }
    public static ResultadoComando filas(List<Registro> f)       { return new ResultadoComando(Tipo.FILAS, null, f,     f.size()); }
    public static ResultadoComando error(String msg)             { return new ResultadoComando(Tipo.ERROR, msg,  null,  0); }
    public static ResultadoComando afectados(int n, String msg)  { return new ResultadoComando(Tipo.OK,    msg,  null,  n); }
    public static ResultadoComando salir()                       { return new ResultadoComando(Tipo.SALIR, "Hasta luego.", null, 0); }

    public Tipo getTipo()           { return tipo; }
    public String getMensaje()      { return mensaje; }
    public List<Registro> getFilas(){ return filas; }
    public int getAfectados()       { return afectados; }
    public boolean isError()        { return tipo == Tipo.ERROR; }
    public boolean isSalir()        { return tipo == Tipo.SALIR; }
}
