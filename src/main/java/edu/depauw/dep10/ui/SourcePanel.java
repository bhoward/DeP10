package edu.depauw.dep10.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import edu.depauw.dep10.assemble.Result;

public class SourcePanel extends JPanel implements SearchListener {
    private static final long serialVersionUID = -209897295836797500L;

    private static final String DEFAULT_FILENAME = "Untitled.pep";

    public static final Font DEFAULT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private MainFrame parent;

    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private FindToolBar findToolBar;
    private ReplaceToolBar replaceToolBar;
    private TextEditorPane srcTextArea;
    private Gutter srcGutter;
    private TextEditorPane resTextArea;
    private Gutter resGutter;
    private StatusBar statusBar;
    private CollapsibleSectionPanel csp;
    private JTabbedPane tabs;

    private JFileChooser chooser;

    private Action showFindBarAction;
    private Action showReplaceBarAction;
    private Action hideSearchBarAction;
    private Action showFindDialogAction;
    private Action showReplaceDialogAction;
    private Action goToLineAction;
    private Action buildAction;
    private Action runAction;
    private Action debugAction;
    private Action stepAction;
    private Action backAction;
    private Action newAction;
    private Action openDialogAction;
    private Action saveAction;
    private Action saveAsDialogAction;

    private RTextScrollPane resultPane;

    private Map<Integer, Integer> addressMap;
    private Map<Integer, Integer> lineMap;

