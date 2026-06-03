package edu.depauw.dep10.ui;

import java.awt.Font;
import java.awt.Toolkit;
import java.text.ParseException;
import java.util.regex.Pattern;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

import edu.depauw.dep10.simulator.State;

public class StatePanel extends JPanel implements TabPanel {
    private static final long serialVersionUID = 1L;
    private static final int WORD_COLUMNS = 4;
    private static final int BYTE_COLUMNS = 2;
    private static final int FONT_SIZE = 11;

    private JTextField txtA;
    private JTextField txtX;
    private JTextField txtPC;
    private JTextField txtSP;
    private JTextField txtNZVC;
    private JTextField txtPX;
    private JTextField txtIR1;
    private JTextField txtIR2;
    private JTextField txtEA;

    private State state;
    private Font font;

    /**
     * Create the panel.
     */
    public StatePanel() {
        this.state = null;
        this.font = new Font(Font.MONOSPACED, Font.PLAIN, FONT_SIZE);
        
        SpringLayout springLayout = new SpringLayout();
        setLayout(springLayout);

        JLabel lblA = new JLabel("A");
        springLayout.putConstraint(SpringLayout.NORTH, lblA, 10, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, lblA, 20, SpringLayout.WEST, this);
        add(lblA);

        txtA = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtA, -5, SpringLayout.NORTH, lblA);
        springLayout.putConstraint(SpringLayout.WEST, txtA, 10, SpringLayout.EAST, lblA);
        add(txtA);
        txtA.setEditable(false);
        txtA.setColumns(WORD_COLUMNS);
        txtA.setFont(font);

        JLabel lblX = new JLabel("X");
        springLayout.putConstraint(SpringLayout.NORTH, lblX, 15, SpringLayout.SOUTH, lblA);
        springLayout.putConstraint(SpringLayout.EAST, lblX, 0, SpringLayout.EAST, lblA);
        add(lblX);

