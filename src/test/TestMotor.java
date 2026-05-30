package test;

import motor.arbol.ArbolAVL;
import motor.parser.Motor;
import motor.parser.ResultadoComando;

/**
 * Suite de pruebas del motor. Ejecuta sin JUnit.
 * Cubre: CREATE, INSERT, SELECT, UPDATE, DELETE, RANGE, persistencia.
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class TestMotor {

    private static int ok = 0, fail = 0;

    public static void main(String[] args) {
        System.out.println("=== SUITE DE PRUEBAS — MOTOR BD AVL ===\n");

        String dir = "data_test";
        new java.io.File(dir + "/esquemas").mkdirs();
        new java.io.File(dir + "/espacios").mkdirs();

        probarArbolAVL();

        Motor m = new Motor(dir);

        // ── Crear espacios ──────────────────────────────
        titulo("1. Gestion de esquemas");
        assert_ok(m, "CREATE SPACE estudiantes (id ENTERO PK, nombre TEXTO, promedio REAL, activo BOOLEAN)",
            "creado");
        assert_ok(m, "CREATE SPACE notas", "creado");
        assert_ok(m, "CREATE SPACE productos (id ENTERO PK, nombre TEXTO, precio REAL)", "creado");
        assert_ok(m, "SHOW SPACES",         "estudiantes");
        assert_error(m, "CREATE SPACE estudiantes (id ENTERO PK)", "ya existe");

        // ── INSERT ──────────────────────────────────────
        titulo("2. INSERT");
        assert_ok(m, "INSERT INTO estudiantes VALUES (1, 'Ana Gomez', 4.5, true)",  "1 registro");
        assert_ok(m, "INSERT INTO estudiantes VALUES (2, 'Carlos Ruiz', 3.8, true)","1 registro");
        assert_ok(m, "INSERT INTO estudiantes VALUES (3, 'Laura Torres', 4.1, false)","1 registro");
        assert_ok(m, "INSERT INTO estudiantes VALUES (4, 'Miguel Herrera', 3.5, true)","1 registro");
        assert_ok(m, "INSERT INTO estudiantes VALUES (5, 'Sofia Castro', 4.9, true)","1 registro");
        assert_error(m, "INSERT INTO estudiantes VALUES (1, 'Duplicado', 1.0, false)", "duplicada");
        // Espacio libre
        assert_ok(m, "INSERT INTO notas (id, materia, nota) VALUES ('n001', 'Calculo', 4.2)", "1 registro");
        assert_ok(m, "INSERT INTO notas (id, materia, nota) VALUES ('n002', 'Algoritmos', 3.9)", "1 registro");
        // Texto con comillas dobles internas
        assert_ok(m, "INSERT INTO productos VALUES (1, 'Monitor 24\"', 950000)", "1 registro");
        assert_error(m, "INSERT INTO productos (id, nombre) VALUES (2, 'Incompleto')", "falta el campo");

        // ── SELECT ──────────────────────────────────────
        titulo("3. SELECT");
        assert_filas(m, "SELECT * FROM estudiantes", 5);
        assert_filas(m, "SELECT * FROM estudiantes WHERE id = 3", 1);
        assert_filas(m, "SELECT * FROM estudiantes WHERE promedio >= 4.0", 3);
        assert_filas(m, "SELECT * FROM estudiantes WHERE promedio >= 4", 3);
        assert_filas(m, "SELECT * FROM estudiantes WHERE activo = true", 4);
        assert_filas(m, "SELECT * FROM estudiantes WHERE nombre = 'Sofia Castro'", 1);
        assert_filas(m, "SELECT * FROM notas WHERE materia = 'Calculo'", 1);
        assert_filas(m, "SELECT * FROM productos WHERE nombre = 'Monitor 24\"'", 1);
        assert_filas(m, "SELECT * FROM productos WHERE precio >= 100000", 1);

        // ── RANGE ───────────────────────────────────────
        titulo("4. SELECT BETWEEN (rango AVL)");
        assert_filas(m, "SELECT * FROM estudiantes WHERE id BETWEEN 2 AND 4", 3);
        assert_filas(m, "SELECT * FROM estudiantes WHERE id BETWEEN 1 AND 5", 5);
        assert_filas(m, "SELECT * FROM estudiantes WHERE id BETWEEN 6 AND 9", 0);
        assert_filas(m, "SELECT * FROM estudiantes WHERE promedio BETWEEN 4 AND 4.6", 2);

        // ── UPDATE ──────────────────────────────────────
        titulo("5. UPDATE");
        assert_afectados(m, "UPDATE estudiantes SET promedio=4.7 WHERE id = 2", 1);
        assert_afectados(m, "UPDATE estudiantes SET activo=false WHERE promedio < 4.0", 1);
        assert_error(m, "UPDATE estudiantes SET id=20 WHERE id = 2", "clave primaria");
        // Verificar
        ResultadoComando r = m.ejecutar("SELECT * FROM estudiantes WHERE id = 2");
        assertTrue("Promedio actualizado",
            r.getFilas().get(0).get("promedio").toString().equals("4.7"));

        // ── DELETE ──────────────────────────────────────
        titulo("6. DELETE");
        assert_afectados(m, "DELETE FROM estudiantes WHERE id = 5", 1);
        assert_filas(m, "SELECT * FROM estudiantes", 4);
        assert_afectados(m, "DELETE FROM notas WHERE materia = 'Calculo'", 1);
        assert_filas(m, "SELECT * FROM notas", 1);

        // ── TREE ────────────────────────────────────────
        titulo("7. Visualizacion del arbol AVL");
        ResultadoComando tree = m.ejecutar("TREE estudiantes");
        assertFalse("Arbol no vacio", tree.getMensaje().contains("vacio"));

        // ── DESC ────────────────────────────────────────
        titulo("8. DESC");
        assert_ok(m, "DESC estudiantes", "estudiantes");

        // ── SAVE y recuperacion ─────────────────────────
        titulo("9. Persistencia y recuperacion");
        assert_ok(m, "SAVE ALL", "guardado");
        // Nuevo motor leyendo los mismos datos
        Motor m2 = new Motor(dir);
        assert_filas(m2, "SELECT * FROM estudiantes", 4);
        assert_filas(m2, "SELECT * FROM notas", 1);
        System.out.println("  Datos recuperados correctamente tras reinicio del motor.");

        // ── DROP ────────────────────────────────────────
        titulo("10. DROP SPACE");
        assert_ok(m, "DROP SPACE notas", "eliminado");
        assert_error(m, "SELECT * FROM notas", "no existe");

        // Limpiar datos de prueba
        borrarDir(new java.io.File(dir));

        // ── Resumen ─────────────────────────────────────
        System.out.println("\n" + "=".repeat(45));
        System.out.printf("  RESULTADO: %d pruebas OK, %d FALLIDAS%n", ok, fail);
        System.out.println("=".repeat(45));
        System.exit(fail > 0 ? 1 : 0);
    }

    // ── Helpers de asercion ───────────────────────────────

    private static void probarArbolAVL() {
        titulo("0. Arbol AVL");
        ArbolAVL<String, Integer> avl = new ArbolAVL<>();
        avl.insertar("30", 30);
        avl.insertar("20", 20);
        avl.insertar("10", 10);
        avl.insertar("40", 40);
        avl.insertar("50", 50);

        assertTrue("AVL busca clave existente", avl.buscar("20") == 20);
        assertTrue("AVL mantiene altura balanceada", avl.getAltura() <= 3);
        assertTrue("AVL recorrido inorder ordenado", avl.inorden().toString().equals("[10, 20, 30, 40, 50]"));
        assertTrue("AVL rango inclusivo", avl.rango("20", "40").toString().equals("[20, 30, 40]"));
        assertTrue("AVL elimina clave", avl.eliminar("30") && avl.buscar("30") == null);
        assertTrue("AVL tamano tras eliminar", avl.getTamano() == 4);
    }

    private static void titulo(String t) {
        System.out.println("\n--- " + t + " ---");
    }

    private static void assert_ok(Motor m, String cmd, String contiene) {
        ResultadoComando r = m.ejecutar(cmd);
        String s = r.getMensaje() != null ? r.getMensaje().toLowerCase() : "";
        boolean paso = !r.isError() && s.contains(contiene.toLowerCase());
        reportar(cmd, paso, r.isError() ? r.getMensaje() : null);
    }

    private static void assert_error(Motor m, String cmd, String contiene) {
        ResultadoComando r = m.ejecutar(cmd);
        String s = r.getMensaje() != null ? r.getMensaje().toLowerCase() : "";
        boolean paso = r.isError() && s.contains(contiene.toLowerCase());
        reportar("(error esperado) " + cmd, paso,
            !r.isError() ? "Se esperaba error pero no hubo" : null);
    }

    private static void assert_filas(Motor m, String cmd, int esperadas) {
        ResultadoComando r = m.ejecutar(cmd);
        int obtenidas = r.getFilas() != null ? r.getFilas().size() : 0;
        boolean paso = !r.isError() && obtenidas == esperadas;
        reportar(cmd + " [filas=" + esperadas + "]", paso,
            paso ? null : "Obtenidas: " + obtenidas + (r.isError() ? " | " + r.getMensaje() : ""));
    }

    private static void assert_afectados(Motor m, String cmd, int esperados) {
        ResultadoComando r = m.ejecutar(cmd);
        boolean paso = !r.isError() && r.getAfectados() == esperados;
        reportar(cmd + " [afectados=" + esperados + "]", paso,
            paso ? null : "Afectados: " + r.getAfectados() + (r.isError() ? " | " + r.getMensaje() : ""));
    }

    private static void assertFalse(String desc, boolean condicion) {
        reportar(desc, !condicion, condicion ? "Condicion es true, se esperaba false" : null);
    }

    private static void assertTrue(String desc, boolean condicion) {
        reportar(desc, condicion, condicion ? null : "Condicion es false");
    }

    private static void reportar(String desc, boolean paso, String detalle) {
        String estado = paso ? "[ OK ]" : "[FAIL]";
        System.out.printf("  %s %s%n", estado, desc.length() > 65 ? desc.substring(0,65)+"..." : desc);
        if (!paso && detalle != null) System.out.println("         >> " + detalle);
        if (paso) ok++; else fail++;
    }

    private static void borrarDir(java.io.File dir) {
        if (dir.isDirectory()) for (java.io.File f : dir.listFiles()) borrarDir(f);
        dir.delete();
    }
}
