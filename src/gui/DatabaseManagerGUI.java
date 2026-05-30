package gui;

import motor.almacenamiento.Registro;
import motor.parser.Motor;
import motor.parser.ResultadoComando;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Interfaz grafica principal. Es una capa visual sobre Motor:
 * no manipula archivos ni nodos del AVL directamente.
 */
public class DatabaseManagerGUI extends JFrame {

    private final Motor motor;

    public DatabaseManagerGUI(String dataDir) {
        super("MotorBD - Gestor de datos");
        this.motor = new Motor(dataDir);
        construir();
    }

    private void construir() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 720);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Espacios", new SpacePanel(motor));
        tabs.addTab("Registros", new RecordPanel(motor));
        tabs.addTab("Busquedas", new SearchPanel(motor));
        tabs.addTab("Indice / Arbol", new TreePanel(motor));
        tabs.addTab("Ayuda", new HelpPanel(motor));
        setContentPane(tabs);
    }

    public static void main(String[] args) {
        String dataDir = "data";
        for (int i = 0; i < args.length; i++) {
            if ("--data".equals(args[i]) && i + 1 < args.length) dataDir = args[i + 1];
        }
        final String finalDataDir = dataDir;
        SwingUtilities.invokeLater(() -> new DatabaseManagerGUI(finalDataDir).setVisible(true));
    }
}

class GuiUtils {
    static final String[] TIPOS = {"ENTERO", "TEXTO", "REAL", "BOOLEAN"};

    static ResultadoComando ejecutar(Component parent, Motor motor, String comando) {
        ResultadoComando r = motor.ejecutar(comando);
        if (r.isError()) {
            JOptionPane.showMessageDialog(parent, r.getMensaje(), "No se pudo completar", JOptionPane.ERROR_MESSAGE);
        } else if (r.getMensaje() != null && !r.getMensaje().trim().isEmpty()) {
            JOptionPane.showMessageDialog(parent, r.getMensaje(), "Operacion completada", JOptionPane.INFORMATION_MESSAGE);
        }
        return r;
    }

    static void mostrarFilas(JTable tabla, List<Registro> filas) {
        DefaultTableModel model = new DefaultTableModel();
        LinkedHashSet<String> columnas = new LinkedHashSet<>();
        for (Registro r : filas) columnas.addAll(r.getCampos().keySet());
        for (String c : columnas) model.addColumn(c);
        for (Registro r : filas) {
            Object[] row = new Object[columnas.size()];
            int i = 0;
            for (String c : columnas) row[i++] = r.get(c);
            model.addRow(row);
        }
        tabla.setModel(model);
    }

    static java.util.List<FieldInfo> cargarCampos(Motor motor, String espacio) {
        ResultadoComando r = motor.ejecutar("DESC " + espacio);
        java.util.List<FieldInfo> campos = new ArrayList<>();
        if (r.isError() || r.getMensaje() == null) return campos;
        boolean enCampos = false;
        for (String line : r.getMensaje().split("\\R")) {
            String t = line.trim();
            if (t.equals("Campos:")) {
                enCampos = true;
                continue;
            }
            if (!enCampos || t.isEmpty() || t.startsWith("NOMBRE") || t.startsWith("-")) continue;
            String[] parts = t.split("\\s+");
            if (parts.length >= 2) {
                boolean pk = parts.length >= 3 && parts[2].equalsIgnoreCase("SI");
                campos.add(new FieldInfo(parts[0], parts[1], pk));
            }
        }
        return campos;
    }

    static String clavePrimaria(Motor motor, String espacio) {
        for (FieldInfo f : cargarCampos(motor, espacio)) if (f.pk) return f.nombre;
        ResultadoComando r = motor.ejecutar("DESC " + espacio);
        if (!r.isError() && r.getMensaje() != null) {
            for (String line : r.getMensaje().split("\\R")) {
                if (line.trim().startsWith("Clave:")) return line.substring(line.indexOf(':') + 1).trim();
            }
        }
        return "id";
    }

    static String literal(String tipo, Object valor) {
        String s = valor == null ? "" : valor.toString().trim();
        if (s.isEmpty()) return "null";
        if ("TEXTO".equalsIgnoreCase(tipo)) return "'" + s.replace("'", "\\'") + "'";
        return s;
    }

    static String literalLibre(String valor) {
        String s = valor == null ? "" : valor.trim();
        if (s.matches("-?\\d+(\\.\\d+)?") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) return s;
        return "'" + s.replace("'", "\\'") + "'";
    }

    static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        return sp;
    }
}

class FieldInfo {
    final String nombre;
    final String tipo;
    final boolean pk;

    FieldInfo(String nombre, String tipo, boolean pk) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.pk = pk;
    }
}