        txtX = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtX, -5, SpringLayout.NORTH, lblX);
        springLayout.putConstraint(SpringLayout.WEST, txtX, 0, SpringLayout.WEST, txtA);
        add(txtX);
        txtX.setEditable(false);
        txtX.setColumns(WORD_COLUMNS);
        txtX.setFont(font);

        JLabel lblPC = new JLabel("PC");
        springLayout.putConstraint(SpringLayout.NORTH, lblPC, 15, SpringLayout.SOUTH, lblX);
        springLayout.putConstraint(SpringLayout.EAST, lblPC, 0, SpringLayout.EAST, lblA);
        add(lblPC);

        txtPC = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtPC, -5, SpringLayout.NORTH, lblPC);
        springLayout.putConstraint(SpringLayout.WEST, txtPC, 0, SpringLayout.WEST, txtA);
        add(txtPC);
        txtPC.setEditable(false);
        txtPC.setColumns(WORD_COLUMNS);
        txtPC.setFont(font);

        JLabel lblSP = new JLabel("SP");
        springLayout.putConstraint(SpringLayout.NORTH, lblSP, 15, SpringLayout.SOUTH, lblPC);
        springLayout.putConstraint(SpringLayout.EAST, lblSP, 0, SpringLayout.EAST, lblA);
        add(lblSP);

        txtSP = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtSP, -5, SpringLayout.NORTH, lblSP);
        springLayout.putConstraint(SpringLayout.WEST, txtSP, 0, SpringLayout.WEST, txtA);
        add(txtSP);
        txtSP.setEditable(false);
        txtSP.setColumns(WORD_COLUMNS);
        txtSP.setFont(font);

        JLabel lblNZVC = new JLabel("NZVC");
        springLayout.putConstraint(SpringLayout.NORTH, lblNZVC, 0, SpringLayout.NORTH, lblA);
        springLayout.putConstraint(SpringLayout.WEST, lblNZVC, 20, SpringLayout.EAST, txtA);
        add(lblNZVC);

        txtNZVC = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtNZVC, 0, SpringLayout.NORTH, txtA);
        springLayout.putConstraint(SpringLayout.WEST, txtNZVC, 10, SpringLayout.EAST, lblNZVC);
        txtNZVC.setEditable(false);
        add(txtNZVC);
        txtNZVC.setColumns(WORD_COLUMNS);
        txtNZVC.setFont(font);

        JLabel lblPX = new JLabel("PX");
        springLayout.putConstraint(SpringLayout.NORTH, lblPX, 0, SpringLayout.NORTH, lblX);
        springLayout.putConstraint(SpringLayout.EAST, lblPX, 0, SpringLayout.EAST, lblNZVC);
        add(lblPX);

        txtPX = new JTextField();
        txtPX.setEditable(false);
        springLayout.putConstraint(SpringLayout.NORTH, txtPX, 0, SpringLayout.NORTH, txtX);
        springLayout.putConstraint(SpringLayout.WEST, txtPX, 10, SpringLayout.EAST, lblPX);
        add(txtPX);
        txtPX.setColumns(BYTE_COLUMNS);
        txtPX.setFont(font);

        JLabel lblIR1 = new JLabel("IR1");
        springLayout.putConstraint(SpringLayout.NORTH, lblIR1, 0, SpringLayout.NORTH, lblPX);
        springLayout.putConstraint(SpringLayout.WEST, lblIR1, 10, SpringLayout.EAST, txtPX);
        add(lblIR1);

        txtIR1 = new JTextField();
        txtIR1.setEditable(false);
        springLayout.putConstraint(SpringLayout.NORTH, txtIR1, 0, SpringLayout.NORTH, txtPX);
        springLayout.putConstraint(SpringLayout.WEST, txtIR1, 10, SpringLayout.EAST, lblIR1);
        add(txtIR1);
        txtIR1.setColumns(BYTE_COLUMNS);
        txtIR1.setFont(font);

        JLabel lblIR2 = new JLabel("IR2");
        springLayout.putConstraint(SpringLayout.NORTH, lblIR2, 0, SpringLayout.NORTH, lblPC);
        springLayout.putConstraint(SpringLayout.EAST, lblIR2, 0, SpringLayout.EAST, lblNZVC);
        add(lblIR2);

        txtIR2 = new JTextField();
        txtIR2.setEditable(false);
        springLayout.putConstraint(SpringLayout.NORTH, txtIR2, 0, SpringLayout.NORTH, txtPC);
        springLayout.putConstraint(SpringLayout.WEST, txtIR2, 10, SpringLayout.EAST, lblIR2);
        add(txtIR2);
        txtIR2.setColumns(WORD_COLUMNS);
        txtIR2.setFont(font);

        JLabel lblEA = new JLabel("EA");
        springLayout.putConstraint(SpringLayout.NORTH, lblEA, 0, SpringLayout.NORTH, lblSP);
        springLayout.putConstraint(SpringLayout.EAST, lblEA, 0, SpringLayout.EAST, lblNZVC);
        add(lblEA);

        txtEA = new JTextField();
        txtEA.setEditable(false);
        springLayout.putConstraint(SpringLayout.NORTH, txtEA, 0, SpringLayout.NORTH, txtSP);
        springLayout.putConstraint(SpringLayout.WEST, txtEA, 10, SpringLayout.EAST, lblEA);
        add(txtEA);
        txtEA.setColumns(WORD_COLUMNS);
        txtEA.setFont(font);

        JLabel lblMem = new JLabel("MEM");
        springLayout.putConstraint(SpringLayout.NORTH, lblMem, 15, SpringLayout.SOUTH, lblEA);
        springLayout.putConstraint(SpringLayout.EAST, lblMem, 0, SpringLayout.EAST, lblNZVC);
        add(lblMem);

        JSpinner spnMem = new JSpinner();
        spnMem.setModel(new SpinnerNumberModel(0, 0, 65535, 1));
        springLayout.putConstraint(SpringLayout.NORTH, spnMem, 6, SpringLayout.SOUTH, txtEA);
        springLayout.putConstraint(SpringLayout.WEST, spnMem, 0, SpringLayout.WEST, txtNZVC);
        springLayout.putConstraint(SpringLayout.EAST, spnMem, 20, SpringLayout.EAST, txtNZVC);
        add(spnMem);

        // see
        // https://github.com/aterai/java-swing-tips/blob/main/examples/HexFormatterSpinner/src/java/example/MainPanel.java
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spnMem.getEditor();
        JFormattedTextField ftf = editor.getTextField();
        ftf.setFont(font);
        ftf.setFormatterFactory(createFormatterFactory());

        JTextArea txtMem = new JTextArea();
        txtMem.setText("0000 00 01 02 03 04 05 06 07\n0008 08 09 0A 0B 0C 0D 0E 0F");
        txtMem.setRows(10);
        txtMem.setEditable(false);
        springLayout.putConstraint(SpringLayout.NORTH, txtMem, 6, SpringLayout.SOUTH, spnMem);
        springLayout.putConstraint(SpringLayout.WEST, txtMem, 0, SpringLayout.WEST, spnMem);
        springLayout.putConstraint(SpringLayout.SOUTH, txtMem, 130, SpringLayout.SOUTH, spnMem);
        springLayout.putConstraint(SpringLayout.EAST, txtMem, 130, SpringLayout.EAST, txtIR1);
        add(txtMem);
        txtMem.setFont(font);

        JLabel lblStack = new JLabel("S");
        springLayout.putConstraint(SpringLayout.NORTH, lblStack, 15, SpringLayout.SOUTH, lblSP);
        springLayout.putConstraint(SpringLayout.EAST, lblStack, 0, SpringLayout.EAST, lblA);
        add(lblStack);

        JSpinner spnStack = new JSpinner();
        spnStack.setModel(new SpinnerNumberModel(0, -100, 100, 1));
        springLayout.putConstraint(SpringLayout.NORTH, spnStack, 6, SpringLayout.SOUTH, txtSP);
        springLayout.putConstraint(SpringLayout.WEST, spnStack, 0, SpringLayout.WEST, txtA);
        springLayout.putConstraint(SpringLayout.EAST, spnStack, 20, SpringLayout.EAST, txtSP);
        add(spnStack);
        spnStack.setFont(font);

        JTextArea txtStack = new JTextArea();
        txtStack.setText("0000 1234\n0002 5678\n0004 9ABC\n0006 DEF0");
        txtStack.setRows(10);
        txtStack.setEditable(false);
        springLayout.putConstraint(SpringLayout.NORTH, txtStack, 6, SpringLayout.SOUTH, spnStack);
        springLayout.putConstraint(SpringLayout.WEST, txtStack, 0, SpringLayout.WEST, spnStack);
        springLayout.putConstraint(SpringLayout.SOUTH, txtStack, 134, SpringLayout.SOUTH, spnStack);
        springLayout.putConstraint(SpringLayout.EAST, txtStack, -38, SpringLayout.WEST, txtMem);
        add(txtStack);
        txtStack.setFont(font);
    }

    public void attach(State state) {
        this.state = state;
        refresh();
    }

    private void refresh() {
        if (state != null) {
            // TODO Auto-generated method stub
        }
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
                // try { return Integer.valueOf(text, 16); } catch (Exception ex) {...}
            }

            @Override
            public String valueToString(Object value) {
                return String.format("%04X", (Integer) value);
            }
        };
        formatter.setValueClass(Integer.class);
        formatter.setOverwriteMode(true);
        return new DefaultFormatterFactory(formatter);
    }

    @Override
    public void setPanelFont(Font font) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getTitle() {
        return "state";
    }
}
