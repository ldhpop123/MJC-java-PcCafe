package pccafe;

import pccafe.ui.AdminLoginFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminLoginFrame().display());
    }
}
