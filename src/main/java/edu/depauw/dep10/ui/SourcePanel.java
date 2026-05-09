package edu.depauw.dep10.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
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
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
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
    private static final String DEFAULT_FILENAME = "Untitled.pep";

    private MainFrame parent;

    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private FindToolBar findToolBar;
    private ReplaceToolBar replaceToolBar;
    private TextEditorPane textArea;
    private StatusBar statusBar;
    private CollapsibleSectionPanel csp;

    private JFileChooser chooser;

    private Action showFindBarAction;
    private Action showReplaceBarAction;
    private Action hideSearchBarAction;
    private Action showFindDialogAction;
    private Action showReplaceDialogAction;
    private Action goToLineAction;
    private Action assembleAction;
    private Action runAction;
    private Action newAction;
    private Action openDialogAction;
    private Action saveAction;
    private Action saveAsDialogAction;

    public SourcePanel(MainFrame parent, String name) {
        this.parent = parent;

        this.chooser = new JFileChooser();
        var filter = new FileNameExtensionFilter("Pep source", "pep");
        chooser.setFileFilter(filter);

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

        var file = new File(chooser.getCurrentDirectory(), DEFAULT_FILENAME);
        try {
            textArea = new TextEditorPane(TextEditorPane.INSERT_MODE, false, FileLocation.create(file));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        textArea.setRows(25);
        textArea.setColumns(120);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86); // TODO
        textArea.setPaintTabLines(true);
        textArea.setMarkOccurrences(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        csp.add(sp);

        updateTitle();

        textArea.addPropertyChangeListener(TextEditorPane.DIRTY_PROPERTY, evt -> {
            updateTitle();
        });

        textArea.addPropertyChangeListener(TextEditorPane.FULL_PATH_PROPERTY, evt -> {
            updateTitle();
        });

        ErrorStrip errorStrip = new ErrorStrip(textArea);
        this.add(errorStrip, BorderLayout.LINE_END);

        statusBar = new StatusBar();
        this.add(statusBar, BorderLayout.SOUTH);
    }

    private void updateTitle() {
        var title = (textArea.isDirty() ? "*" : "") + textArea.getFileName();
        parent.updateTitle(title);

        if (textArea.getFileName().equals(DEFAULT_FILENAME) || !textArea.isDirty()) {
            getSaveAction().setEnabled(false);
        } else {
            getSaveAction().setEnabled(true);
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
        if (showFindBarAction == null) {
            int ctrl = getToolkit().getMenuShortcutKeyMaskEx();
            int shift = InputEvent.SHIFT_DOWN_MASK;

            KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl | shift);
            showFindBarAction = csp.addBottomComponent(ks, findToolBar);
            showFindBarAction.putValue(Action.NAME, "Show Find Search Bar");
        }

        return showFindBarAction;
    }

    public Action getShowReplaceBarAction() {
        if (showReplaceBarAction == null) {
            int ctrl = getToolkit().getMenuShortcutKeyMaskEx();
            int shift = InputEvent.SHIFT_DOWN_MASK;

            var ks = KeyStroke.getKeyStroke(KeyEvent.VK_H, ctrl | shift);
            showReplaceBarAction = csp.addBottomComponent(ks, replaceToolBar);
            showReplaceBarAction.putValue(Action.NAME, "Show Replace Search Bar");
        }

        return showReplaceBarAction;
    }

    public Action getHideSearchBarAction() {
        if (hideSearchBarAction == null) {
            var ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0); // duplicates what csp already does, but this makes
                                                                    // the
            hideSearchBarAction = new AbstractAction("Hide Search Bar") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    csp.hideBottomComponent();
                }
            };
            hideSearchBarAction.putValue(Action.ACCELERATOR_KEY, ks);
        }

        return hideSearchBarAction;
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

    public Action getShowFindDialogAction() {
        if (showFindDialogAction == null) {
            showFindDialogAction = new ShowFindDialogAction();
        }

        return showFindDialogAction;
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

    public Action getShowReplaceDialogAction() {
        if (showReplaceDialogAction == null) {
            showReplaceDialogAction = new ShowReplaceDialogAction();
        }

        return showReplaceDialogAction;
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

    public Action getGoToLineAction() {
        if (goToLineAction == null) {
            goToLineAction = new GoToLineAction();
        }

        return goToLineAction;
    }

    private class AssembleAction extends AbstractAction {
        private OutputPanel listing;
        private OutputPanel object;

        public AssembleAction(OutputPanel listing, OutputPanel object) {
            super("Assemble");
            this.listing = listing;
            this.object = object;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            var log = new ErrorLog();
            Sources sources = new Sources();
            sources.addResource(Driver.FULL_OS_HEADER, log); // TODO change based on sourceType in MainFrame
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
                
                if (result.hasErrors()) {
                    runAction.setEnabled(false);
                } else {
                    runAction.setEnabled(true);
                }

                listing.setContent(writer.toString());
                object.setContent(result.toObjectFile());
                parent.selectListingTab();
            }

            // TODO deal with errors; don't run on UI thread!
        }
    }

    public Action getAssembleAction(OutputPanel listing, OutputPanel object) {
        if (assembleAction == null) {
            assembleAction = new AssembleAction(listing, object);
        }

        return assembleAction;
    }

    private class RunAction extends AbstractAction {
        private OutputPanel object;
        private TerminalPanel terminal;

        public RunAction(OutputPanel object, TerminalPanel terminal) {
            super("Run");
            this.object = object;
            this.terminal = terminal;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            State state = new State();
            state.loadString(object.getContent());
            state.loadResource(Driver.FULL_OS_OBJECT);

            terminal.clear();
            state.setInput(terminal.getInputStream());
            state.setOutput(terminal.getOutputStream());
            state.setError(terminal.getOutputStream());
            // TODO allow batch I/O

            Simulator sim = new Simulator(state);

            Controller control = new PlainController();
            var t = new Thread(() -> sim.run(control));
            t.start();

            parent.selectTerminalTab();

            // TODO tracing; errors, ...
        }
    }

    public Action getRunAction(OutputPanel object, TerminalPanel terminal) {
        if (runAction == null) {
            runAction = new RunAction(object, terminal);
        }

        return runAction;
    }

    private class NewAction extends AbstractAction {
        public NewAction() {
            super("New");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (textArea.isDirty()) {
                var result = JOptionPane.showConfirmDialog(parent, "Save current file?");
                if (result == JOptionPane.CANCEL_OPTION) {
                    return;
                } else if (result == JOptionPane.YES_OPTION) {
                    try {
                        textArea.save();
                    } catch (IOException e1) {
                        // TODO display error message?
                        return;
                    }
                }
            }

            try {
                var newFile = new File(chooser.getCurrentDirectory(), DEFAULT_FILENAME);
                textArea.load(FileLocation.create(newFile));
                runAction.setEnabled(false);
            } catch (IOException e1) {
                // TODO display error message?
            }
        }
    }

    public Action getNewAction() {
        if (newAction == null) {
            newAction = new NewAction();
        }

        return newAction;
    }

    private class OpenDialogAction extends AbstractAction {
        public OpenDialogAction() {
            super("Open...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                try {
                    textArea.load(FileLocation.create(chooser.getSelectedFile()));
                    runAction.setEnabled(false);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
    }

    public Action getOpenDialogAction() {
        if (openDialogAction == null) {
            openDialogAction = new OpenDialogAction();
        }

        return openDialogAction;
    }

    private class SaveAction extends AbstractAction {
        public SaveAction() {
            super("Save");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                textArea.save();
            } catch (IOException e1) {
                // TODO
                e1.printStackTrace();
            }
        }
    }
    
    public Action getSaveAction() {
        if (saveAction == null) {
            saveAction = new SaveAction();
        }
        
        return saveAction;
    }
    
    private class SaveAsDialogAction extends AbstractAction {
        public SaveAsDialogAction() {
            super("Save As...");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                try {
                    textArea.saveAs(FileLocation.create(chooser.getSelectedFile()));
                } catch (IOException e1) {
                    // TODO
                    e1.printStackTrace();
                }
            }
        }
    }

    public Action getSaveAsDialogAction() {
        if (saveAsDialogAction == null) {
            saveAsDialogAction = new SaveAsDialogAction();
        }
        
        return saveAsDialogAction;
    }

    public boolean canQuit() {
        if (textArea.isDirty()) {
            var result = JOptionPane.showConfirmDialog(parent, "Save before quit?");
            if (result == JOptionPane.CANCEL_OPTION) {
                return false;
            } else if (result == JOptionPane.YES_OPTION) {
                try {
                    textArea.save();
                } catch (IOException e) {
                    // TODO display error message?
                    return false;
                }
            }
        }

        return true;
    }
}
