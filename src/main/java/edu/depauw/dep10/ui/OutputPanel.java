package edu.depauw.dep10.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;

@SuppressWarnings("serial")
class OutputPanel extends JPanel {
    private final String name;
    private final JTextPane text;

    public OutputPanel(String name) {
        this.name = name;

        text = new JTextPane();
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        text.setContentType("text/plain");
        
        JScrollPane sp = new JScrollPane(text);
        this.setLayout(new BorderLayout());
        this.add(sp);
    }

	public String getTitle() {
		return name;
	}

    public void setContent(String content) {
        text.setText(content);
    }
    
    public void setDocument(Document document) {
        text.setDocument(document);
    }

	public String getContent() {
		return text.getText();
	}
}