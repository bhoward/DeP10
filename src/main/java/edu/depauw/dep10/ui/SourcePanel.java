package edu.depauw.dep10.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

@SuppressWarnings("serial")
class SourcePanel extends JPanel {
    private final String name;
    private final RSyntaxTextArea textArea;
    private final JMenu editMenu;
    private final JMenu buildMenu;

    public SourcePanel(String name) {
        this.name = name;

        textArea = new RSyntaxTextArea(25, 80);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86); // TODO
        textArea.setPaintTabLines(true);

        RTextScrollPane sp = new RTextScrollPane(textArea);
        this.setLayout(new BorderLayout());
        this.add(sp);

        this.editMenu = new JMenu("Edit");
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.UNDO_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.REDO_ACTION)));
        editMenu.addSeparator();
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.CUT_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.COPY_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.PASTE_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.DELETE_ACTION)));
        editMenu.addSeparator();
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.SELECT_ALL_ACTION)));

        this.buildMenu = new JMenu("Build");
        buildMenu.add(createMenuItem(new AbstractAction("Assemble") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub

            }
        }));
    }

    // Source:
    // https://github.com/bobbylight/RSyntaxTextArea/wiki/Example:-Adding-an-Edit-menu-for-the-editor
    private static JMenuItem createMenuItem(Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setToolTipText(null); // Swing annoyingly adds tool tip text to the menu item
        return item;
    }

    public String getContent() {
        return textArea.getText();
    }

    public void setContent(String content) {
        textArea.setText(content);
    }

    public JMenu getEditMenu() {
        return editMenu;
    }

    public JMenu getBuildMenu() {
        return buildMenu;
    }

    public String getTitle() {
        return name;
    }
}