package gui;

import motor.almacenamiento.Registro;
import motor.parser.Motor;
import motor.parser.ResultadoComando;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class RecordPanel extends JPanel {
    private final Motor motor;
    private final JTextField espacio = new JTextField();
    private final DefaultTableModel valoresModel = new DefaultTableModel(new Object[]{"Campo", "Tipo", "Valor"}, 0);
    private final JTable valores = new JTable(valoresModel);
    private final JTable registros = new JTable();
    private java.util.List<FieldInfo> campos = new ArrayList<>();

    RecordPanel(Motor motor) {
        this.motor = motor;
        construir();
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.setBorder(BorderFactory.createTitledBorder("Seleccionar espacio"));
        top.add(new JLabel("Nombre del espacio"), BorderLayout.WEST);
        top.add(espacio, BorderLayout.CENTER);
        JButton cargar = new JButton("Cargar campos y registros");
        top.add(cargar, BorderLayout.EAST);
        cargar.addActionListener(e -> cargar());
        add(top, BorderLayout.NORTH);

        JPanel editor = new JPanel(new BorderLayout());
        editor.setBorder(BorderFactory.createTitledBorder("Valores del registro"));
        valores.setRowHeight(24);
        valores.setPreferredScrollableViewportSize(new Dimension(440, 220));
        valores.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        editor.add(GuiUtils.scroll(valores), BorderLayout.CENTER);
        JPanel botones = new JPanel(new GridLayout(0, 1, 4, 4));
        JButton insertar = new JButton("Insertar registro");
        JButton actualizar = new JButton("Actualizar seleccionado");
        JButton eliminar = new JButton("Eliminar seleccionado");
        botones.add(insertar);
        botones.add(actualizar);
        botones.add(eliminar);
        editor.add(botones, BorderLayout.SOUTH);
        insertar.addActionListener(e -> insertar());
        actualizar.addActionListener(e -> actualizar());
        eliminar.addActionListener(e -> eliminar());

        JPanel tabla = new JPanel(new BorderLayout());
        tabla.setBorder(BorderFactory.createTitledBorder("Registros"));
        JButton refrescar = new JButton("Refrescar");
        tabla.add(refrescar, BorderLayout.NORTH);
        tabla.add(GuiUtils.scroll(registros), BorderLayout.CENTER);
        refrescar.addActionListener(e -> refrescar());

        registros.getSelectionModel().addListSelectionListener(e -> cargarSeleccionado());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editor, tabla);
        split.setDividerLocation(460);
        add(split, BorderLayout.CENTER);
    }

    private void cargar() {
        campos = GuiUtils.cargarCampos(motor, espacio.getText().trim());
        valoresModel.setRowCount(0);
        if (campos.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No se pudieron cargar campos. Si es un espacio libre, usa INSERT desde la consola o crea un esquema fijo.",
                "Campos no disponibles", JOptionPane.WARNING_MESSAGE);
        }
        for (FieldInfo f : campos) valoresModel.addRow(new Object[]{f.nombre, f.tipo, ""});
        if (!campos.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Se cargaron " + campos.size() + " campo(s). Ahora escribe los valores y presiona Insertar registro.",
                "Campos cargados", JOptionPane.INFORMATION_MESSAGE);
        }
        refrescar();
    }

    private void refrescar() {
        String nom = espacio.getText().trim();
        if (nom.isEmpty()) return;
        ResultadoComando r = motor.ejecutar("SELECT * FROM " + nom);
        if (r.isError()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo consultar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        GuiUtils.mostrarFilas(registros, r.getFilas());
    }

    private void insertar() {
        confirmarEdicion();
        if (!validarCampos()) return;
        String cmd = "INSERT INTO " + espacio.getText().trim() + " (" + nombresCampos() + ") VALUES (" + valoresCampos() + ")";
        ResultadoComando r = GuiUtils.ejecutar(this, motor, cmd);
        if (!r.isError()) refrescar();
    }

    private void actualizar() {
        confirmarEdicion();
        int row = registros.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un registro de la tabla.", "Seleccion requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!validarCampos()) return;
        String pk = GuiUtils.clavePrimaria(motor, espacio.getText().trim());
        int pkCol = columna(pk);
        if (pkCol < 0) {
            JOptionPane.showMessageDialog(this, "No se encontro la clave primaria en la tabla.", "Clave no encontrada", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object pkValue = registros.getValueAt(row, pkCol);
        StringBuilder set = new StringBuilder();
        for (int i = 0; i < valoresModel.getRowCount(); i++) {
            String campo = String.valueOf(valoresModel.getValueAt(i, 0));
            if (campo.equalsIgnoreCase(pk)) continue;
            String tipo = String.valueOf(valoresModel.getValueAt(i, 1));
            Object valor = valoresModel.getValueAt(i, 2);
            if (set.length() > 0) set.append(", ");
            set.append(campo).append("=").append(GuiUtils.literal(tipo, valor));
        }
        String cmd = "UPDATE " + espacio.getText().trim() + " SET " + set +
            " WHERE " + pk + " = " + GuiUtils.literal(tipoDe(pk), pkValue);
        ResultadoComando r = GuiUtils.ejecutar(this, motor, cmd);
        if (!r.isError()) refrescar();
    }

    private void eliminar() {
        int row = registros.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un registro para eliminar.", "Seleccion requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String pk = GuiUtils.clavePrimaria(motor, espacio.getText().trim());
        int pkCol = columna(pk);
        if (pkCol < 0) return;
        Object pkValue = registros.getValueAt(row, pkCol);
        int ok = JOptionPane.showConfirmDialog(this,
            "Eliminar el registro con " + pk + " = " + pkValue + "?",
            "Confirmar eliminacion", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            ResultadoComando r = GuiUtils.ejecutar(this, motor,
                "DELETE FROM " + espacio.getText().trim() + " WHERE " + pk + " = " + GuiUtils.literal(tipoDe(pk), pkValue));
            if (!r.isError()) refrescar();
        }
    }

    private void cargarSeleccionado() {
        int row = registros.getSelectedRow();
        if (row < 0 || row >= registros.getRowCount()) return;
        for (int i = 0; i < valoresModel.getRowCount(); i++) {
            String campo = String.valueOf(valoresModel.getValueAt(i, 0));
            int col = columna(campo);
            if (col >= 0) valoresModel.setValueAt(registros.getValueAt(row, col), i, 2);
        }
    }

    private boolean validarCampos() {
        if (espacio.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe y carga un espacio.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (valoresModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Carga los campos antes de guardar.", "Campos requeridos", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void confirmarEdicion() {
        if (valores.isEditing()) {
            valores.getCellEditor().stopCellEditing();
        }
    }

    private String nombresCampos() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valoresModel.getRowCount(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(valoresModel.getValueAt(i, 0));
        }
        return sb.toString();
    }

    private String valoresCampos() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valoresModel.getRowCount(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(GuiUtils.literal(String.valueOf(valoresModel.getValueAt(i, 1)), valoresModel.getValueAt(i, 2)));
        }
        return sb.toString();
    }

    private int columna(String nombre) {
        for (int i = 0; i < registros.getColumnCount(); i++) {
            if (registros.getColumnName(i).equalsIgnoreCase(nombre)) return i;
        }
        return -1;
    }

    private String tipoDe(String campo) {
        for (FieldInfo f : campos) if (f.nombre.equalsIgnoreCase(campo)) return f.tipo;
        return "TEXTO";
    }
}
