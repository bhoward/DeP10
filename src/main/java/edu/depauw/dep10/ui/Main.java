package edu.depauw.dep10.ui;

import java.awt.Desktop;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;

public class Main {
    public static void main(String[] args) {
        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.awt.application.name", MainFrame.APP_TITLE);
        }

        FlatLightLaf.setup();
        
        var frame = new MainFrame();

        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(e -> {
                frame.showHelpDialog("aboutdep10.html");
            });
        }
        if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
            desktop.setPreferencesHandler(e -> {
                // TODO show preferences dialog
            });
        }
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((e, response) -> {
                if (frame.canQuit())
                    response.performQuit();
                else
                    response.cancelQuit();
            });
        }

        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }
}
