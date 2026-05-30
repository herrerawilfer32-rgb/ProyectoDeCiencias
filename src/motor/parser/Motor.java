package motor.parser;

import java.util.*;
import motor.almacenamiento.*;
import motor.catalogo.*;
import motor.persistencia.GestorPersistencia;

/**
 * Motor principal del gestor de base de datos. Parsea y ejecuta comandos en
 * lenguaje propio (SQL-like).
 *
 * Comandos soportados: CREATE SPACE <nombre> [(<campo> <tipo> [PK], ...)] DROP
 * SPACE <nombre>
 * SHOW SPACES DESC <nombre>
 * INSERT INTO <nombre> VALUES (<v1>, <v2>, ...) INSERT INTO <nombre>
 * (<c1>,<c2>) VALUES (<v1>,<v2>) SELECT * FROM <nombre> [WHERE <campo> <op>
 * <valor>] SELECT * FROM <nombre> WHERE <campo> BETWEEN <v1> AND <v2>
 * UPDATE <nombre> SET <c>=<v> [,<c>=<v>] WHERE <campo> <op> <valor>
 * DELETE FROM <nombre> WHERE <campo> <op> <valor>
 * DELETE FROM <nombre> ALL TREE <nombre>
 * SAVE <nombre> | SAVE ALL EXIT | QUIT
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class Motor {

    private Catalogo catalogo;
    private GestorEspacios gestorEspacios;
    private GestorPersistencia persistencia;

    public Motor(String dirDatos) {
        this.catalogo = new Catalogo();
        this.gestorEspacios = new GestorEspacios(catalogo);
        this.persistencia = new GestorPersistencia(dirDatos);
        // Cargar datos existentes
        persistencia.cargarTodo(gestorEspacios, catalogo);
    }

    // ── PUNTO DE ENTRADA ──────────────────────────────────
    public ResultadoComando ejecutar(String comando) {
        if (comando == null) {
            return ResultadoComando.error("Comando vacio.");
        }
        comando = comando.trim();
        if (comando.isEmpty() || comando.startsWith("--")) {
            return ResultadoComando.ok("");
        }

        String upper = comando.toUpperCase();
        try {
            if (upper.startsWith("CREATE SPACE")) {
                return crearEspacio(comando);
            }
            if (upper.startsWith("DROP SPACE")) {
                return eliminarEspacio(comando);
            }
            if (upper.startsWith("SHOW SPACES")) {
                return listarEspacios();
            }
            if (upper.startsWith("DESC ")) {
                return describir(comando);
            }
            if (upper.startsWith("INSERT INTO")) {
                return insertar(comando);
            }
            if (upper.startsWith("SELECT")) {
                return seleccionar(comando);
            }
            if (upper.startsWith("UPDATE")) {
                return actualizar(comando);
            }
            if (upper.startsWith("DELETE FROM")) {
                return eliminarRegistros(comando);
            }
            if (upper.startsWith("TREE")) {
                return verArbol(comando);
            }
            if (upper.startsWith("SAVE ALL")) {
                return guardarTodo();
            }
            if (upper.startsWith("SAVE ")) {
                return guardar(comando);
            }
            if (upper.equals("HELP") || upper.equals("AYUDA") || upper.equals("MENU")) {
                return ayuda();
            }
            if (upper.equals("EXIT") || upper.equals("QUIT")) {
                return ResultadoComando.salir();
            }
            return ResultadoComando.error("Comando no reconocido: " + comando + ". Usa HELP para ver ejemplos.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResultadoComando.error(e.getMessage() + " Usa HELP si necesitas ver la sintaxis.");
        } catch (Exception e) {
            return ResultadoComando.error("No se pudo ejecutar el comando. Detalle: " + e.getMessage());
        }
    }

    private ResultadoComando ayuda() {
        String msg
                = "MENU DE TAREAS BASICAS\n"
                + "============================================================\n"
                + "Usa estos comandos para manejar tus datos. Puedes copiar y\n"
                + "adaptar los ejemplos cambiando nombres y valores.\n\n"
                + "1. Crear una tabla o espacio\n"
                + "   CREATE SPACE alumnos (id ENTERO PK, nombre TEXTO, nota REAL);\n"
                + "   CREATE SPACE notas;        -- espacio libre con campo id\n\n"
                + "2. Agregar datos\n"
                + "   INSERT INTO alumnos VALUES (1, 'Ana', 4.5);\n"
                + "   INSERT INTO notas (id, materia, nota) VALUES ('n001', 'Calculo', 4.2);\n\n"
                + "3. Ver o buscar datos\n"
                + "   SELECT * FROM alumnos;\n"
                + "   SELECT * FROM alumnos WHERE id = 1;\n"
                + "   SELECT * FROM alumnos WHERE nota >= 4;\n\n"
                + "4. Buscar por rango\n"
                + "   SELECT * FROM alumnos WHERE id BETWEEN 1 AND 10;\n"
                + "   SELECT * FROM alumnos WHERE nota BETWEEN 3.5 AND 5.0;\n\n"
                + "5. Cambiar datos existentes\n"
                + "   UPDATE alumnos SET nota=4.8 WHERE id = 1;\n\n"
                + "6. Eliminar datos\n"
                + "   DELETE FROM alumnos WHERE id = 1;\n"
                + "   DELETE FROM alumnos ALL;\n\n"
                + "7. Revisar lo que tienes creado\n"
                + "   SHOW SPACES;\n"
                + "   DESC alumnos;\n\n"
                + "8. Guardar cambios\n"
                + "   SAVE ALL;\n\n"
                + "9. Salir del programa\n"
                + "    EXIT;\n"
                + "    QUIT;\n\n"
                + "Opcional para defensa tecnica:\n"
                + "   TREE alumnos;              -- muestra el arbol AVL del indice\n\n"
                + "Tipos permitidos: ENTERO, TEXTO, REAL, BOOLEAN.\n"
                + "Operadores: =, !=, <>, <, <=, >, >=, BETWEEN.\n"
                + "Consejo: termina cada comando con punto y coma (;).\n"
                + "Para volver a ver esta lista escribe HELP; o MENU;";
        return ResultadoComando.ok(msg);
    }

    // ── CREATE SPACE ──────────────────────────────────────
    private ResultadoComando crearEspacio(String cmd) {
        // CREATE SPACE nombre
        // CREATE SPACE nombre (campo tipo [PK], ...)
        String resto = cmd.substring(12).trim();
        int parenIdx = resto.indexOf('(');
        String nombre;
        Esquema esquema;

        if (parenIdx == -1) {
            // Sin esquema → libre
            nombre = resto.trim();
            esquema = new Esquema(nombre);
        } else {
            nombre = resto.substring(0, parenIdx).trim();
            String defCampos = resto.substring(parenIdx + 1, resto.lastIndexOf(')')).trim();
            List<Campo> campos = parsearCampos(defCampos);
            esquema = new Esquema(nombre, campos);
        }
        gestorEspacios.crear(esquema);
        persistencia.guardar(gestorEspacios.obtener(nombre));
        return ResultadoComando.ok("Espacio '" + nombre + "' creado. Clave: " + esquema.getCampoClave());
    }

    private List<Campo> parsearCampos(String def) {
        List<Campo> lista = new ArrayList<>();
        String[] partes = def.split(",");
        for (String p : partes) {
            String[] tokens = p.trim().split("\\s+");
            if (tokens.length < 2) {
                continue;
            }
            String nombre = tokens[0];
            TipoDato tipo = TipoDato.parse(tokens[1]);
            boolean pk = tokens.length >= 3 && tokens[2].equalsIgnoreCase("PK");
            lista.add(new Campo(nombre, tipo, pk));
        }
        // Si ninguno tiene PK, el primero es la clave
        boolean tienePK = false;
        for (Campo c : lista) {
            if (c.isClavePrimaria()) {
                tienePK = true;
                break;
            }
        }
        if (!tienePK && !lista.isEmpty()) {
            lista.set(0, new Campo(lista.get(0).getNombre(), lista.get(0).getTipo(), true));
        }
        return lista;
    }

    // ── DROP SPACE ────────────────────────────────────────
    private ResultadoComando eliminarEspacio(String cmd) {
        String nombre = cmd.substring(10).trim();
        if (!gestorEspacios.existe(nombre)) {
            return ResultadoComando.error("El espacio '" + nombre + "' no existe.");
        }
        gestorEspacios.eliminar(nombre);
        persistencia.eliminar(nombre.toLowerCase());
        return ResultadoComando.ok("Espacio '" + nombre + "' eliminado.");
    }

    // ── SHOW SPACES ───────────────────────────────────────
    private ResultadoComando listarEspacios() {
        StringBuilder sb = new StringBuilder();
        Collection<Espacio> lista = gestorEspacios.todos();
        if (lista.isEmpty()) {
            return ResultadoComando.ok("(Sin espacios creados)");
        }
        sb.append(String.format("%-20s %-10s %-8s %-8s\n", "ESPACIO", "ESQUEMA", "REGISTROS", "ALTURA AVL"));
        sb.append("-".repeat(52)).append("\n");
        for (Espacio e : lista) {
            sb.append(String.format("%-20s %-10s %-9d %-8d\n",
                    e.getEsquema().getNombreEspacio(),
                    e.getEsquema().isEsquemaLibre() ? "libre" : "fijo",
                    e.getTamano(),
                    e.getAltura()));
        }
        return ResultadoComando.ok(sb.toString().trim());
    }

    // ── DESC ──────────────────────────────────────────────
    private ResultadoComando describir(String cmd) {
        String nombre = cmd.substring(5).trim();
        Espacio esp = requerirEspacio(nombre);
        Esquema esq = esp.getEsquema();
        StringBuilder sb = new StringBuilder();
        sb.append("Espacio: ").append(esq.getNombreEspacio()).append("\n");
        sb.append("Tipo:    ").append(esq.isEsquemaLibre() ? "No relacional (libre)" : "Relacional (esquema fijo)").append("\n");
        sb.append("Clave:   ").append(esq.getCampoClave()).append("\n");
        sb.append("Registros: ").append(esp.getTamano()).append("\n");
        sb.append("Altura AVL: ").append(esp.getAltura()).append("\n");
        if (!esq.isEsquemaLibre()) {
            sb.append("\nCampos:\n");
            sb.append(String.format("  %-15s %-10s %-5s\n", "NOMBRE", "TIPO", "PK"));
            sb.append("  " + "-".repeat(32) + "\n");
            for (Campo c : esq.getCampos()) {
                sb.append(String.format("  %-15s %-10s %-5s\n",
                        c.getNombre(), c.getTipo(), c.isClavePrimaria() ? "SI" : ""));
            }
        }
        return ResultadoComando.ok(sb.toString().trim());
    }

    // ── INSERT ────────────────────────────────────────────
    private ResultadoComando insertar(String cmd) {
        // INSERT INTO nombre VALUES (v1, v2, ...)
        // INSERT INTO nombre (c1, c2) VALUES (v1, v2, ...)
        String resto = cmd.substring(11).trim(); // quitar "INSERT INTO"
        int valIdx = resto.toUpperCase().indexOf("VALUES");
        if (valIdx == -1) {
            return ResultadoComando.error("Falta VALUES en INSERT.");
        }

        String cabecera = resto.substring(0, valIdx).trim();
        String valsPart = resto.substring(valIdx + 6).trim();

        // Extraer nombre y campos opcionales
        String nombre;
        List<String> camposOrden = null;
        int parenIdx = cabecera.indexOf('(');
        if (parenIdx == -1) {
            nombre = cabecera.trim();
        } else {
            nombre = cabecera.substring(0, parenIdx).trim();
            String camposStr = cabecera.substring(parenIdx + 1, cabecera.lastIndexOf(')')).trim();
            camposOrden = splitCsv(camposStr);
        }

        Espacio esp = requerirEspacio(nombre);
        Esquema esq = esp.getEsquema();

        // Extraer valores
        if (!valsPart.startsWith("(") || !valsPart.contains(")")) {
            return ResultadoComando.error("Formato de VALUES incorrecto.");
        }
        String valsStr = valsPart.substring(1, valsPart.lastIndexOf(')')).trim();
        List<String> valores = splitCsv(valsStr);

        // Construir registro
        Registro r = new Registro();
        if (camposOrden != null) {
            if (camposOrden.size() != valores.size()) {
                return ResultadoComando.error("Numero de campos (" + camposOrden.size()
                        + ") no coincide con valores (" + valores.size() + ").");
            }
            // INSERT con lista de campos
            for (int i = 0; i < camposOrden.size() && i < valores.size(); i++) {
                String campo = camposOrden.get(i).trim();
                Object val = esq.isEsquemaLibre()
                        ? parsearValorLibre(valores.get(i).trim())
                        : parsearConEsquema(esq, campo, valores.get(i).trim());
                r.set(campo, val);
            }
        } else {
            // INSERT sin lista → asignar por orden del esquema
            List<Campo> campos = esq.getCampos();
            if (!esq.isEsquemaLibre() && campos.size() != valores.size()) {
                return ResultadoComando.error("Numero de valores (" + valores.size()
                        + ") no coincide con campos del esquema (" + campos.size() + ").");
            }
            if (esq.isEsquemaLibre()) {
                return ResultadoComando.error("Espacio libre requiere especificar campos: INSERT INTO " + nombre + " (campo,...) VALUES (...)");
            }
            for (int i = 0; i < campos.size(); i++) {
                r.set(campos.get(i).getNombre(),
                        campos.get(i).parsearValor(valores.get(i).trim()));
            }
        }

        esp.insertar(r);
        persistencia.guardar(esp);
        return ResultadoComando.ok("1 registro insertado en '" + nombre + "'.");
    }

    // ── SELECT ────────────────────────────────────────────
    private ResultadoComando seleccionar(String cmd) {
        // SELECT * FROM nombre [WHERE campo op valor]
        // SELECT * FROM nombre WHERE campo BETWEEN v1 AND v2
        String upper = cmd.toUpperCase();
        int fromIdx = upper.indexOf("FROM");
        if (fromIdx == -1) {
            return ResultadoComando.error("Falta FROM en SELECT.");
        }

        String restoFrom = cmd.substring(fromIdx + 4).trim();
        int whereIdx = restoFrom.toUpperCase().indexOf(" WHERE ");
        String nombre;
        String wherePart = null;

        if (whereIdx == -1) {
            nombre = restoFrom.trim();
        } else {
            nombre = restoFrom.substring(0, whereIdx).trim();
            wherePart = restoFrom.substring(whereIdx + 7).trim();
        }

        Espacio esp = requerirEspacio(nombre);
        List<Registro> resultado;

        if (wherePart == null) {
            resultado = esp.todos();
        } else {
            String wUpper = wherePart.toUpperCase();
            int betweenIdx = wUpper.indexOf(" BETWEEN ");
            if (betweenIdx != -1) {
                // rango
                String campo = wherePart.substring(0, betweenIdx).trim().toLowerCase();
                String resto2 = wherePart.substring(betweenIdx + 9).trim();
                String[] andParts = resto2.split("(?i)\\s+AND\\s+");
                if (andParts.length != 2) {
                    return ResultadoComando.error("BETWEEN requiere: campo BETWEEN v1 AND v2");
                }
                resultado = esp.buscarRango(campo, limpiar(andParts[0]), limpiar(andParts[1]));
            } else {
                String[] partes = parsearCondicion(wherePart);
                if (partes == null) {
                    return ResultadoComando.error("Condicion WHERE invalida: " + wherePart);
                }
                resultado = esp.buscarPorCondicion(partes[0], partes[1], partes[2]);
            }
        }

        return ResultadoComando.filas(resultado);
    }

    // ── UPDATE ────────────────────────────────────────────
    private ResultadoComando actualizar(String cmd) {
        // UPDATE nombre SET c1=v1, c2=v2 WHERE campo op valor
        String upper = cmd.toUpperCase();
        int setIdx = upper.indexOf(" SET ");
        int whereIdx = upper.indexOf(" WHERE ");
        if (setIdx == -1) {
            return ResultadoComando.error("Falta SET en UPDATE.");
        }
        if (whereIdx == -1) {
            return ResultadoComando.error("Falta WHERE en UPDATE.");
        }

        String nombre = cmd.substring(6, setIdx).trim();
        String setPart = cmd.substring(setIdx + 5, whereIdx).trim();
        String wherePart = cmd.substring(whereIdx + 7).trim();

        Espacio esp = requerirEspacio(nombre);
        Map<String, Object> nuevos = parsearSet(setPart, esp);
        String[] cond = parsearCondicion(wherePart);
        if (cond == null) {
            return ResultadoComando.error("Condicion WHERE invalida.");
        }

        int n = esp.actualizar(cond[0], cond[1], cond[2], nuevos);
        if (n > 0) {
            persistencia.guardar(esp);
        }
        return ResultadoComando.afectados(n, n + " registro(s) actualizado(s) en '" + nombre + "'.");
    }

    // ── DELETE ────────────────────────────────────────────
    private ResultadoComando eliminarRegistros(String cmd) {
        // DELETE FROM nombre WHERE campo op valor
        // DELETE FROM nombre ALL
        String upper = cmd.toUpperCase();
        int fromIdx = upper.indexOf("FROM");
        String restoFrom = cmd.substring(fromIdx + 4).trim();
        int whereIdx = restoFrom.toUpperCase().indexOf(" WHERE ");

        String nombre;
        String wherePart;

        if (whereIdx == -1) {
            // DELETE FROM nombre ALL o sin WHERE
            String[] partes = restoFrom.split("\\s+", 2);
            nombre = partes[0];
            wherePart = partes.length > 1 ? partes[1].trim() : "";
        } else {
            nombre = restoFrom.substring(0, whereIdx).trim();
            wherePart = restoFrom.substring(whereIdx + 7).trim();
        }

        Espacio esp = requerirEspacio(nombre);

        if (wherePart.isEmpty() || wherePart.equalsIgnoreCase("ALL")) {
            int total = esp.getTamano();
            // Eliminar todos
            for (Registro r : esp.todos()) {
                Object clave = r.get(esp.getEsquema().getCampoClave());
                if (clave != null) {
                    esp.eliminarPorClave(clave.toString());
                }
            }
            persistencia.guardar(esp);
            return ResultadoComando.afectados(total, total + " registro(s) eliminado(s) de '" + nombre + "'.");
        }

        String[] cond = parsearCondicion(wherePart);
        if (cond == null) {
            return ResultadoComando.error("Condicion WHERE invalida.");
        }
        int n = esp.eliminarPorCondicion(cond[0], cond[1], cond[2]);
        if (n > 0) {
            persistencia.guardar(esp);
        }
        return ResultadoComando.afectados(n, n + " registro(s) eliminado(s) de '" + nombre + "'.");
    }

    // ── TREE ──────────────────────────────────────────────
    private ResultadoComando verArbol(String cmd) {
        String nombre = cmd.substring(4).trim();
        Espacio esp = requerirEspacio(nombre);
        return ResultadoComando.ok("Arbol AVL de '" + nombre + "':\n" + esp.visualizarArbol());
    }

    // ── SAVE ──────────────────────────────────────────────
    private ResultadoComando guardar(String cmd) {
        String nombre = cmd.substring(4).trim();
        Espacio esp = requerirEspacio(nombre);
        persistencia.guardar(esp);
        return ResultadoComando.ok("'" + nombre + "' guardado en disco.");
    }

    private ResultadoComando guardarTodo() {
        int n = 0;
        for (Espacio e : gestorEspacios.todos()) {
            persistencia.guardar(e);
            n++;
        }
        return ResultadoComando.ok(n + " espacio(s) guardado(s).");
    }

    // ── UTILIDADES ────────────────────────────────────────
    private Espacio requerirEspacio(String nombre) {
        Espacio e = gestorEspacios.obtener(nombre.trim());
        if (e == null) {
            throw new IllegalArgumentException("Espacio no existe: '" + nombre + "'");
        }
        return e;
    }

    private String[] parsearCondicion(String where) {
        // Operadores multi-char primero
        String[] ops = {"<=", ">=", "!=", "<>", "=", "<", ">"};
        for (String op : ops) {
            int idx = where.indexOf(op);
            if (idx > 0) {
                String campo = where.substring(0, idx).trim().toLowerCase();
                String valor = where.substring(idx + op.length()).trim();
                return new String[]{campo, op, limpiar(valor)};
            }
        }
        return null;
    }

    private Map<String, Object> parsearSet(String setPart, Espacio esp) {
        Map<String, Object> map = new LinkedHashMap<>();
        String[] asignaciones = setPart.split(",");
        for (String asig : asignaciones) {
            int eq = asig.indexOf('=');
            if (eq == -1) {
                continue;
            }
            String campo = asig.substring(0, eq).trim().toLowerCase();
            String valor = limpiar(asig.substring(eq + 1).trim());
            Esquema esq = esp.getEsquema();
            if (!esq.isEsquemaLibre()) {
                Campo c = esq.getCampo(campo);
                if (c == null) {
                    throw new IllegalArgumentException("El campo '" + campo + "' no existe en el esquema '" + esq.getNombreEspacio() + "'.");
                }
                if (campo.equals(esq.getCampoClave())) {
                    throw new IllegalArgumentException("No se permite actualizar la clave primaria '" + campo + "' porque romperia el indice AVL.");
                }
                map.put(campo, c.parsearValor(valor));
                continue;
            }
            map.put(campo, parsearValorLibre(valor));
        }
        return map;
    }

    private List<String> splitCsv(String s) {
        List<String> lista = new ArrayList<>();
        boolean inStr = false;
        char quoteChar = '\0';
        int inicio = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '\'' || c == '"') && (i == 0 || s.charAt(i - 1) != '\\')) {
                if (!inStr) {
                    inStr = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inStr = false;
                    quoteChar = '\0';
                }
            }
            if (c == ',' && !inStr) {
                lista.add(s.substring(inicio, i).trim());
                inicio = i + 1;
            }
        }
        lista.add(s.substring(inicio).trim());
        return lista;
    }

    private Object parsearValorLibre(String v) {
        v = limpiar(v);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
        }
        if (v.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (v.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        return v;
    }

    private Object parsearConEsquema(Esquema esq, String campo, String valor) {
        Campo c = esq.getCampo(campo);
        if (c != null) {
            return c.parsearValor(valor);
        }
        return parsearValorLibre(valor);
    }

    private String limpiar(String s) {
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
}
