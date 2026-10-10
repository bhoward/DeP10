package edu.depauw.dep10.ui;

import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

public class RegisterField extends JComponent {
    private static final long serialVersionUID = 3514660556404348979L;
    
    private String label;
    private JTextField txt;

    public RegisterField() {
        this("", 2);
    }
    
    public RegisterField(String label, int bytes) {
        txt = new JTextField(bytes);
        
        txt.setEditable(false);
        txt.setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), label));
        setLayout(new FlowLayout());
        add(txt);
    }
    
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public int getBytes() {
        return txt.getColumns();
    }
    
    public void setBytes(int bytes) {
        txt.setColumns(bytes);
    }
    
    public void setFont(Font font) {
        txt.setFont(font);
    }
    
    public void setText(String text) {
        txt.setText(text);
    }
}
