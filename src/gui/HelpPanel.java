package gui;

import motor.parser.Motor;
import motor.parser.ResultadoComando;

import javax.swing.*;
import java.awt.*;

class HelpPanel extends JPanel {
    private final JTextArea ayuda = new JTextArea();

    HelpPanel(Motor motor) {
        setLayout(new BorderLayout());
        ayuda.setEditable(false);
        ayuda.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        ResultadoComando r = motor.ejecutar("MENU");
        String texto = r.getMensaje() == null ? "" : r.getMensaje();
        ayuda.setText(texto +
            "\n\nGUI DISPONIBLE\n" +
            "============================================================\n" +
            "Espacios: crear, listar y eliminar tablas o espacios.\n" +
            "Registros: insertar, ver, actualizar y eliminar registros.\n" +
            "Busquedas: consultar por clave, igualdad o rango.\n" +
            "Indice / Arbol: mostrar claves ordenadas, altura y arbol AVL.\n" +
            "Ayuda: esta guia.\n\n" +
            "La GUI usa el mismo motor que la consola, por eso no duplica la logica del gestor.");
        add(GuiUtils.scroll(ayuda), BorderLayout.CENTER);
    }
}
