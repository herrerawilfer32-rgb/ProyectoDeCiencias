package gui;

import motor.almacenamiento.Registro;
import motor.parser.Motor;
import motor.parser.ResultadoComando;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class TreePanel extends JPanel {
    private final Motor motor;
    private final JTextField espacio = new JTextField();
    private final JTextField clave = new JTextField();
    private final JTextArea salida = new JTextArea();

    TreePanel(Motor motor) {
        this.motor = motor;
        construir();
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Indice primario AVL"));
        form.add(new JLabel("Espacio"));
        form.add(espacio);
        form.add(new JLabel("Clave a buscar"));
        form.add(clave);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton mostrar = new JButton("Mostrar arbol");
        JButton inorder = new JButton("Mostrar claves en orden");
        JButton buscar = new JButton("Buscar clave");
        botones.add(mostrar);
        botones.add(inorder);
        botones.add(buscar);
        form.add(new JLabel(""));
        form.add(botones);

        mostrar.addActionListener(e -> mostrarArbol());
        inorder.addActionListener(e -> mostrarInorder());
        buscar.addActionListener(e -> buscarClave());

        salida.setEditable(false);
        salida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(form, BorderLayout.NORTH);
        add(GuiUtils.scroll(salida), BorderLayout.CENTER);
    }

    private void mostrarArbol() {
        ResultadoComando tree = motor.ejecutar("TREE " + espacio.getText().trim());
        ResultadoComando desc = motor.ejecutar("DESC " + espacio.getText().trim());
        if (tree.isError()) {
            JOptionPane.showMessageDialog(this, tree.getMensaje(), "No se pudo mostrar el arbol", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String altura = "";
        if (!desc.isError() && desc.getMensaje() != null) {
            for (String line : desc.getMensaje().split("\\R")) {
                if (line.trim().startsWith("Altura AVL:")) altura = line.trim();
            }
        }
        salida.setText("Propiedades del indice\n" + altura + "\n\n" + tree.getMensaje());
    }

    private void mostrarInorder() {
        ResultadoComando r = motor.ejecutar("SELECT * FROM " + espacio.getText().trim());
        if (r.isError()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo consultar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String pk = GuiUtils.clavePrimaria(motor, espacio.getText().trim());
        List<String> claves = new ArrayList<>();
        for (Registro reg : r.getFilas()) {
            Object v = reg.get(pk);
            if (v != null) claves.add(v.toString());
        }
        salida.setText("Recorrido inorder del indice primario:\n" +
            String.join(", ", claves) + "\n\nCampo clave: " + pk +
            "\nTotal de claves: " + claves.size());
    }

    private void buscarClave() {
        String pk = GuiUtils.clavePrimaria(motor, espacio.getText().trim());
        ResultadoComando r = motor.ejecutar("SELECT * FROM " + espacio.getText().trim() +
            " WHERE " + pk + " = " + GuiUtils.literalLibre(clave.getText()));
        if (r.isError()) {
            JOptionPane.showMessageDialog(this, r.getMensaje(), "No se pudo buscar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        salida.setText("Busqueda en el indice primario\nCampo clave: " + pk +
            "\nClave buscada: " + clave.getText().trim() +
            "\nResultado: " + r.getFilas().size() + " registro(s)\n\n" + r.getFilas());
    }
}
