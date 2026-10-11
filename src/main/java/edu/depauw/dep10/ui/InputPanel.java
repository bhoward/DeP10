package edu.depauw.dep10.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class InputPanel extends JPanel implements TabPanel {
    private static final long serialVersionUID = 6846573450653256646L;
    
    private final String name;
    private final JTextPane text;
    private final JCheckBox use;

    public InputPanel(String name) {
        this.name = name;

        text = new JTextPane();
        text.setEditable(true);
        text.setFont(SourcePanel.DEFAULT_FONT);
        text.setContentType("text/plain");

        use = new JCheckBox("Use as input");
        
        JScrollPane sp = new JScrollPane(text);
        
        this.setLayout(new BorderLayout());
        this.add(sp, BorderLayout.CENTER);
        this.add(use, BorderLayout.NORTH);
    }

    public String getTitle() {
        return name;
    }
    
    public boolean isActive() {
        return use.isSelected();
    }

    public void setContent(String content) {
        text.setText(content);
        text.setCaretPosition(0);
    }

    public String getContent() {
        return text.getText();
    }

    public void setPanelFont(Font font) {
        text.setFont(font);
    }
}
