package workspace.color;

import javax.swing.*;
import java.awt.*;

public class CoefficientTableDialog extends JDialog {

    public CoefficientTableDialog(Frame parent, JPanel swatch) {
        super(parent, "Coefficient / Gradient Workspace", true);
        setSize(350, 300);
        setLocationRelativeTo(parent);

        String[] columnNames = {"Coefficient X", "Coefficient Y"};
        
        // 1. Retrieve saved table data from the swatch's client property, or fall back to defaults
        Object[][] data = (Object[][]) swatch.getClientProperty("tableData");
        if (data == null) {
            data = new Object[][] {
                { "0.0", "1.0" },
                { "0.5", "0.5" },
                { "1.0", "0.2" }
            };
        }

        JTable table = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(table);
        
        JButton saveButton = new JButton("Apply Changes");
        saveButton.addActionListener(e -> {
            // 2. Stop any active cell editing so the final value is captured
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }

            int rowCount = table.getRowCount();
            int colCount = table.getColumnCount();
            Object[][] updatedDataMatrix = new Object[rowCount][colCount];

            // 3. Extract the data row by row from the table model
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    updatedDataMatrix[row][col] = table.getValueAt(row, col);
                }
            }

            // 4. Store it back into the swatch's client property
            swatch.putClientProperty("tableData", updatedDataMatrix);

            // 5. Close the dialog
            dispose();
        });

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(saveButton, BorderLayout.SOUTH);
    }
}