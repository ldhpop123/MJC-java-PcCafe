package pccafe;

import pccafe.ui.AdminFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminFrame().display());
    }
}
