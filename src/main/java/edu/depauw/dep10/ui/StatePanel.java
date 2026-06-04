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
import edu.depauw.dep10.util.Util;
import edu.depauw.dep10.util.Word;

public class StatePanel extends JPanel implements TabPanel {
    private static final long serialVersionUID = 1L;
    private static final int WORD_COLUMNS = 5;
    private static final int BYTE_COLUMNS = 3;
    private static final int FONT_SIZE = 11;
    private static final int STACK_COLUMNS = 10;
    private static final int STACK_ROWS = 10;
    private static final int MEM_COLUMNS = 30;
    private static final int MEM_ROWS = 10;

    private JTextField txtA;
    private JTextField txtX;
    private JTextField txtPC;
    private JTextField txtSP;
    private JTextField txtNZVC;
    private JTextField txtPX;
    private JTextField txtIR1;
    private JTextField txtIR2;
    private JTextField txtEA;
    private JTextField txtOperation;

    private State state;
    private Font font;
    private JSpinner spnStack;
    private JTextArea txtStack;
    private JSpinner spnMem;
    private JTextArea txtMem;

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
        springLayout.putConstraint(SpringLayout.NORTH, txtA, 0, SpringLayout.NORTH, lblA);
        springLayout.putConstraint(SpringLayout.WEST, txtA, 10, SpringLayout.EAST, lblA);
        add(txtA);
        txtA.setEditable(false);
        txtA.setColumns(WORD_COLUMNS);
        txtA.setFont(font);

        JLabel lblX = new JLabel("X");
        springLayout.putConstraint(SpringLayout.NORTH, lblX, 0, SpringLayout.NORTH, lblA);
        springLayout.putConstraint(SpringLayout.WEST, lblX, 10, SpringLayout.EAST, txtA);
        add(lblX);

