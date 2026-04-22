package edu.depauw.dep10.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

@SuppressWarnings("serial")
class OutputPanel extends JPanel {
    private final String name;
    private final JTextArea textArea;

    public OutputPanel(String name) {
        this.name = name;

        textArea = new JTextArea(25, 80);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane sp = new JScrollPane(textArea);
        this.setLayout(new BorderLayout());
        this.add(sp);
    }

	public String getTitle() {
		return name;
	}

    public void setContent(String content) {
        textArea.setText(content);
    }

	public String getContent() {
		return textArea.getText();
	}
}