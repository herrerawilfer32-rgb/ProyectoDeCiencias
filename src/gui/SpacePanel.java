package gui;

import motor.parser.Motor;
import motor.parser.ResultadoComando;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

class SpacePanel extends JPanel {
    private final Motor motor;
    private final JTextField nombre = new JTextField();
    private final DefaultTableModel camposModel = new DefaultTableModel(new Object[]{"Campo", "Tipo", "PK"}, 0) {
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 2 ? Boolean.class : String.class;
        }
    };
    private final JTable campos = new JTable(camposModel);
    private final JTextField eliminarNombre = new JTextField();
    private final JTextArea espacios = new JTextArea();

    SpacePanel(Motor motor) {
        this.motor = motor;
        construir();
        refrescar();
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));
        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Crear tabla o espacio"));

        JPanel arriba = new JPanel(new GridLayout(2, 1, 4, 4));
        arriba.add(new JLabel("Nombre del espacio"));
        arriba.add(nombre);
        form.add(arriba, BorderLayout.NORTH);

        campos.setRowHeight(24);
        campos.setPreferredScrollableViewportSize(new Dimension(520, 190));
        form.add(GuiUtils.scroll(campos), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Agregar campo");
        JButton crear = new JButton("Crear espacio");
        JButton libre = new JButton("Crear espacio libre");
        botones.add(add);
        botones.add(crear);
        botones.add(libre);
        form.add(botones, BorderLayout.SOUTH);

        add.addActionListener(e -> camposModel.addRow(new Object[]{"", "TEXTO", false}));
        crear.addActionListener(e -> crearEspacio());
        libre.addActionListener(e -> crearLibre());

        JComboBox<String> editor = new JComboBox<>(GuiUtils.TIPOS);
        campos.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(editor));

        JPanel eliminar = new JPanel(new BorderLayout(6, 6));
        eliminar.setBorder(BorderFactory.createTitledBorder("Eliminar espacio"));
        eliminar.add(new JLabel("Nombre del espacio a eliminar"), BorderLayout.NORTH);
        eliminar.add(eliminarNombre, BorderLayout.CENTER);
        JButton btnEliminar = new JButton("Eliminar espacio");
        eliminar.add(btnEliminar, BorderLayout.SOUTH);
        btnEliminar.addActionListener(e -> eliminarEspacio());

        espacios.setEditable(false);
        espacios.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JPanel lista = new JPanel(new BorderLayout());
        lista.setBorder(BorderFactory.createTitledBorder("Espacios existentes"));
        JButton ref = new JButton("Refrescar lista");
        lista.add(ref, BorderLayout.NORTH);
        lista.add(GuiUtils.scroll(espacios), BorderLayout.CENTER);
        ref.addActionListener(e -> refrescar());

        JPanel left = new JPanel(new GridLayout(2, 1, 8, 8));
        left.setPreferredSize(new Dimension(560, 0));
        left.add(form);
        left.add(eliminar);
        add(left, BorderLayout.WEST);
        add(lista, BorderLayout.CENTER);
    }

    private void crearEspacio() {
        String nom = nombre.getText().trim();
        if (nom.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe el nombre del espacio.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (camposModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Agrega al menos un campo o usa espacio libre.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Set<String> nombres = new HashSet<>();
        int pkCount = 0;
        StringBuilder def = new StringBuilder();
        for (int i = 0; i < camposModel.getRowCount(); i++) {
            String campo = String.valueOf(camposModel.getValueAt(i, 0)).trim().toLowerCase();
            String tipo = String.valueOf(camposModel.getValueAt(i, 1)).trim().toUpperCase();
            boolean pk = Boolean.TRUE.equals(camposModel.getValueAt(i, 2));
            if (campo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Hay un campo sin nombre.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!nombres.add(campo)) {
                JOptionPane.showMessageDialog(this, "El campo '" + campo + "' esta repetido.", "Campo repetido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (pk) pkCount++;
            if (def.length() > 0) def.append(", ");
            def.append(campo).append(" ").append(tipo);
            if (pk) def.append(" PK");
        }
        if (pkCount != 1) {
            JOptionPane.showMessageDialog(this, "Marca exactamente un campo como clave primaria.", "Clave primaria requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        GuiUtils.ejecutar(this, motor, "CREATE SPACE " + nom + " (" + def + ")");
        refrescar();
    }

    private void crearLibre() {
        String nom = nombre.getText().trim();
        if (nom.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe el nombre del espacio.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        GuiUtils.ejecutar(this, motor, "CREATE SPACE " + nom);
        refrescar();
    }

    private void eliminarEspacio() {
        String nom = eliminarNombre.getText().trim();
        if (nom.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe el nombre del espacio a eliminar.", "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
            "Esta seguro de eliminar el espacio '" + nom + "'? Esta accion borrara sus registros.",
            "Confirmar eliminacion", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            GuiUtils.ejecutar(this, motor, "DROP SPACE " + nom);
            refrescar();
        }
    }

    private void refrescar() {
        ResultadoComando r = motor.ejecutar("SHOW SPACES");
        espacios.setText(r.getMensaje() == null ? "" : r.getMensaje());
    }
}
