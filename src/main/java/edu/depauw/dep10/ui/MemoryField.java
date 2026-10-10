package edu.depauw.dep10.ui;

import java.awt.Font;
import java.awt.Toolkit;
import java.text.ParseException;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Util;
import edu.depauw.dep10.util.Word;

public class MemoryField extends JComponent {
    private static final long serialVersionUID = -2386326322494074091L;
    
    private static final int MEM_COLUMNS = 28;
    private static final int MEM_ROWS = 10;
    
    private State state;
    
    private JSpinner spnMem;
    private JTextArea txtMem;

    public MemoryField() {
        this.state = null;
        
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Memory"));
        var font = new Font("Monospaced", Font.PLAIN, 11);
        
        spnMem = new JSpinner();
        spnMem.setFont(new Font("Monospaced", Font.PLAIN, 11));
        spnMem.setModel(new SpinnerNumberModel(0, 0, 65535, 8));
        add(spnMem);
        spnMem.addChangeListener(e -> {
            this.refresh();
        });

        // see
        // https://github.com/aterai/java-swing-tips/blob/main/examples/HexFormatterSpinner/src/java/example/MainPanel.java
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spnMem.getEditor();
        JFormattedTextField ftf = editor.getTextField();
        ftf.setFormatterFactory(createFormatterFactory());

        txtMem = new JTextArea();
        txtMem.setColumns(MEM_COLUMNS);
        txtMem.setRows(MEM_ROWS);
        txtMem.setEditable(false);
        add(txtMem);
        txtMem.setFont(font);
    }
    
    private static DefaultFormatterFactory createFormatterFactory() {
        DefaultFormatter formatter = new DefaultFormatter() {
            @Override
            public Object stringToValue(String text) throws ParseException {
                Pattern pattern = Pattern.compile("^\\s*(\\p{XDigit}{1,4})\\s*$");
                if (pattern.matcher(text).find()) {
                    return Integer.valueOf(text, 16);
                }
                Toolkit.getDefaultToolkit().beep();
                throw new ParseException(text, 0);
            }

            @Override
            public String valueToString(Object value) {
                return Util.HEX_FORMAT.toHexDigits(((Integer) value).shortValue());
            }
        };
        formatter.setValueClass(Integer.class);
        formatter.setOverwriteMode(true);
        return new DefaultFormatterFactory(formatter);
    }

    public void attach(State state) {
        this.state = state;
        refresh();
    }
    
    public void refresh() {
        var memStart = ((Integer) spnMem.getValue()).intValue();
        var builder = new StringBuilder();
        for (int i = memStart; i < memStart + 8 * MEM_ROWS; i += 8) {
            var address = Word.of(i);
            builder.append(address);

            for (int col = 0; col < 8; col++) {
                var contents = state.mem1Safe(address.plus(col));
                builder.append(' ');
                builder.append(contents);
            }
            builder.append('\n');
        }
        txtMem.setText(builder.toString());
    }
    
    public void setFont(Font font) {
        spnMem.setFont(font);
        txtMem.setFont(font);
    }
}
