package edu.depauw.dep10.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

import org.fife.ui.rtextarea.RTextArea;

import com.formdev.flatlaf.util.SystemInfo;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    static final String APP_TITLE = "DeP10 IDE";

    private SourcePanel source;
    private OutputPanel listing;
    private OutputPanel object;
    private TerminalPanel terminal;
    private JComboBox<SourceType> sourceType;
    private JTabbedPane tabs;

    public MainFrame() {
        setSize(800, 600);
        setTitle(APP_TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        source = new SourcePanel(this, "source");

        tabs = new JTabbedPane(JTabbedPane.TOP);

        listing = new OutputPanel("listing");
        tabs.add(listing.getTitle(), listing);

        object = new OutputPanel("object");
        tabs.add(object.getTitle(), object);

        terminal = new TerminalPanel("term");
        tabs.add(terminal.getTitle(), terminal);

        var split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, source, tabs);
        this.add(split, BorderLayout.CENTER);

        var tools = new JToolBar(JToolBar.HORIZONTAL);

        SourceType[] sourceTypes = new SourceType[] {
                SourceType.Pep10UserFull,
                SourceType.Pep10UserBare,
                SourceType.Pep10System,
                SourceType.DeCLan
        };
        sourceType = new JComboBox<>(sourceTypes);
        sourceType.setMaximumSize(sourceType.getPreferredSize());
        tools.add(sourceType);
        tools.add(new JToolBar.Separator());

        var menuBar = createMenuBar(tools);
        setJMenuBar(menuBar);

        this.add(tools, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
        split.setDividerLocation(0.5);
    }

    public JMenuBar createMenuBar(JToolBar tools) {
        var menuBar = new JMenuBar();

        // File Menu
        var fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem(source.getNewAction()));
        fileMenu.add(new JMenuItem(source.getOpenDialogAction()));
        fileMenu.add(new JMenuItem(source.getSaveAction()));
        fileMenu.add(new JMenuItem(source.getSaveAsDialogAction()));
        menuBar.add(fileMenu);

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
        searchMenu.add(new JMenuItem(source.getShowFindDialogAction())); // TODO createMenuItem?
        searchMenu.add(new JMenuItem(source.getShowReplaceDialogAction()));
        searchMenu.add(new JMenuItem(source.getGoToLineAction()));
        searchMenu.addSeparator();
        searchMenu.add(new JMenuItem(source.getShowFindBarAction()));
        searchMenu.add(new JMenuItem(source.getShowReplaceBarAction()));
        searchMenu.add(new JMenuItem(source.getHideSearchBarAction()));
        menuBar.add(searchMenu);

        // Build Menu
        var buildMenu = new JMenu("Build");
        var build = source.getAssembleAction(listing, object);
        buildMenu.add(createMenuItem(build));
        tools.add(new JButton(build));
        menuBar.add(buildMenu);

        // Simulator Menu
        var simulatorMenu = new JMenu("Simulator");
        
        var run = source.getRunAction(object, terminal);
        run.setEnabled(false); // not enabled until assembly successful
        simulatorMenu.add(createMenuItem(run));
        tools.add(new JButton(run));
        
        var debug = source.getDebugAction(object, terminal);
        debug.setEnabled(false);
        simulatorMenu.add(createMenuItem(debug));
        tools.add(new JButton(debug));
        
        menuBar.add(simulatorMenu);

        // Help Menu
        var helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        if (!SystemInfo.isMacOS) {
            fileMenu.addSeparator();
            fileMenu.add(new JMenuItem(new AbstractAction("Exit") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    MainFrame.this.dispose(); // TODO check whether it is OK to close
                }
            }));

            editMenu.addSeparator();
            editMenu.add(new JMenuItem("Preferences..."));

            helpMenu.addSeparator();
            helpMenu.add(new JMenuItem("About..."));
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

    public void selectListingTab() {
        tabs.setSelectedComponent(listing);
    }

    public void selectTerminalTab() {
        tabs.setSelectedComponent(terminal);
    }

    public void updateTitle(String fileName) {
        setTitle(fileName + " - " + APP_TITLE);
    }

    public boolean canQuit() {
        return source.canQuit();
    }

    public SourceType getSourceType() {
        return sourceType.getItemAt(sourceType.getSelectedIndex());
    }
}