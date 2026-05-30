package gui;

import motor.parser.Motor;
import motor.parser.ResultadoComando;

import javax.swing.*;
import java.awt.*;

class SearchPanel extends JPanel {
    private final Motor motor;
    private final JTextField espacio = new JTextField();
    private final JTextField campo = new JTextField("id");
    private final JTextField valor = new JTextField();
    private final JTextField desde = new JTextField();
    private final JTextField hasta = new JTextField();
    private final JComboBox<String> operador = new JComboBox<>(new String[]{"=", "!=", "<", "<=", ">", ">="});
    private final JTable resultados = new JTable();

    SearchPanel(Motor motor) {
        this.motor = motor;
        construir();
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Buscar registros"));
        form.add(new JLabel("Espacio"));
        form.add(espacio);
        form.add(new JLabel("Campo"));
        form.add(campo);
        form.add(new JLabel("Operador"));
        form.add(operador);
        form.add(new JLabel("Valor"));
        form.add(valor);
        form.add(new JLabel("Desde"));
        form.add(desde);
        form.add(new JLabel("Hasta"));
        form.add(hasta);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clave = new JButton("Buscar por clave primaria");
        JButton igualdad = new JButton("Buscar por condicion");
        JButton rango = new JButton("Buscar por rango");
        botones.add(clave);
        botones.add(igualdad);
        botones.add(rango);
        form.add(new JLabel(""));
        form.add(botones);

        clave.addActionListener(e -> buscarClave());
        igualdad.addActionListener(e -> buscarCondicion());
        rango.addActionListener(e -> buscarRango());

        add(form, BorderLayout.NORTH);
        add(GuiUtils.scroll(resultados), BorderLayout.CENTER);
    }

    private void buscarClave() {
        String pk = GuiUtils.clavePrimaria(motor, espacio.getText().trim());
        campo.setText(pk);
        buscar("SELECT * FROM " + espacio.getText().trim() + " WHERE " + pk + " = " + GuiUtils.literalLibre(valor.getText()));
    }

    private void buscarCondicion() {
        buscar("SELECT * FROM " + espacio.getText().trim() + " WHERE " + campo.getText().trim() + " " +
            operador.getSelectedItem() + " " + GuiUtils.literalLibre(valor.getText()));
    }

    private void buscarRango() {
        buscar("SELECT * FROM " + espacio.getText().trim() + " WHERE " + campo.getText().trim() +
            " BETWEEN " + GuiUtils.literalLibre(desde.getText()) + " AND " + GuiUtils.literalLibre(hasta.getText()));
    }

    private void buscar(String cmd) {
        ResultadoComando r = motor.ejecutar(cmd);
        if (r.isError()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo buscar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        GuiUtils.mostrarFilas(resultados, r.getFilas());
    }
}
