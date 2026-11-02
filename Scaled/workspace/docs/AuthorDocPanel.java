package workspace.docs;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AuthorDocPanel extends JPanel{
	private final JTextField nameField;
    private final JTextField dateField;

	public AuthorDocPanel(){
		setLayout(new BorderLayout(15, 15));
		setBorder(BorderFactory.createEmptyBorder(15,20,20,20));

		setBackground(Color.LIGHT_GRAY);
		JPanel headerPanel = new JPanel(new BorderLayout(0, 10));
        headerPanel.setOpaque(false);
		JLabel titleLabel = new JLabel("Project Documentation & Author Guide", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(new Color(40, 40, 40));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        JPanel metaFormPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        metaFormPanel.setOpaque(false);
        metaFormPanel.setBorder(BorderFactory.createTitledBorder("Document Metadata"));
        // Name Text Box
        JLabel nameLabel = new JLabel("Author Name:");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        nameField = new JTextField(15);

        // Date Text Box (Defaults to current date)
        JLabel dateLabel = new JLabel("Date Modified:");
        dateLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        String currentDateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        dateField = new JTextField(currentDateStr, 10);

        metaFormPanel.add(nameLabel);
        metaFormPanel.add(nameField);
        metaFormPanel.add(dateLabel);
        metaFormPanel.add(dateField);

        headerPanel.add(metaFormPanel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Documentation Content Body
        JEditorPane docContentPane = new JEditorPane();
        docContentPane.setContentType("text/html");
        docContentPane.setEditable(false);
        docContentPane.setBackground(Color.WHITE);
        
        String htmlContent = "<html>" +
            "<body style='font-family: Arial, sans-serif; color: #333333; margin: 15px;'>" +
            "<h2 style='color: #1a73e8; border-bottom: 2px solid #1a73e8; padding-bottom: 5px;'>Overview</h2>" +
            "<p>Welcome to the <b>Multi-Tab Drag & Drop Workspace</b> application. This modular desktop software features multiple interactive workspaces, custom drag-and-drop components, dynamic relational connection mapping, and XML data persistence.</p>" +
            
            "<h2 style='color: #1a73e8; border-bottom: 2px solid #1a73e8; padding-bottom: 5px;'>User Guide & Controls</h2>" +
            "<ul style='line-height: 1.6;'>" +
            "  <li><b>Drag Swatch:</b> Click and drag any color swatch from the palette onto the canvas workspace.</li>" +
            "  <li><b>Create Connection:</b> Hold down the <b>Shift</b> key and drag from a source component to a target component to draw a directional relationship arrow.</li>" +
            "  <li><b>Edit Properties:</b> Right-click a component swatch to modify its properties (e.g., adjust Red/Orange alpha transparency values or open the Blue coefficient gradient table editor).</li>" +
            "  <li><b>Delete Elements:</b> Double-click any active workspace swatch or right-click a connection line to delete it.</li>" +
            "  <li><b>Save & Load:</b> Use the control buttons at the bottom to persist your entire multi-tab workspace layout to an XML file.</li>" +
            "</ul>" +

            "<h2 style='color: #1a73e8; border-bottom: 2px solid #1a73e8; padding-bottom: 5px;'>Architecture & Packages</h2>" +
            "<p>The application is organized using a clean, modular package hierarchy:</p>" +
            "<ul style='line-height: 1.6;'>" +
            "  <li><b><code>workspace.core</code>:</b> Houses the foundational drag-and-drop canvas engine and connection vector logic.</li>" +
            "  <li><b><code>workspace.color</code>:</b> Manages the Color Drop program, custom swatches, alpha values, and table property editors.</li>" +
            "  <li><b><code>workspace.docs</code>:</b> Contains project documentation and author notes.</li>" +
            "  <li><b><code>workspace.stickers</code>:</b> Reserved for future stamp/sticker workspace modules.</li>" +
            "  <li><b><code>workspace.main</code>:</b> Manages the primary window frame, navigation tabs, and global XML serialization.</li>" +
            "</ul>" +
            
            "<br><hr style='border: none; border-top: 1px solid #dddddd;'>" +
            "<p style='font-size: 11px; color: #777777; text-align: center;'>Developed with Java Swing & Maven Architecture</p>" +
            "</body></html>";

        docContentPane.setText(htmlContent);
        docContentPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(docContentPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        add(scrollPane, BorderLayout.CENTER);
    }

    // Getters in case you want to serialize the name & date to XML later
    public String getAuthorName() {
        return nameField.getText();
    }

    public void setAuthorName(String name) {
        nameField.setText(name);
    }

    public String getDateModified() {
        return dateField.getText();
    }

    public void setDateModified(String dateStr) {
        dateField.setText(dateStr);
    
	}
}