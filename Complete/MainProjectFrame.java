import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

public class MainProjectFrame extends JFrame{
		private File currentFile = null;
		JTabbedPane tabbedPane;
	public MainProjectFrame(){
		setTitle("Multi-Tab Drag & Drop Project");
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		tabbedPane = new JTabbedPane();

		tabbedPane.addTab("Workspace 1", new ColorWorkspacePanel());
		Color[] tab2Colors = {Color.BLUE, Color.RED, Color.MAGENTA};
		tabbedPane.addTab("Workspace 2", new ColorWorkspacePanel(tab2Colors));
		Color[] tab3Colors = {Color.ORANGE, Color.GREEN};
		tabbedPane.addTab("Workspace 3", new ColorWorkspacePanel(tab3Colors));
		
        JButton infoButton = new JButton("i");
        infoButton.setToolTipText("Help & Instructions");
        infoButton.setFont(new Font("Serif", Font.BOLD, 14));
        infoButton.setPreferredSize(new Dimension(15,15));

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
        tabHeaderWrapper.add(tabbedPane, BorderLayout.CENTER);
        
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        buttonWrapper.add(infoButton);
        tabHeaderWrapper.add(buttonWrapper, BorderLayout.EAST);

        JPanel controlPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton loadButton = new JButton("Load from XML");
        
        saveButton.addActionListener(e -> {
            if(currentFile != null){
                saveAllTabsToXML(currentFile);
            } else {
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    currentFile = fileChooser.getSelectedFile();
                    saveAllTabsToXML(currentFile);
                }
            }
        });


        loadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
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

	public void saveAllTabsToXML(File file){

        try{
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("MultiTabWorkspace");
            doc.appendChild(rootElement);

            for(int i = 0; i < tabbedPane.getTabCount();i++){
            	String tabTitle = tabbedPane.getTitleAt(i);
                ColorWorkspacePanel panel = (ColorWorkspacePanel) tabbedPane.getComponentAt(i);

                Element tabElem = doc.createElement("Tab");
                tabElem.setAttribute("title", tabTitle);
                rootElement.appendChild(tabElem);

                panel.saveTabToXML(doc, tabElem);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        
            JOptionPane.showMessageDialog(this, "workspace successfully saved to xml", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch(Exception e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "error saving" + e.getMessage(), "error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadAllTabsFromXML(File file){
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            tabbedPane.removeAll();

            NodeList tabList = doc.getElementsByTagName("Tab");
            for (int i = 0; i < tabList.getLength(); i++) {
                Node node = tabList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element tabElem = (Element) node;
                    String title = tabElem.getAttribute("title");

                    ColorWorkspacePanel panel = new ColorWorkspacePanel();
                    panel.loadTabFromXML(tabElem);

                    tabbedPane.addTab(title, panel);
                }
            }
            
            currentFile = file;
            
            JOptionPane.showMessageDialog(this, "Workspace successfully loaded from XML!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading XML: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        
    }

	public static void main(String[] args){
		SwingUtilities.invokeLater(() -> new MainProjectFrame().setVisible(true));
	}
}