package edu.depauw.dep10.ui;

import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JPanel;

import com.hermant.terminal.JTerminal;

@SuppressWarnings("serial")
public class TerminalPanel extends JPanel {
    private final String name;
    private final JTerminal terminal;

    public TerminalPanel(String name) {
        this.name = name;

        terminal = new JTerminal();
        
//        JScrollPane sp = new JScrollPane(terminal);
//        this.setLayout(new BorderLayout());
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
}
