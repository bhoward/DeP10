package edu.depauw.dep10.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

import org.fife.rsta.ui.CollapsibleSectionPanel;
import org.fife.rsta.ui.GoToDialog;
import org.fife.rsta.ui.SizeGripIcon;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.FindToolBar;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.ReplaceToolBar;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import com.formdev.flatlaf.util.SystemInfo;

import edu.depauw.dep10.assemble.Assembler;
import edu.depauw.dep10.assemble.Result;
import edu.depauw.dep10.driver.Driver;
import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.preprocess.Preprocessor;
import edu.depauw.dep10.preprocess.Sources;

// Largely based on RSTAUIDemoApp
@SuppressWarnings("serial")
public class MainFrame extends JFrame implements SearchListener {
    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private FindToolBar findToolBar;
    private ReplaceToolBar replaceToolBar;
    private RSyntaxTextArea textArea;
    private StatusBar statusBar;
    private CollapsibleSectionPanel csp;
    
    private OutputPanel listing;
    private OutputPanel object;

    public MainFrame() {
        setSize(400, 300);
        setTitle("DeP10 IDE");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var tabs = new JTabbedPane();
        this.add(tabs);

        this.findDialog = new FindDialog(this, this);
        this.replaceDialog = new ReplaceDialog(this, this);
        
        // This ties the properties of the two dialogs together (match case,
        // regex, etc.).
        SearchContext context = findDialog.getSearchContext();
        replaceDialog.setSearchContext(context);

        // Create toolbars and tie their search contexts together also.
        findToolBar = new FindToolBar(this);
        findToolBar.setSearchContext(context);
        replaceToolBar = new ReplaceToolBar(this);
        replaceToolBar.setSearchContext(context);

        var source = createSourcePanel();
        tabs.add("source", source);

        listing = new OutputPanel("listing");
        object = new OutputPanel("object");
        
        tabs.add(listing.getTitle(), listing);
        tabs.add(object.getTitle(), object);

        pack();
        setLocationRelativeTo(null);

        var menuBar = createMenuBar();

        setJMenuBar(menuBar);
    }

    private JPanel createSourcePanel() {
        JPanel contentPane = new JPanel(new BorderLayout());
        csp = new CollapsibleSectionPanel();
        contentPane.add(csp);

        textArea = new RSyntaxTextArea(25, 80);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86); // TODO
        textArea.setPaintTabLines(true);
        textArea.setMarkOccurrences(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        csp.add(sp);

        ErrorStrip errorStrip = new ErrorStrip(textArea);
        contentPane.add(errorStrip, BorderLayout.LINE_END);

        statusBar = new StatusBar();
        contentPane.add(statusBar, BorderLayout.SOUTH);

        return contentPane;
    }

