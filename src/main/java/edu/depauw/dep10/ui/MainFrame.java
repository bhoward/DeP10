package edu.depauw.dep10.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

import org.fife.ui.rtextarea.RTextArea;

import com.formdev.flatlaf.util.SystemInfo;

// Largely based on RSTAUIDemoApp
@SuppressWarnings("serial")
public class MainFrame extends JFrame {    
    private SourcePanel source;
    private OutputPanel listing;
    private OutputPanel object;
    private TerminalPanel terminal;

    public MainFrame() {
        setSize(400, 300);
        setTitle("DeP10 IDE");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var tabs = new JTabbedPane();
        this.add(tabs);

        source = new SourcePanel(this, "source");
        tabs.add(source.getTitle(), source);

        listing = new OutputPanel("listing");
        tabs.add(listing.getTitle(), listing);
        
        object = new OutputPanel("object");
        tabs.add(object.getTitle(), object);
        
        terminal = new TerminalPanel("term");
        tabs.add(terminal.getTitle(), terminal);

        pack();
        setLocationRelativeTo(null);

        var menuBar = createMenuBar();
        setJMenuBar(menuBar);
    }

    public JMenuBar createMenuBar() {
        var menuBar = new JMenuBar();

        // File Menu
        var fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem("New"));
        fileMenu.add(new JMenuItem("Open..."));
        fileMenu.add(new JMenuItem("Save"));
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
        buildMenu.add(createMenuItem(source.getAssembleAction(listing, object)));
        menuBar.add(buildMenu);

        // Debug Menu
        var debugMenu = new JMenu("Debug");
        menuBar.add(debugMenu);

        // Simulator Menu
        var simulatorMenu = new JMenu("Simulator");
        simulatorMenu.add(createMenuItem(source.getRunAction(object, terminal)));
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
}