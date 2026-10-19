import javax.swing.*;
import java.awt.*;

public class MainProjectFrame extends JFrame{
	public MainProjectFrame(){
		setTitle("Multi-Tab Drag & Drop Project");
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		JTabbedPane tabbedPane = new JTabbedPane();

		tabbedPane.addTab("Workspace 1", new ColorWorkspacePanel());
		Color[] tab2Colors = {Color.BLUE, Color.RED, Color.MAGENTA};
		tabbedPane.addTab("Workspace 2", new ColorWorkspacePanel(tab2Colors));
		Color[] tab3Colors = {Color.ORANGE, Color.GREEN};
		tabbedPane.addTab("Workspace 3", new ColorWorkspacePanel(tab3Colors));

		add(tabbedPane);
	}

	public static void main(String[] args){
		SwingUtilities.invokeLater(() -> new MainProjectFrame().setVisible(true));
	}
}