    public JMenuBar createMenuBar() {
        var menuBar = new JMenuBar();

        // File Menu
        var fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        var newMenuItem = new JMenuItem("New");
        fileMenu.add(newMenuItem);

        var openMenuItem = new JMenuItem("Open...");
        fileMenu.add(openMenuItem);

        var saveMenuItem = new JMenuItem("Save");
        fileMenu.add(saveMenuItem);

        // Edit Menu
        var editMenu = new JMenu("Edit");
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.UNDO_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.REDO_ACTION)));
        editMenu.addSeparator();
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.CUT_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.COPY_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.PASTE_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.DELETE_ACTION)));
        editMenu.addSeparator();
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.SELECT_ALL_ACTION)));
        menuBar.add(editMenu);

        // Search Menu
        var searchMenu = new JMenu("Search");
        menuBar.add(searchMenu);

        var findMenuItem = new JMenuItem(new ShowFindDialogAction());
        searchMenu.add(findMenuItem);

        var replaceMenuItem = new JMenuItem(new ShowReplaceDialogAction());
        searchMenu.add(replaceMenuItem);

        var gotoMenuItem = new JMenuItem(new GoToLineAction());
        searchMenu.add(gotoMenuItem);

        int ctrl = getToolkit().getMenuShortcutKeyMaskEx();
        int shift = InputEvent.SHIFT_DOWN_MASK;

        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl | shift);
        Action showFindBarAction = csp.addBottomComponent(ks, findToolBar);
        showFindBarAction.putValue(Action.NAME, "Show Find Search Bar");
        searchMenu.add(new JMenuItem(showFindBarAction));

        ks = KeyStroke.getKeyStroke(KeyEvent.VK_H, ctrl | shift);
        Action showReplaceBarAction = csp.addBottomComponent(ks, replaceToolBar);
        showReplaceBarAction.putValue(Action.NAME, "Show Replace Search Bar");
        searchMenu.add(new JMenuItem(showReplaceBarAction));

        ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0); // duplicates what csp already does, but this makes the
                                                            // accelerator key show in the menu
        Action hideSearchBarAction = new AbstractAction("Hide Search Bar") {
            @Override
            public void actionPerformed(ActionEvent e) {
                csp.hideBottomComponent();
            }
        };
        hideSearchBarAction.putValue(Action.ACCELERATOR_KEY, ks);
        searchMenu.add(new JMenuItem(hideSearchBarAction));

        // Build Menu
        var buildMenu = new JMenu("Build");
        buildMenu.add(createMenuItem(new AbstractAction("Assemble") {
            @Override
            public void actionPerformed(ActionEvent e) {
                var log = new ErrorLog();
                Sources sources = new Sources();
                sources.addResource(Driver.FULL_OS_HEADER, log);
                sources.addString(textArea.getText());
                
                var preprocessor = new Preprocessor(log);
                Result result = null;
                
                if (log.noErrors()) {
                    var lines = preprocessor.preprocess(sources);
                    var assembler = new Assembler(log);
                    result = assembler.assemble(lines);
                    
                    // Print a listing for testing purposes
                    var writer = new StringWriter();
                    try (var out = new PrintWriter(writer)) {
                        result.printListing(out);
                    }
                    
                    listing.setContent(writer.toString());
                    object.setContent(result.toObjectFile());
                }
                
                // TODO deal with errors
            }
        }));
        menuBar.add(buildMenu);

        // Debug Menu
        var debugMenu = new JMenu("Debug");
        menuBar.add(debugMenu);

        // Simulator Menu
        var simulatorMenu = new JMenu("Simulator");
        menuBar.add(simulatorMenu);

        // Help Menu
        var helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        if (!SystemInfo.isMacOS) {
            fileMenu.addSeparator();

            var exitMenuItem = new JMenuItem("Exit");
            fileMenu.add(exitMenuItem);

            editMenu.addSeparator();

            var preferencesMenuItem = new JMenuItem("Preferences...");
            editMenu.add(preferencesMenuItem);

            helpMenu.addSeparator();

            var aboutMenuItem = new JMenuItem("About...");
            helpMenu.add(aboutMenuItem);
        }

        return menuBar;
    }

    // Source:
    // https://github.com/bobbylight/RSyntaxTextArea/wiki/Example:-Adding-an-Edit-menu-for-the-editor
    private static JMenuItem createMenuItem(Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setToolTipText(null); // Swing annoyingly adds tool tip text to the menu item
        return item;
    }

    /**
     * Opens the "Go to Line" dialog.
     */
    private class GoToLineAction extends AbstractAction {
        public GoToLineAction() {
            super("Go To Line...");
            int c = getToolkit().getMenuShortcutKeyMaskEx();
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, c));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (findDialog.isVisible()) {
                findDialog.setVisible(false);
            }
            if (replaceDialog.isVisible()) {
                replaceDialog.setVisible(false);
            }
            GoToDialog dialog = new GoToDialog(MainFrame.this);
            dialog.setMaxLineNumberAllowed(textArea.getLineCount());
            dialog.setVisible(true);
            int line = dialog.getLineNumber();
            if (line > 0) {
                try {
                    textArea.setCaretPosition(textArea.getLineStartOffset(line - 1));
                } catch (BadLocationException ble) { // Never happens
                    UIManager.getLookAndFeel().provideErrorFeedback(textArea);
                    ble.printStackTrace();
                }
            }
        }
    }

    /**
     * Shows the Find dialog.
     */
    private class ShowFindDialogAction extends AbstractAction {
        public ShowFindDialogAction() {
            super("Find...");
            int c = getToolkit().getMenuShortcutKeyMaskEx();
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, c));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (replaceDialog.isVisible()) {
                replaceDialog.setVisible(false);
            }
            findDialog.setVisible(true);
        }
    }

    /**
     * Shows the Replace dialog.
     */
    private class ShowReplaceDialogAction extends AbstractAction {
        public ShowReplaceDialogAction() {
            super("Replace...");
            int c = getToolkit().getMenuShortcutKeyMaskEx();
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, c));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (findDialog.isVisible()) {
                findDialog.setVisible(false);
            }
            replaceDialog.setVisible(true);
        }
    }

    private static class StatusBar extends JPanel {
        private JLabel label;

        public StatusBar() {
            label = new JLabel("Ready");
            setLayout(new BorderLayout());
            add(label, BorderLayout.LINE_START);
            add(new JLabel(new SizeGripIcon()), BorderLayout.LINE_END);
        }

        public void setLabel(String label) {
            this.label.setText(label);
        }
    }

    @Override
    public void searchEvent(SearchEvent e) {
        SearchEvent.Type type = e.getType();
        SearchContext context = e.getSearchContext();
        SearchResult result;

        switch (type) {
        case MARK_ALL:
            result = SearchEngine.markAll(textArea, context);
            break;

        case FIND:
            result = SearchEngine.find(textArea, context);
            if (!result.wasFound() || result.isWrapped()) {
                UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            }
            break;

        case REPLACE:
            result = SearchEngine.replace(textArea, context);
            if (!result.wasFound() || result.isWrapped()) {
                UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            }
            break;

        case REPLACE_ALL:
            result = SearchEngine.replaceAll(textArea, context);
            JOptionPane.showMessageDialog(null, result.getCount() +
                    " occurrences replaced.");
            break;
        default:
            result = null; // should not happen
        }

        String text;
        if (result.wasFound()) {
            text = "Text found; occurrences marked: " + result.getMarkedCount();
        } else if (type == SearchEvent.Type.MARK_ALL) {
            if (result.getMarkedCount() > 0) {
                text = "Occurrences marked: " + result.getMarkedCount();
            } else {
                text = "";
            }
        } else {
            text = "Text not found";
        }
        statusBar.setLabel(text);
    }

    @Override
    public String getSelectedText() {
        return textArea.getSelectedText();
    }
}