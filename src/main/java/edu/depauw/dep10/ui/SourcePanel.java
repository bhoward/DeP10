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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import edu.depauw.dep10.assemble.Assembler;
import edu.depauw.dep10.assemble.Result;
import edu.depauw.dep10.driver.Driver;
import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.preprocess.Preprocessor;
import edu.depauw.dep10.preprocess.Sources;
import edu.depauw.dep10.simulator.Controller;
import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;

@SuppressWarnings("serial")
public class SourcePanel extends JPanel implements SearchListener {
    private JFrame parent;
    private String name;
    
    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private FindToolBar findToolBar;
    private ReplaceToolBar replaceToolBar;
    private RSyntaxTextArea textArea;
    private StatusBar statusBar;
    private CollapsibleSectionPanel csp;

    public SourcePanel(JFrame parent, String name) {
        this.parent = parent;
        this.name = name;
        
        this.findDialog = new FindDialog(parent, this);
        this.replaceDialog = new ReplaceDialog(parent, this);
        
        // This ties the properties of the two dialogs together (match case,
        // regex, etc.).
        SearchContext context = findDialog.getSearchContext();
        replaceDialog.setSearchContext(context);

        // Create toolbars and tie their search contexts together also.
        findToolBar = new FindToolBar(this);
        findToolBar.setSearchContext(context);
        replaceToolBar = new ReplaceToolBar(this);
        replaceToolBar.setSearchContext(context);

        this.setLayout(new BorderLayout());
        csp = new CollapsibleSectionPanel();
        this.add(csp);

        textArea = new RSyntaxTextArea(25, 80);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86); // TODO
        textArea.setPaintTabLines(true);
        textArea.setMarkOccurrences(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        csp.add(sp);

        ErrorStrip errorStrip = new ErrorStrip(textArea);
        this.add(errorStrip, BorderLayout.LINE_END);

        statusBar = new StatusBar();
        this.add(statusBar, BorderLayout.SOUTH);
    }

    public String getTitle() {
        return name;
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
            GoToDialog dialog = new GoToDialog(parent);
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

    public Action getShowFindBarAction() {
        int ctrl = getToolkit().getMenuShortcutKeyMaskEx();
        int shift = InputEvent.SHIFT_DOWN_MASK;

        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl | shift);
        Action showFindBarAction = csp.addBottomComponent(ks, findToolBar);
        showFindBarAction.putValue(Action.NAME, "Show Find Search Bar");
        return showFindBarAction;
    }

    public Action getShowReplaceBarAction() {
        int ctrl = getToolkit().getMenuShortcutKeyMaskEx();
        int shift = InputEvent.SHIFT_DOWN_MASK;

        var ks = KeyStroke.getKeyStroke(KeyEvent.VK_H, ctrl | shift);
        Action showReplaceBarAction = csp.addBottomComponent(ks, replaceToolBar);
        showReplaceBarAction.putValue(Action.NAME, "Show Replace Search Bar");
        return showReplaceBarAction;
    }
    
    public Action getHideSearchBarAction() {
        var ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0); // duplicates what csp already does, but this makes the
                                                            // accelerator key show in the menu
        Action hideSearchBarAction = new AbstractAction("Hide Search Bar") {
            @Override
            public void actionPerformed(ActionEvent e) {
                csp.hideBottomComponent();
            }
        };
        hideSearchBarAction.putValue(Action.ACCELERATOR_KEY, ks);
        return hideSearchBarAction;
    }

    public Action getShowFindDialogAction() {
        return new ShowFindDialogAction();
    }

    public Action getShowReplaceDialogAction() {
        return new ShowReplaceDialogAction();
    }

    public Action getGoToLineAction() {
        return new GoToLineAction();
    }
    
    public Action getAssembleAction(OutputPanel listing, OutputPanel object) {
        return new AbstractAction("Assemble") {
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
                
                // TODO deal with errors; don't run on UI thread!
            }
        };
    }

    public Action getRunAction(OutputPanel object, TerminalPanel terminal) {
    	return new AbstractAction("Run") {
			@Override
			public void actionPerformed(ActionEvent e) {
				State state = new State();
				state.loadString(object.getContent());
				state.loadResource(Driver.FULL_OS_OBJECT);
				state.setInput(terminal.getInputStream());
				state.setOutput(terminal.getOutputStream());
				state.setError(terminal.getOutputStream());
				
				Simulator sim = new Simulator(state);
				
				Controller control = new PlainController();
				var t = new Thread(() -> sim.run(control));
				t.start();
				
				// TODO tracing; errors, ...
			}
			
		};
    }
}