        txtX = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtX, 0, SpringLayout.NORTH, lblA);
        springLayout.putConstraint(SpringLayout.WEST, txtX, 10, SpringLayout.EAST, lblX);
        add(txtX);
        txtX.setEditable(false);
        txtX.setColumns(WORD_COLUMNS);
        txtX.setFont(font);

        JLabel lblNZVC = new JLabel("NZVC");
        springLayout.putConstraint(SpringLayout.NORTH, lblNZVC, 0, SpringLayout.NORTH, lblA);
        springLayout.putConstraint(SpringLayout.WEST, lblNZVC, 10, SpringLayout.EAST, txtX);
        add(lblNZVC);

        txtNZVC = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtNZVC, 0, SpringLayout.NORTH, lblA);
        springLayout.putConstraint(SpringLayout.WEST, txtNZVC, 10, SpringLayout.EAST, lblNZVC);
        txtNZVC.setEditable(false);
        add(txtNZVC);
        txtNZVC.setColumns(WORD_COLUMNS);
        txtNZVC.setFont(font);

        JLabel lblPC = new JLabel("PC");
        springLayout.putConstraint(SpringLayout.NORTH, lblPC, 0, SpringLayout.NORTH, lblA);
        springLayout.putConstraint(SpringLayout.WEST, lblPC, 10, SpringLayout.EAST, txtNZVC);
        add(lblPC);

        txtPC = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtPC, 0, SpringLayout.NORTH, lblA);
        springLayout.putConstraint(SpringLayout.WEST, txtPC, 10, SpringLayout.EAST, lblPC);
        add(txtPC);
        txtPC.setEditable(false);
        txtPC.setColumns(WORD_COLUMNS);
        txtPC.setFont(font);

        JLabel lblPX = new JLabel("PX");
        springLayout.putConstraint(SpringLayout.NORTH, lblPX, 10, SpringLayout.SOUTH, lblA);
        springLayout.putConstraint(SpringLayout.EAST, lblPX, 0, SpringLayout.EAST, lblA);
        add(lblPX);

        txtPX = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtPX, 0, SpringLayout.NORTH, lblPX);
        springLayout.putConstraint(SpringLayout.WEST, txtPX, 10, SpringLayout.EAST, lblPX);
        txtPX.setEditable(false);
        add(txtPX);
        txtPX.setColumns(BYTE_COLUMNS);
        txtPX.setFont(font);

        JLabel lblIR1 = new JLabel("IR1");
        springLayout.putConstraint(SpringLayout.NORTH, lblIR1, 0, SpringLayout.NORTH, lblPX);
        springLayout.putConstraint(SpringLayout.EAST, lblIR1, 0, SpringLayout.EAST, lblX);
        add(lblIR1);

        txtIR1 = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtIR1, 0, SpringLayout.NORTH, lblPX);
        springLayout.putConstraint(SpringLayout.WEST, txtIR1, 10, SpringLayout.EAST, lblIR1);
        txtIR1.setEditable(false);
        add(txtIR1);
        txtIR1.setColumns(BYTE_COLUMNS);
        txtIR1.setFont(font);

        JLabel lblIR2 = new JLabel("IR2");
        springLayout.putConstraint(SpringLayout.NORTH, lblIR2, 0, SpringLayout.NORTH, lblPX);
        springLayout.putConstraint(SpringLayout.EAST, lblIR2, 0, SpringLayout.EAST, lblNZVC);
        add(lblIR2);

        txtIR2 = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtIR2, 0, SpringLayout.NORTH, lblPX);
        springLayout.putConstraint(SpringLayout.WEST, txtIR2, 10, SpringLayout.EAST, lblIR2);
        txtIR2.setEditable(false);
        add(txtIR2);
        txtIR2.setColumns(WORD_COLUMNS);
        txtIR2.setFont(font);

        JLabel lblEA = new JLabel("EA");
        springLayout.putConstraint(SpringLayout.NORTH, lblEA, 0, SpringLayout.NORTH, lblPX);
        springLayout.putConstraint(SpringLayout.EAST, lblEA, 0, SpringLayout.EAST, lblPC);
        add(lblEA);

        txtEA = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtEA, 0, SpringLayout.NORTH, lblPX);
        springLayout.putConstraint(SpringLayout.WEST, txtEA, 10, SpringLayout.EAST, lblEA);
        txtEA.setEditable(false);
        add(txtEA);
        txtEA.setColumns(WORD_COLUMNS);
        txtEA.setFont(font);

        JLabel lblSP = new JLabel("SP");
        springLayout.putConstraint(SpringLayout.NORTH, lblSP, 10, SpringLayout.SOUTH, lblPX);
        springLayout.putConstraint(SpringLayout.EAST, lblSP, 0, SpringLayout.EAST, lblA);
        add(lblSP);

        txtSP = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtSP, 0, SpringLayout.NORTH, lblSP);
        springLayout.putConstraint(SpringLayout.WEST, txtSP, 10, SpringLayout.EAST, lblSP);
        add(txtSP);
        txtSP.setEditable(false);
        txtSP.setColumns(WORD_COLUMNS);
        txtSP.setFont(font);

        JLabel lblStack = new JLabel("S");
        springLayout.putConstraint(SpringLayout.NORTH, lblStack, 10, SpringLayout.SOUTH, lblSP);
        springLayout.putConstraint(SpringLayout.EAST, lblStack, 0, SpringLayout.EAST, lblA);
        add(lblStack);

        spnStack = new JSpinner();
        springLayout.putConstraint(SpringLayout.NORTH, spnStack, 0, SpringLayout.NORTH, lblStack);
        springLayout.putConstraint(SpringLayout.WEST, spnStack, 10, SpringLayout.EAST, lblStack);
        spnStack.setModel(new SpinnerNumberModel(0, -100, 100, 2));
        springLayout.putConstraint(SpringLayout.EAST, spnStack, 20, SpringLayout.EAST, txtSP);
        add(spnStack);
        spnStack.setFont(new Font("Monospaced", Font.PLAIN, 11));
        spnStack.addChangeListener(e -> {
            this.refresh();
        });

        txtStack = new JTextArea();
        springLayout.putConstraint(SpringLayout.NORTH, txtStack, 10, SpringLayout.SOUTH, spnStack);
        springLayout.putConstraint(SpringLayout.WEST, txtStack, 0, SpringLayout.WEST, spnStack);
        txtStack.setColumns(STACK_COLUMNS);
        txtStack.setRows(STACK_ROWS);
        txtStack.setEditable(false);
        add(txtStack);
        txtStack.setFont(font);

        JLabel lblMem = new JLabel("MEM");
        springLayout.putConstraint(SpringLayout.NORTH, lblMem, 0, SpringLayout.NORTH, lblStack);
        springLayout.putConstraint(SpringLayout.EAST, lblMem, 0, SpringLayout.EAST, lblNZVC);
        add(lblMem);

        spnMem = new JSpinner();
        spnMem.setFont(new Font("Monospaced", Font.PLAIN, 11));
        springLayout.putConstraint(SpringLayout.NORTH, spnMem, 0, SpringLayout.NORTH, lblMem);
        springLayout.putConstraint(SpringLayout.WEST, spnMem, 10, SpringLayout.EAST, lblMem);
        springLayout.putConstraint(SpringLayout.EAST, spnMem, 20, SpringLayout.EAST, txtNZVC);
        spnMem.setModel(new SpinnerNumberModel(0, 0, 65535, 8));
        add(spnMem);
        spnMem.addChangeListener(e -> {
            this.refresh();
        });

        // see
        // https://github.com/aterai/java-swing-tips/blob/main/examples/HexFormatterSpinner/src/java/example/MainPanel.java
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spnMem.getEditor();
        JFormattedTextField ftf = editor.getTextField();
        ftf.setFont(font);
        ftf.setFormatterFactory(createFormatterFactory());

        txtMem = new JTextArea();
        springLayout.putConstraint(SpringLayout.NORTH, txtMem, 10, SpringLayout.SOUTH, spnMem);
        springLayout.putConstraint(SpringLayout.WEST, txtMem, 40, SpringLayout.EAST, txtStack);
        txtMem.setColumns(MEM_COLUMNS);
        txtMem.setRows(MEM_ROWS);
        txtMem.setEditable(false);
        add(txtMem);
        txtMem.setFont(font);

        txtOperation = new JTextField();
        springLayout.putConstraint(SpringLayout.NORTH, txtOperation, 0, SpringLayout.NORTH, txtSP);
        springLayout.putConstraint(SpringLayout.WEST, txtOperation, 0, SpringLayout.WEST, txtX);
        add(txtOperation);
        txtOperation.setColumns(10);
        txtOperation.setEditable(false);
        txtOperation.setFont(font);
    }

    public void attach(State state) {
        this.state = state;
        refresh();
    }

    public void refresh() {
        if (state != null) {
            txtA.setText(state.getA().toString());
            txtX.setText(state.getX().toString());
            txtPC.setText(state.getPC().toString());
            txtSP.setText(state.getSP().toString());
            txtPX.setText(state.getPrefix().toString());
            txtIR1.setText(state.getOpCode().toString());
            txtIR2.setText(state.getOperand().toString());
            txtEA.setText(state.getEA().toString());

            if (state.getOp() != null) {
                txtOperation.setText(state.getOp().toString());
            }

            String flags = (state.getN() ? "1" : "0") +
                    (state.getZ() ? "1" : "0") +
                    (state.getV() ? "1" : "0") +
                    (state.getC() ? "1" : "0");
            txtNZVC.setText(flags);

            var stackOffset = ((Integer) spnStack.getValue()).intValue();
            var builder = new StringBuilder();
            for (int i = stackOffset; i < stackOffset + 2 * STACK_ROWS; i += 2) {
                var address = state.getSP().plus(i);
                var contents = state.mem2Safe(address);

                builder.append(i == 0 ? '>' : ' ');
                builder.append(address);
                builder.append(" ");
                builder.append(contents);
                builder.append("\n");
            }
            txtStack.setText(builder.toString());

            var memStart = ((Integer) spnMem.getValue()).intValue();
            builder = new StringBuilder();
            for (int i = memStart; i < memStart + 8 * MEM_ROWS; i += 8) {
                var address = Word.of(i);
                builder.append(address);

                for (int col = 0; col < 8; col++) {
                    var contents = state.mem1Safe(address.plus(col));
                    builder.append(" ");
                    builder.append(contents);
                }
                builder.append("\n");
            }
            txtMem.setText(builder.toString());
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

    @Override
    public void setPanelFont(Font font) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getTitle() {
        return "state";
    }
}
