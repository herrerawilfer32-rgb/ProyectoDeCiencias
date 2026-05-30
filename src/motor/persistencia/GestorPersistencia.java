package motor.persistencia;

import motor.almacenamiento.Espacio;
import motor.almacenamiento.GestorEspacios;
import motor.almacenamiento.Registro;
import motor.catalogo.Campo;
import motor.catalogo.Catalogo;
import motor.catalogo.Esquema;
import motor.catalogo.TipoDato;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Gestiona la persistencia del motor en disco.
 * Formato:
 *   data/esquemas/<espacio>.schema  → definicion del esquema (JSON simple)
 *   data/espacios/<espacio>.json    → registros en JSON Lines
 *
 * @author Anyelo Esteban Casas Zapata (20251020106)
 * @author Diego Alejandro Yanez Zabala (20210103)
 * @author Wilfer Arbey Herrera Garzon (20251020071)
 */
public class GestorPersistencia {

    private String dirEsquemas;
    private String dirEspacios;

    public GestorPersistencia(String baseDir) {
        this.dirEsquemas = baseDir + "/esquemas";
        this.dirEspacios = baseDir + "/espacios";
        new File(dirEsquemas).mkdirs();
        new File(dirEspacios).mkdirs();
    }

    // ── GUARDAR ───────────────────────────────────────────

    /** Persiste el esquema y todos los registros de un espacio. */
    public void guardar(Espacio espacio) {
        guardarEsquema(espacio.getEsquema());
        guardarRegistros(espacio);
    }

    private void guardarEsquema(Esquema e) {
        File f = new File(dirEsquemas, e.getNombreEspacio() + ".schema");
        File tmp = new File(dirEsquemas, e.getNombreEspacio() + ".schema.tmp");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(tmp, false))) {
            w.write("{\"nombre\":\"" + e.getNombreEspacio() + "\"");
            w.write(",\"libre\":" + e.isEsquemaLibre());
            w.write(",\"clave\":\"" + e.getCampoClave() + "\"");
            w.write(",\"campos\":[");
            List<Campo> campos = e.getCampos();
            for (int i = 0; i < campos.size(); i++) {
                Campo c = campos.get(i);
                if (i > 0) w.write(",");
                w.write("{\"n\":\"" + c.getNombre() + "\",\"t\":\"" + c.getTipo() + "\",\"pk\":" + c.isClavePrimaria() + "}");
            }
            w.write("]}");
            w.newLine();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el esquema de '" + e.getNombreEspacio() + "': " + ex.getMessage(), ex);
        }
        try {
            reemplazarAtomico(tmp, f);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo reemplazar el esquema de '" + e.getNombreEspacio() + "': " + ex.getMessage(), ex);
        }
    }

    private void guardarRegistros(Espacio espacio) {
        File f = new File(dirEspacios, espacio.getEsquema().getNombreEspacio() + ".json");
        File tmp = new File(dirEspacios, espacio.getEsquema().getNombreEspacio() + ".json.tmp");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(tmp, false))) {
            for (Registro r : espacio.todos()) {
                w.write(r.toJson());
                w.newLine();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron guardar los registros de '" +
                espacio.getEsquema().getNombreEspacio() + "': " + ex.getMessage(), ex);
        }
        try {
            reemplazarAtomico(tmp, f);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo reemplazar el archivo de registros de '" +
                espacio.getEsquema().getNombreEspacio() + "': " + ex.getMessage(), ex);
        }
    }

    private void reemplazarAtomico(File tmp, File destino) throws IOException {
        try {
            Files.move(tmp.toPath(), destino.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ── CARGAR ────────────────────────────────────────────

    /** Carga todos los espacios que existen en disco. */
    public void cargarTodo(GestorEspacios gestor, Catalogo catalogo) {
        File dirE = new File(dirEsquemas);
        File[] schemas = dirE.listFiles((d, n) -> n.endsWith(".schema"));
        if (schemas == null) return;
        for (File sf : schemas) {
            try {
                Esquema esquema = cargarEsquema(sf);
                if (esquema == null) continue;
                Espacio espacio = new Espacio(esquema);
                cargarRegistros(espacio);
                gestor.cargar(espacio);
                catalogo.registrar(esquema);
            } catch (Exception ex) {
                System.err.println("[Persistencia] Error cargando " + sf.getName() + ": " + ex.getMessage());
            }
        }
    }

    private Esquema cargarEsquema(File f) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(f));
        String linea = r.readLine(); r.close();
        if (linea == null || linea.trim().isEmpty()) return null;

        String nombre = extraer(linea, "nombre");
        boolean libre = "true".equals(extraer(linea, "libre"));
        String clave  = extraer(linea, "clave");

        if (libre) return new Esquema(nombre);

        List<Campo> campos = new ArrayList<>();
        int camposIdx = linea.indexOf("\"campos\":[");
        if (camposIdx != -1) {
            String camposStr = linea.substring(camposIdx + 10);
            String[] partes = camposStr.split("\\},\\{");
            for (String p : partes) {
                p = p.replace("[","").replace("]","").replace("{","").replace("}","").trim();
                String n2 = extraerSimple(p, "n");
                String t  = extraerSimple(p, "t");
                String pk = extraerSimple(p, "pk");
                if (n2 != null && t != null) {
                    campos.add(new Campo(n2, TipoDato.valueOf(t), "true".equals(pk)));
                }
            }
        }
        return new Esquema(nombre, campos);
    }

    private void cargarRegistros(Espacio espacio) {
        File f = new File(dirEspacios, espacio.getEsquema().getNombreEspacio() + ".json");
        if (!f.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = r.readLine()) != null) {
                linea = linea.trim();
                if (!linea.isEmpty()) {
                    try {
                        Registro reg = Registro.fromJson(linea);
                        espacio.insertar(reg);
                    } catch (Exception ex) {
                        System.err.println("[Persistencia] Registro omitido: " + ex.getMessage());
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("[Persistencia] Error leyendo registros: " + ex.getMessage());
        }
    }

    /** Elimina archivos de un espacio del disco. */
    public void eliminar(String nombre) {
        new File(dirEsquemas, nombre + ".schema").delete();
        new File(dirEspacios, nombre + ".json").delete();
    }

    // ── UTILIDADES JSON simple ────────────────────────────

    private String extraer(String json, String clave) {
        String key = "\"" + clave + "\":";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        if (start >= json.length()) return null;
        char c = json.charAt(start);
        if (c == '"') {
            int end = json.indexOf('"', start + 1);
            return end == -1 ? null : json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    private String extraerSimple(String s, String clave) {
        String key = "\"" + clave + "\":";
        int idx = s.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        if (start >= s.length()) return null;
        char c = s.charAt(start);
        if (c == '"') {
            int end = s.indexOf('"', start + 1);
            return end == -1 ? null : s.substring(start + 1, end);
        } else {
            int end = start;
            while (end < s.length() && s.charAt(end) != ',' && s.charAt(end) != '}') end++;
            return s.substring(start, end).trim();
        }
    }
}
