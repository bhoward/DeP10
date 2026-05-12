package edu.depauw.dep10.ui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JPanel;

import com.hermant.terminal.JTerminal;

@SuppressWarnings("serial")
public class TerminalPanel extends JPanel implements TabPanel {
    private final String name;
    private JTerminal terminal;

    public TerminalPanel(String name) {
        this.name = name;
        this.setFont(SourcePanel.DEFAULT_FONT);

        terminal = new JTerminal(false, true);
        terminal.setTerminalFont(this.getFont());

        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.add(terminal);
    }

    public String getTitle() {
        return name;
    }

    public InputStream getInputStream() {
        return terminal.getTis();
    }

    public OutputStream getOutputStream() {
        return terminal.getTos();
    }

    public void clear() {
        // Workaround a bug by just creating a new terminal...
        this.remove(terminal);
        terminal = new JTerminal(false, true);
        terminal.setTerminalFont(this.getFont());
        this.add(terminal);
        this.revalidate();
        this.repaint();
    }

    public void setPanelFont(Font font) {
        terminal.setTerminalFont(font);
    }
}