    public SourcePanel(MainFrame parent, String name) {
        this.parent = parent;

        this.chooser = new JFileChooser();
        var pepFilter = new FileNameExtensionFilter("Pep source", "pep");
        var dclFilter = new FileNameExtensionFilter("DeCLan source", "dcl");
        chooser.setFileFilter(pepFilter);
        chooser.addChoosableFileFilter(dclFilter);

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

        var file = new File(chooser.getCurrentDirectory(), DEFAULT_FILENAME);
        try {
            srcTextArea = new TextEditorPane(TextEditorPane.INSERT_MODE, false, FileLocation.create(file));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        srcTextArea.setRows(25);
        srcTextArea.setColumns(120);
        srcTextArea.setFont(DEFAULT_FONT);
        srcTextArea.setPaintTabLines(true);
        srcTextArea.setMarkOccurrences(true);

        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/pep10", "edu.depauw.dep10.ui.Pep10TokenMaker");
        atmf.putMapping("text/declan", "edu.depauw.dep10.ui.DeCLanTokenMaker");
        srcTextArea.setSyntaxEditingStyle("text/pep10");
        RTextScrollPane sp = new RTextScrollPane(srcTextArea);
        csp.add(sp);

        ErrorStrip errorStrip = new ErrorStrip(srcTextArea);
        csp.add(errorStrip, BorderLayout.LINE_END);

        resTextArea = new TextEditorPane(TextEditorPane.INSERT_MODE, false);
        resTextArea.setRows(25);
        resTextArea.setColumns(120);
        resTextArea.setFont(DEFAULT_FONT);
        resTextArea.setPaintTabLines(true);
        resTextArea.setMarkOccurrences(true);
        resTextArea.setSyntaxEditingStyle("text/pep10");
        resTextArea.setText("This\nis\na\ntest.");
        resTextArea.setEditable(false);
        resultPane = new RTextScrollPane(resTextArea);

        tabs = new JTabbedPane(JTabbedPane.RIGHT);
        tabs.add("Source", csp);
        tabs.add("Result", resultPane);
        this.add(tabs);

        srcGutter = sp.getGutter();
        resGutter = resultPane.getGutter();

        URL imgURL = getClass().getResource("/images/breakpoint.png");
        if (imgURL != null) {
            var breakIcon = new ImageIcon(imgURL);
            srcGutter.setBookmarkIcon(breakIcon);
            resGutter.setBookmarkIcon(breakIcon);
            srcGutter.setBookmarkingEnabled(true);
            resGutter.setBookmarkingEnabled(true);
        }

        updateTitle();

        srcTextArea.addPropertyChangeListener(TextEditorPane.DIRTY_PROPERTY, evt -> {
            updateTitle();
        });

        srcTextArea.addPropertyChangeListener(TextEditorPane.FULL_PATH_PROPERTY, evt -> {
            updateTitle();
        });

        statusBar = new StatusBar();
        this.add(statusBar, BorderLayout.SOUTH);
    }

    private void updateTitle() {
        var title = (srcTextArea.isDirty() ? "*" : "") + srcTextArea.getFileName();
        parent.updateTitle(title);

        if (srcTextArea.getFileName().equals(DEFAULT_FILENAME) || !srcTextArea.isDirty()) {
            getSaveAction().setEnabled(false);
        } else {
            getSaveAction().setEnabled(true);
        }
    }

    private List<Integer> getSrcBreakpoints() {
        var icons = srcGutter.getBookmarks();
        List<Integer> result = new ArrayList<>();

        try {
            for (int i = 0; i < icons.length; i++) {
                var info = icons[i];
                result.add(srcTextArea.getLineOfOffset(info.getMarkedOffset()));
            }
        } catch (BadLocationException e) {
            // Ignore this
        }

        return result;
    }

    private List<Integer> getResBreakpoints() {
        var icons = resGutter.getBookmarks();
        List<Integer> result = new ArrayList<>();

        try {
            for (int i = 0; i < icons.length; i++) {
                var info = icons[i];
                var lineNumber = resTextArea.getLineOfOffset(info.getMarkedOffset());
                result.add(addressMap.get(lineNumber));
            }
        } catch (BadLocationException e) {
            // Ignore this
        }

        return result;
    }

    private void synchronizeBreakpoints(Result result) {
        resGutter.removeAllTrackingIcons();

        var breakpoints = getSrcBreakpoints();
        var lineMap = result.getSourceMap("<string>"); // TODO change this for DeCLan source...
        for (var bp : breakpoints) {
            if (lineMap.containsKey(bp + 1)) {
                var line = lineMap.get(bp + 1);
                try {
                    resGutter.toggleBookmark(line);
                } catch (BadLocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        this.addressMap = result.getAddressMap();
        this.lineMap = result.getLineMap();
    }

    private static class StatusBar extends JPanel {
        private static final long serialVersionUID = -8132584109632782731L;

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
            result = SearchEngine.markAll(srcTextArea, context);
            break;

        case FIND:
            result = SearchEngine.find(srcTextArea, context);
            if (!result.wasFound() || result.isWrapped()) {
                UIManager.getLookAndFeel().provideErrorFeedback(srcTextArea);
            }
            break;

        case REPLACE:
            result = SearchEngine.replace(srcTextArea, context);
            if (!result.wasFound() || result.isWrapped()) {
                UIManager.getLookAndFeel().provideErrorFeedback(srcTextArea);
            }
            break;

        case REPLACE_ALL:
            result = SearchEngine.replaceAll(srcTextArea, context);
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
        return srcTextArea.getSelectedText();
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
        private static final long serialVersionUID = 3984884489642219739L;

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
        private static final long serialVersionUID = 8712440621830110429L;

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
        private static final long serialVersionUID = -4692244454848794539L;

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
            dialog.setMaxLineNumberAllowed(srcTextArea.getLineCount());
            dialog.setVisible(true);
            int line = dialog.getLineNumber();
            if (line > 0) {
                try {
                    srcTextArea.setCaretPosition(srcTextArea.getLineStartOffset(line - 1));
                } catch (BadLocationException ble) { // Never happens
                    UIManager.getLookAndFeel().provideErrorFeedback(srcTextArea);
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

    private class BuildAction extends AbstractAction {
        private static final long serialVersionUID = 7293647898019218237L;

        private OutputPanel symbols;
        private OutputPanel object;

        public BuildAction(OutputPanel symbols, OutputPanel object) {
            super("Build");
            this.symbols = symbols;
            this.object = object;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            var result = parent.getSourceType().build(srcTextArea.getText(), symbols, object, resTextArea);
            if (!result.hasErrors()) {
                setStopped();
                parent.selectSymbolsTab();
                synchronizeBreakpoints(result);
                resTextArea.setCaretPosition(0);
                tabs.setSelectedComponent(resultPane);
            } else {
                setBuildNeeded();
            }
        }
    }

    public Action getBuildAction(OutputPanel listing, OutputPanel object) {
        if (buildAction == null) {
            buildAction = new BuildAction(listing, object);
        }

        return buildAction;
    }

    private class RunAction extends AbstractAction {
        private static final long serialVersionUID = -1140970514699364890L;

        public RunAction() {
            super("Run");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            var control = parent.getController();

            if (control == null) {
                setRunning();
                parent.getSourceType().run(parent);
                parent.selectTerminalTab();

                // TODO errors, ...
            } else {
                control.end();
                setStopped();

                parent.setController(null);
            }
        }
    }

    public Action getRunAction() {
        if (runAction == null) {
            runAction = new RunAction();
        }

        return runAction;
    }

    private class DebugAction extends AbstractAction {
        private static final long serialVersionUID = -6131375880019259398L;

        public DebugAction() {
            super("Debug");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            var control = parent.getController();

            if (control == null) {
                setDebugging();
                parent.getSourceType().debug(parent, getResBreakpoints());
                parent.selectStateTab();
            } else {
                if (control.isPaused()) {
                    setDebugging();
                    control.resume(parent, getResBreakpoints());
                } else {
                    control.pause();
                    setPaused();
                }
            }
        }
    }

    public Action getDebugAction() {
        if (debugAction == null) {
            debugAction = new DebugAction();
        }

        return debugAction;
    }

    private class StepAction extends AbstractAction {
        private static final long serialVersionUID = 8039510471073829841L;

        public StepAction() {
            super("Step");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            var control = parent.getController();
            control.forward(parent);
        }
    }

    public Action getStepAction() {
        if (stepAction == null) {
            stepAction = new StepAction();
        }

        return stepAction;
    }

    private class BackAction extends AbstractAction {
        private static final long serialVersionUID = 2222542354555992822L;

        public BackAction() {
            super("Back");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            var control = parent.getController();
            control.backward(parent);
        }
    }

    public Action getBackAction() {
        if (backAction == null) {
            backAction = new BackAction();
        }

        return backAction;
    }

    private class NewAction extends AbstractAction {
        private static final long serialVersionUID = 7217391752823374519L;

        public NewAction() {
            super("New");
            var key = KeyStroke.getKeyStroke(KeyEvent.VK_N, getToolkit().getMenuShortcutKeyMaskEx());
            this.putValue(ACCELERATOR_KEY, key);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (srcTextArea.isDirty()) {
                var result = JOptionPane.showConfirmDialog(parent, "Save current file?");
                if (result == JOptionPane.CANCEL_OPTION) {
                    return;
                } else if (result == JOptionPane.YES_OPTION) {
                    try {
                        srcTextArea.save();
                    } catch (IOException e1) {
                        // TODO display error message?
                        return;
                    }
                }
            }

            try {
                var newFile = new File(chooser.getCurrentDirectory(), DEFAULT_FILENAME);
                srcTextArea.load(FileLocation.create(newFile));
                srcGutter.removeAllTrackingIcons();
                setBuildNeeded();
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
        private static final long serialVersionUID = -2250962913688302159L;

        public OpenDialogAction() {
            super("Open...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                try {
                    srcTextArea.load(FileLocation.create(chooser.getSelectedFile()));
                    srcGutter.removeAllTrackingIcons();

                    if (chooser.getSelectedFile().getName().endsWith(".dcl")) {
                        srcTextArea.setSyntaxEditingStyle("text/declan"); // TODO use constants
                        parent.setSourceType(SourceType.DeCLan);
                    } else {
                        srcTextArea.setSyntaxEditingStyle("text/pep10");

                        // Attempt to guess SourceType from content
                        String content = srcTextArea.getText();
                        if (content.contains(".SECTION")) {
                            parent.setSourceType(SourceType.Pep10System);
                        } else if (content.contains("@DECO") || content.contains("@STRO")) {
                            parent.setSourceType(SourceType.Pep10UserFull);
                        } else {
                            parent.setSourceType(SourceType.Pep10UserBare);
                        }
                    }

                    setBuildNeeded();
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
        private static final long serialVersionUID = -272434131007284377L;

        public SaveAction() {
            super("Save");
            var key = KeyStroke.getKeyStroke(KeyEvent.VK_S, getToolkit().getMenuShortcutKeyMaskEx());
            this.putValue(ACCELERATOR_KEY, key);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                srcTextArea.save();
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
        private static final long serialVersionUID = -5848229817626747864L;

        public SaveAsDialogAction() {
            super("Save As...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                try {
                    srcTextArea.saveAs(FileLocation.create(chooser.getSelectedFile()));
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
        if (srcTextArea.isDirty()) {
            var result = JOptionPane.showConfirmDialog(parent, "Save before quit?");
            if (result == JOptionPane.CANCEL_OPTION) {
                return false;
            } else if (result == JOptionPane.YES_OPTION) {
                try {
                    srcTextArea.save();
                } catch (IOException e) {
                    // TODO display error message?
                    return false;
                }
            }
        }

        return true;
    }

    public Font getPanelFont() {
        return srcTextArea.getFont();
    }

    public void setPanelFont(Font font) {
        if (font != null) {
            SyntaxScheme ss = srcTextArea.getSyntaxScheme();
            ss = (SyntaxScheme) ss.clone();
            for (int i = 0; i < ss.getStyleCount(); i++) {
                if (ss.getStyle(i) != null) {
                    ss.getStyle(i).font = font;
                }
            }
            srcTextArea.setSyntaxScheme(ss);
            srcTextArea.setFont(font);
            resTextArea.setSyntaxScheme(ss);
            resTextArea.setFont(font);
        }
    }

    public void setBuildNeeded() {
        runAction.putValue(Action.NAME, "Run");
        runAction.setEnabled(false);
        debugAction.putValue(Action.NAME, "Debug");
        debugAction.setEnabled(false);
        stepAction.setEnabled(false);
        backAction.setEnabled(false);
    }

    public void setRunning() {
        runAction.putValue(Action.NAME, "End");
        runAction.setEnabled(true);
        debugAction.putValue(Action.NAME, "Debug");
        debugAction.setEnabled(false);
        stepAction.setEnabled(false);
        backAction.setEnabled(false);
    }

    public void setDebugging() {
        runAction.putValue(Action.NAME, "End");
        runAction.setEnabled(true);
        debugAction.putValue(Action.NAME, "Pause");
        debugAction.setEnabled(true);
        stepAction.setEnabled(false);
        backAction.setEnabled(false);
    }

    public void setStopped() {
        runAction.putValue(Action.NAME, "Run");
        runAction.setEnabled(true);
        debugAction.putValue(Action.NAME, "Debug");
        debugAction.setEnabled(true);
        stepAction.setEnabled(false);
        backAction.setEnabled(false);
    }

    public void setPaused() {
        runAction.putValue(Action.NAME, "End");
        runAction.setEnabled(true);
        debugAction.putValue(Action.NAME, "Resume");
        debugAction.setEnabled(true);
        stepAction.setEnabled(true);
        backAction.setEnabled(true);
    }

    public void showLine(int pc) {
        Integer line = lineMap.get(pc);
        if (line != null) {
            try {
                var offset = resTextArea.getLineStartOffset(line);
                resTextArea.setCaretPosition(offset);
            } catch (BadLocationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public ActionListener getSourceTypeListener() {
        return e -> {
            @SuppressWarnings("unchecked")
            JComboBox<SourceType> b = (JComboBox<SourceType>) e.getSource();
            if (b.getSelectedItem().equals(SourceType.DeCLan)) {
                srcTextArea.setSyntaxEditingStyle("text/declan");
            } else {
                srcTextArea.setSyntaxEditingStyle("text/pep10");
            }
        };
    }
}
