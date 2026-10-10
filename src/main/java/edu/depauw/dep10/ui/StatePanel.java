package edu.depauw.dep10.ui;

import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import edu.depauw.dep10.simulator.State;

public class StatePanel extends JPanel implements TabPanel {
    private static final long serialVersionUID = 661986232642713099L;
    
    static final int WORD_COLUMNS = 4;
    static final int BYTE_COLUMNS = 2;

    private RegisterField regA;
    private RegisterField regX;
    private RegisterField regH;
    private RegisterField regNZVC;
    private RegisterField regPX;
    private RegisterField regIR1;
    private RegisterField regIR2;
    private RegisterField regEA;
    private RegisterField regPC;
    private RegisterField regSP;
    
    private JTextField txtOperation;

    private MemoryField memory;
    private StackField stack;

    private State state;
    private Font font;
    
    /**
     * Create the panel.
     */
    public StatePanel() {
        this.state = null;
        this.font = SourcePanel.DEFAULT_FONT;

        SpringLayout springLayout = new SpringLayout();
        setLayout(springLayout);

        regA = new RegisterField("A", WORD_COLUMNS);
        regA.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regA, 10, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, regA, 10, SpringLayout.WEST, this);
        add(regA);

        regX = new RegisterField("X", WORD_COLUMNS);
        regX.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regX, 0, SpringLayout.NORTH, regA);
        springLayout.putConstraint(SpringLayout.WEST, regX, 10, SpringLayout.EAST, regA);
        add(regX);

        regH = new RegisterField("H", WORD_COLUMNS);
        regH.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regH, 0, SpringLayout.NORTH, regA);
        springLayout.putConstraint(SpringLayout.WEST, regH, 10, SpringLayout.EAST, regX);
        add(regH);

        regNZVC = new RegisterField("NZVC", WORD_COLUMNS);
        regNZVC.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regNZVC, 0, SpringLayout.NORTH, regA);
        springLayout.putConstraint(SpringLayout.WEST, regNZVC, 10, SpringLayout.EAST, regH);
        add(regNZVC);

        regPX = new RegisterField("PX", BYTE_COLUMNS);
        regPX.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regPX, 10, SpringLayout.SOUTH, regA);
        springLayout.putConstraint(SpringLayout.WEST, regPX, 0, SpringLayout.WEST, regA);
        add(regPX);

        regIR1 = new RegisterField("IR1", BYTE_COLUMNS);
        regIR1.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regIR1, 0, SpringLayout.NORTH, regPX);
        springLayout.putConstraint(SpringLayout.WEST, regIR1, 0, SpringLayout.WEST, regX);
        add(regIR1);

        regIR2 = new RegisterField("IR2", WORD_COLUMNS);
        regIR2.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regIR2, 0, SpringLayout.NORTH, regPX);
        springLayout.putConstraint(SpringLayout.WEST, regIR2, 0, SpringLayout.WEST, regH);
        add(regIR2);

        regEA = new RegisterField("EA", WORD_COLUMNS);
        regEA.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regEA, 0, SpringLayout.NORTH, regPX);
        springLayout.putConstraint(SpringLayout.WEST, regEA, 0, SpringLayout.WEST, regNZVC);
        add(regEA);

        regPC = new RegisterField("PC", WORD_COLUMNS);
        regPC.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regPC, 10, SpringLayout.SOUTH, regPX);
        springLayout.putConstraint(SpringLayout.WEST, regPC, 0, SpringLayout.WEST, regPX);
        add(regPC);

        txtOperation = new JTextField();
        txtOperation.setColumns(10);
        txtOperation.setEditable(false);
        txtOperation.setFont(font);
        
        var borderOp = new JPanel();
        borderOp.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Next"));
        borderOp.add(txtOperation);
        springLayout.putConstraint(SpringLayout.NORTH, borderOp, 0, SpringLayout.NORTH, regPC);
        springLayout.putConstraint(SpringLayout.WEST, borderOp, 0, SpringLayout.WEST, regX);
        add(borderOp);

        regSP = new RegisterField("SP", WORD_COLUMNS);
        regSP.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, regSP, 0, SpringLayout.NORTH, regPC);
        springLayout.putConstraint(SpringLayout.WEST, regSP, 0, SpringLayout.WEST, regNZVC);
        add(regSP);
        
        memory = new MemoryField();
        memory.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, memory, 10, SpringLayout.SOUTH, regPC);
        springLayout.putConstraint(SpringLayout.WEST, memory, 0, SpringLayout.WEST, regPC);
        add(memory);

        stack = new StackField();
        stack.setFont(font);
        springLayout.putConstraint(SpringLayout.NORTH, stack, 10, SpringLayout.SOUTH, regSP);
        springLayout.putConstraint(SpringLayout.WEST, stack, 10, SpringLayout.EAST, memory);
        add(stack);
    }

    public void attach(State state) {
        this.state = state;
        memory.attach(state);
        stack.attach(state);
        refresh();
    }

    public void refresh() {
        if (state != null) {
            regA.setText(state.getA().toString());
            regX.setText(state.getX().toString());
            regH.setText(state.getH().toString());
            regPC.setText(state.getPC().toString());
            regSP.setText(state.getSP().toString());
            regPX.setText(state.getPrefix().toString());
            regIR1.setText(state.getOpCode().toString());
            regIR2.setText(state.getOperand().toString());
            regEA.setText(state.getEA().toString());

            if (state.getOp() != null) {
                txtOperation.setText(state.getOp().toString());
            }

            String flags = (state.getN() ? "1" : "0") +
                    (state.getZ() ? "1" : "0") +
                    (state.getV() ? "1" : "0") +
                    (state.getC() ? "1" : "0");
            regNZVC.setText(flags);

            memory.refresh();
            stack.refresh();
        }
    }

    @Override
    public void setPanelFont(Font font) {
        regA.setFont(font);
        regX.setFont(font);
        regH.setFont(font);
        regNZVC.setFont(font);
        regPC.setFont(font);
        regSP.setFont(font);
        regPX.setFont(font);
        regIR1.setFont(font);
        regIR2.setFont(font);
        regEA.setFont(font);
        txtOperation.setFont(font);
        stack.setFont(font);
        memory.setFont(font);
    }

    @Override
    public String getTitle() {
        return "state";
    }
}
