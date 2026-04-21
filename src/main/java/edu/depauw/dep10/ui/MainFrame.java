package edu.depauw.dep10.ui;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

import com.formdev.flatlaf.util.SystemInfo;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    public MainFrame() {
        setSize(400, 300);
        setTitle("Text Editor Demo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var tabs = new JTabbedPane();
        this.add(tabs);

        var helloWorld = new SourcePanel("hello");
        tabs.add(helloWorld.getTitle(), helloWorld);
        
        var output = new OutputPanel("output");
        tabs.add(output.getTitle(), output);
        output.setContent("This is a demo");

        pack();
        setLocationRelativeTo(null);
        
        var menuBar = createMenuBar(helloWorld);
        
        setJMenuBar(menuBar); 
    }

    public JMenuBar createMenuBar(SourcePanel source) {
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
        var editMenu = source.getEditMenu();
        menuBar.add(editMenu);
        
        // Search Menu
        var searchMenu = new JMenu("Search");
        menuBar.add(searchMenu);
        
        var findMenuItem = new JMenuItem("Find...");
        searchMenu.add(findMenuItem);
        
        var replaceMenuItem = new JMenuItem("Replace...");
        searchMenu.add(replaceMenuItem);
        
        var gotoMenuItem = new JMenuItem("Go To Line...");
        searchMenu.add(gotoMenuItem);
        
        searchMenu.addSeparator();
        
        var findBarMenuItem = new JMenuItem("Show Find Search Bar");
        searchMenu.add(findBarMenuItem);

        var replaceBarMenuItem = new JMenuItem("Show Replace Search Bar");
        searchMenu.add(replaceBarMenuItem);
        
        var hideMenuItem = new JMenuItem("Hide Search Bar");
        searchMenu.add(hideMenuItem);
        
        // Build Menu
        var buildMenu = source.getBuildMenu();
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
}