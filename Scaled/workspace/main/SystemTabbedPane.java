package workspace.main;

import workspace.color.ColorWorkspacePanel;
import javax.swing.*;
import java.awt.*;

public class SystemTabbedPane extends JTabbedPane{

	public SystemTabbedPane(){
		super(JTabbedPane.TOP);

		addTab("Color Workspace 1", new ColorWorkspacePanel());
		Color[] tab2Colors = {Color.BLUE, Color.RED, Color.MAGENTA};
        addTab("Color Workspace 2", new ColorWorkspacePanel(tab2Colors));
        
        Color[] tab3Colors = {Color.ORANGE, Color.GREEN};
        addTab("Color Workspace 3", new ColorWorkspacePanel(tab3Colors));
	}

	public void saveSystemTabsToXML(org.w3c.dom.Document doc, org.w3c.dom.Element rootElement) {
        for (int i = 0; i < getTabCount(); i++) {
            String tabTitle = getTitleAt(i);
            Component comp = getComponentAt(i);

            if (comp instanceof ColorWorkspacePanel) {
                ColorWorkspacePanel panel = (ColorWorkspacePanel) comp;

                org.w3c.dom.Element tabElem = doc.createElement("Tab");
                tabElem.setAttribute("title", tabTitle);
                rootElement.appendChild(tabElem);

                panel.saveTabToXML(doc, tabElem);
            }
        }
    }
    
    public void loadSystemTabsFromXML(org.w3c.dom.Element tabElem, String title) {
        ColorWorkspacePanel panel = new ColorWorkspacePanel();
        panel.loadTabFromXML(tabElem);
        addTab(title, panel);
    }

}