package cli;

import motor.almacenamiento.Registro;
import motor.parser.Motor;
import motor.parser.ResultadoComando;

import java.io.*;
import java.util.*;

/**
 * REPL (Read-Eval-Print Loop) del motor de base de datos.
 * Acepta comandos por consola o por archivo de script.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class REPL {

    private Motor motor;
    private static final String VERSION = "1.0";
    private static final String PROMPT  = "MotorBD> ";

    public REPL(String dirDatos) {
        this.motor = new Motor(dirDatos);
    }

    // ── MODO INTERACTIVO ──────────────────────────────────

    public void iniciarInteractivo() {
        imprimirBanner();
        Scanner sc = new Scanner(System.in);
        StringBuilder buffer = new StringBuilder();

        while (true) {
            System.out.print(buffer.length() == 0 ? PROMPT : "      -> ");
            if (!sc.hasNextLine()) break;
            String linea = sc.nextLine();

            // Acumular lineas multi-linea (terminan en ;)
            buffer.append(" ").append(linea.trim());
            if (!linea.trim().endsWith(";") && sc.hasNextLine()) {
                // Si no termina en ; y no es EXIT/QUIT seguir leyendo
                String upper = linea.trim().toUpperCase();
                if (!upper.equals("EXIT") && !upper.equals("QUIT")) continue;
            }

            String cmd = buffer.toString().trim();
            // Quitar punto y coma final
            if (cmd.endsWith(";")) cmd = cmd.substring(0, cmd.length()-1).trim();
            buffer.setLength(0);

            if (cmd.isEmpty()) continue;

            ResultadoComando res = motor.ejecutar(cmd);
            imprimirResultado(res);
            if (res.isSalir()) break;
        }
        sc.close();
    }

    // ── MODO SCRIPT ───────────────────────────────────────

    public void ejecutarScript(String rutaArchivo) {
        System.out.println("Ejecutando script: " + rutaArchivo);
        System.out.println("-".repeat(50));
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            StringBuilder buffer = new StringBuilder();
            int numLinea = 0;
            while ((linea = br.readLine()) != null) {
                numLinea++;
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("--")) continue;
                buffer.append(" ").append(linea);
                if (linea.endsWith(";")) {
                    String cmd = buffer.toString().trim();
                    if (cmd.endsWith(";")) cmd = cmd.substring(0, cmd.length()-1).trim();
                    buffer.setLength(0);
                    System.out.println(PROMPT + cmd);
                    ResultadoComando res = motor.ejecutar(cmd);
                    imprimirResultado(res);
                    if (res.isSalir()) break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer script: " + e.getMessage());
        }
        System.out.println("-".repeat(50));
        System.out.println("Script completado.");
    }

    // ── IMPRESION ─────────────────────────────────────────

    private void imprimirResultado(ResultadoComando res) {
        switch (res.getTipo()) {
            case OK:
                if (res.getMensaje() != null && !res.getMensaje().isEmpty())
                    System.out.println(res.getMensaje());
                break;
            case FILAS:
                imprimirFilas(res.getFilas());
                System.out.println("(" + res.getAfectados() + " fila(s))");
                break;
            case ERROR:
                System.out.println("[ERROR] " + res.getMensaje());
                break;
            case SALIR:
                System.out.println(res.getMensaje());
                break;
        }
    }

    private void imprimirFilas(List<Registro> filas) {
        if (filas == null || filas.isEmpty()) {
            System.out.println("(sin resultados)");
            return;
        }
        // Recopilar todas las claves
        LinkedHashSet<String> claves = new LinkedHashSet<>();
        for (Registro r : filas) claves.addAll(r.getCampos().keySet());

        // Calcular anchos de columna
        Map<String, Integer> anchos = new LinkedHashMap<>();
        for (String k : claves) anchos.put(k, k.length());
        for (Registro r : filas) {
            for (String k : claves) {
                Object v = r.get(k);
                int len = v == null ? 4 : v.toString().length();
                if (len > anchos.get(k)) anchos.put(k, len);
            }
        }

        // Cabecera
        StringBuilder sep = new StringBuilder("+");
        StringBuilder cab = new StringBuilder("|");
        for (String k : claves) {
            int w = anchos.get(k) + 2;
            sep.append("-".repeat(w)).append("+");
            cab.append(String.format(" %-" + anchos.get(k) + "s ", k)).append("|");
        }
        System.out.println(sep);
        System.out.println(cab);
        System.out.println(sep);

        // Filas
        for (Registro r : filas) {
            StringBuilder fila = new StringBuilder("|");
            for (String k : claves) {
                Object v = r.get(k);
                String s = v == null ? "NULL" : v.toString();
                fila.append(String.format(" %-" + anchos.get(k) + "s ", s)).append("|");
            }
            System.out.println(fila);
        }
        System.out.println(sep);
    }

    private void imprimirBanner() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║        MOTOR BD  v" + VERSION + "  —  Gestion de datos      ║");
        System.out.println("║  Crea, consulta, actualiza, elimina y guarda     ║");
        System.out.println("║  Indice interno: Arbol AVL autobalanceado        ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║  Escribe MENU para ver las tareas principales    ║");
        System.out.println("║  Escribe EXIT para salir                         ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
        imprimirResultado(motor.ejecutar("HELP"));
        System.out.println();
        imprimirResultado(motor.ejecutar("SHOW SPACES"));
        System.out.println();
    }

    // ── MAIN ──────────────────────────────────────────────

    public static void main(String[] args) {
        String dirDatos = "data";
        String script   = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--data") && i+1 < args.length) dirDatos = args[i+1];
            if (args[i].equals("--script") && i+1 < args.length) script = args[i+1];
        }

        REPL repl = new REPL(dirDatos);
        if (script != null) {
            repl.ejecutarScript(script);
        } else {
            repl.iniciarInteractivo();
        }
    }
}
