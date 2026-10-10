package edu.depauw.dep10.ui;

import java.awt.Font;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import edu.depauw.dep10.simulator.State;

public class StackField extends JComponent {
    private static final long serialVersionUID = 5135001881339433854L;
    
    private static final int STACK_COLUMNS = 10;
    private static final int STACK_ROWS = 10;
    
    private State state;
    
    private JSpinner spnStack;
    private JTextArea txtStack;

    public StackField() {
        this.state = null;
        
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Stack"));
        var font = new Font("Monospaced", Font.PLAIN, 11);
        
        spnStack = new JSpinner();
        spnStack.setModel(new SpinnerNumberModel(0, -1000, 1000, 2)); // TODO do this better?
        add(spnStack);
        spnStack.setFont(font);
        spnStack.addChangeListener(e -> {
            this.refresh();
        });

        txtStack = new JTextArea();
        txtStack.setColumns(STACK_COLUMNS);
        txtStack.setRows(STACK_ROWS);
        txtStack.setEditable(false);
        add(txtStack);
        txtStack.setFont(font);
    }
    
    public void attach(State state) {
        this.state = state;
        refresh();
    }
    
    public void refresh() {
        var stackOffset = ((Integer) spnStack.getValue()).intValue();
        var builder = new StringBuilder();
        for (int i = stackOffset; i < stackOffset + 2 * STACK_ROWS; i += 2) {
            var address = state.getSP().plus(i);
            var contents = state.mem2Safe(address);

            builder.append(i == 0 ? '>' : ' ');
            builder.append(address);
            builder.append(' ');
            builder.append(contents);
            builder.append('\n');
        }
        txtStack.setText(builder.toString());
    }
    
    public void setFont(Font font) {
        spnStack.setFont(font);
        txtStack.setFont(font);
    }
}
