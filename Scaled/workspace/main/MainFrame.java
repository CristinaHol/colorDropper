package workspace.main;

import workspace.color.ColorWorkspacePanel;
import workspace.docs.AuthorDocPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

public class MainFrame extends JFrame {
    private File currentFile = null;
    private JTabbedPane rootTabbedPane;
    private SystemTabbedPane systemTabbedPane;

    public MainFrame() {
        setTitle("Multi-Tab Drag & Drop Project");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        rootTabbedPane = new JTabbedPane();

        // Tab 1: Author Documentation
        rootTabbedPane.addTab("Author Documentation", new AuthorDocPanel());

        // Tab 2: System Tabbed Pane (housing sub-programs like Color Workspaces)
        systemTabbedPane = new SystemTabbedPane();
        rootTabbedPane.addTab("System Workspaces", systemTabbedPane);
        
        // Help / Info Button
        JButton infoButton = new JButton("i");
        infoButton.setToolTipText("Help & Instructions");
        infoButton.setFont(new Font("Serif", Font.BOLD, 14));
        infoButton.setPreferredSize(new Dimension(15, 15));

        infoButton.addActionListener(e -> {
            String instructions = "<html><b><font size='4'>Workspace User Guide</font></b><br><br>" +
                "<b>• Drag Swatch:</b> Click and drag any color swatch to move it.<br>" +
                "<b>• Create Connection:</b> Hold <b>Shift</b> and drag from a source swatch to a target swatch.<br>" +
                "<b>• Edit Properties:</b> Right-click a swatch to open the text box and change its opacity.<br>" +
                "<b>• Delete Element:</b> Double-click any swatch or connection line to delete it.<br>" +
                "<b>• Save / Load:</b> Use the bottom buttons to persist your setup to XML.</html>";
                
            JOptionPane.showMessageDialog(this, instructions, "Information", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel tabHeaderWrapper = new JPanel(new BorderLayout());
        tabHeaderWrapper.add(rootTabbedPane, BorderLayout.CENTER);
        
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        buttonWrapper.add(infoButton);
        tabHeaderWrapper.add(buttonWrapper, BorderLayout.NORTH);

        // Bottom Control Panel (Save / Load)
        JPanel controlPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton loadButton = new JButton("Load from XML");
        
        saveButton.addActionListener(e -> {
            // Determine starting directory: use last active file's parent folder, or fall back to project working directory
            File startingDir = (currentFile != null) ? currentFile.getParentFile() : new File(System.getProperty("user.dir"));
            JFileChooser fileChooser = new JFileChooser(startingDir);
            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                saveAllTabsToXML(currentFile);
            }
        });

        loadButton.addActionListener(e -> {
            // Determine starting directory: use last active file's parent folder, or fall back to project working directory
            File startingDir = (currentFile != null) ? currentFile.getParentFile() : new File(System.getProperty("user.dir"));
            JFileChooser fileChooser = new JFileChooser(startingDir);
            
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                loadAllTabsFromXML(currentFile);
            }
        });

        controlPanel.add(saveButton);
        controlPanel.add(loadButton);

        add(tabHeaderWrapper, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    public void saveAllTabsToXML(File file) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("MultiTabWorkspace");
            doc.appendChild(rootElement);

            Component comp0 = rootTabbedPane.getComponentAt(0);
            if (comp0 instanceof AuthorDocPanel) {
                AuthorDocPanel authorPanel = (AuthorDocPanel) comp0;
                Element authorElem = doc.createElement("AuthorMetadata");
                authorElem.setAttribute("name", authorPanel.getAuthorName());
                authorElem.setAttribute("date", authorPanel.getDateModified());
                rootElement.appendChild(authorElem);
            }

            // Delegate saving of system sub-programs to SystemTabbedPane
            systemTabbedPane.saveSystemTabsToXML(doc, rootElement);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        
            JOptionPane.showMessageDialog(this, "Workspace successfully saved to XML", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadAllTabsFromXML(File file) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            AuthorDocPanel authorPanel = new AuthorDocPanel();
            NodeList authorList = doc.getElementsByTagName("AuthorMetadata");
            if (authorList.getLength() > 0) {
                Element authorElem = (Element) authorList.item(0);
                authorPanel.setAuthorName(authorElem.getAttribute("name"));
                authorPanel.setDateModified(authorElem.getAttribute("date"));
            }

            // Re-initialize the system tabbed pane to clear old tabs
            rootTabbedPane.removeAll();
            rootTabbedPane.addTab("Author Documentation", authorPanel);
            systemTabbedPane = new SystemTabbedPane();
            
            // Remove all existing sub-tabs and load fresh from XML
            systemTabbedPane.removeAll();

            NodeList tabList = doc.getElementsByTagName("Tab");
            for (int i = 0; i < tabList.getLength(); i++) {
                Node node = tabList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element tabElem = (Element) node;
                    String title = tabElem.getAttribute("title");
                    systemTabbedPane.loadSystemTabsFromXML(tabElem, title);
                }
            }

            rootTabbedPane.addTab("System Workspaces", systemTabbedPane);
            currentFile = file;
            JOptionPane.showMessageDialog(this, "Workspace successfully loaded from XML!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading XML: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}