package workspace.color;

import workspace.core.ComponentConnections;
import workspace.core.DragDropCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.*;

public class ColorWorkspacePanel extends DragDropCanvas {
    public static final DataFlavor COLOR_FLAVOR = new DataFlavor(Color.class, "Java Color Object");
    
    private Color[] customColors;
    JPanel palettePanel;
    private static final int PALETTE_HEIGHT = 90;

    public ColorWorkspacePanel(){
        this(new Color[] {Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA});
    }

    public ColorWorkspacePanel(Color[] customColors) {
        super(); // DragDropCanvas sets setLayout(null) by default
        this.customColors = customColors;

        palettePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        palettePanel.setBorder(BorderFactory.createTitledBorder("Drag a Color Swatch - Hold shift to add a connection - Right click to delete"));

        for (Color color : customColors) {
            palettePanel.add(createColorSwatch(color));
        }

        // Position the palette panel at the top of the null-layout canvas
        palettePanel.setBounds(0, 0, 900, PALETTE_HEIGHT);
        add(palettePanel);

        // Attach the drop handler directly to this canvas panel
        setTransferHandler(new ColorDropHandler(this));
    }

    // Automatically keep the palette panel spanning the full width when the window/panel is resized
    @Override
    public void doLayout() {
        super.doLayout();
        if (palettePanel != null) {
            palettePanel.setBounds(0, 0, getWidth(), PALETTE_HEIGHT);
        }
    }

    private JPanel createColorSwatch(Color color) {
        JPanel swatch = new JPanel();
        swatch.setBackground(color);
        swatch.setPreferredSize(new Dimension(50, 50));
        swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        swatch.setTransferHandler(new ColorSourceHandler(color));
       
        swatch.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                JComponent comp = (JComponent) e.getSource();
                TransferHandler handler = comp.getTransferHandler();
                handler.exportAsDrag(comp, e, TransferHandler.COPY);
            }
        });

        return swatch;
    }

    @Override
    protected void onRightClick(JComponent comp, MouseEvent e){
        if(!(comp instanceof JPanel)) return;
        JPanel swatch = (JPanel) comp;
        Color currentColor = swatch.getBackground();

        String colorName = getColorName(currentColor);

        if("RED".equals(colorName) || "ORANGE".equals(colorName)){
            String input = JOptionPane.showInputDialog(this, 
                "Enter Alpha/Transparency value (0-255):", 
                String.valueOf(swatch.getBackground().getAlpha()));
            
            if (input != null && !input.trim().isEmpty()) {
                try {
                    int alpha = Integer.parseInt(input.trim());
                    alpha = Math.max(0, Math.min(255, alpha)); 
                    
                    Color current = swatch.getBackground();
                    Color newColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), alpha);
                    
                    swatch.setBackground(newColor);
                    repaint();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if("BLUE".equals(colorName)){
            openTableEditor(swatch);
        } else {
            JOptionPane.showMessageDialog(this, "This component is read-only and cannot be edited.", "Locked", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static class ColorSourceHandler extends TransferHandler {
        private final Color color;

        public ColorSourceHandler(Color color) {
            this.color = color;
        }
        
        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }
            
        @Override
        protected Transferable createTransferable(JComponent c) {
            return new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{ COLOR_FLAVOR };
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return COLOR_FLAVOR.equals(flavor);
                }
                
                @Override
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                    if (isDataFlavorSupported(flavor)) {
                        return color;
                    }
                    throw new UnsupportedFlavorException(flavor);
                }
            };
        }
    }

    private static class ColorDropHandler extends TransferHandler {
        private final ColorWorkspacePanel targetPanel;
        private static final int SWATCH_SIZE = 40;

        public ColorDropHandler(ColorWorkspacePanel targetPanel){
            this.targetPanel = targetPanel;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(COLOR_FLAVOR);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                Color newColor = (Color) support.getTransferable().getTransferData(COLOR_FLAVOR);
                Point dropPoint = support.getDropLocation().getDropPoint();
                Component dropComponent = support.getComponent();

                // Convert drop coordinates relative to the target panel
                if(dropComponent != targetPanel){
                    dropPoint = SwingUtilities.convertPoint(dropComponent, dropPoint, targetPanel);
                }

                JPanel droppedSwatch = new JPanel();
                droppedSwatch.setBackground(newColor);
                droppedSwatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                
                // Center swatch around drop point and enforce 40x40 size
                int x = dropPoint.x - (SWATCH_SIZE / 2);
                int y = dropPoint.y - (SWATCH_SIZE / 2);

                if (y < PALETTE_HEIGHT) {
                    y = PALETTE_HEIGHT + 5;
                }
                droppedSwatch.setBounds(x, y, SWATCH_SIZE, SWATCH_SIZE);

                // Add directly to targetPanel (DragDropCanvas) so connections and swatches share coordinates
                targetPanel.makeInteractive(droppedSwatch);
                targetPanel.add(droppedSwatch);
                targetPanel.canvasComponents.add(droppedSwatch);
                targetPanel.setComponentZOrder(droppedSwatch, 0);
                targetPanel.revalidate();
                targetPanel.repaint();

                return true; 
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
                return false; 
            }
        }
    }

    public void saveTabToXML(Document doc, Element tabElement){
        Element paletteElement = doc.createElement("Palette");
        tabElement.appendChild(paletteElement);

        for(Color color : customColors){
            Element colorElem = doc.createElement("Color");
            colorElem.setAttribute("name", getColorName(color));
            paletteElement.appendChild(colorElem);
        }

        Element swatchesElement = doc.createElement("Swatches");
        tabElement.appendChild(swatchesElement);
        List<JComponent> swatchList = canvasComponents;

        for (JComponent comp : swatchList) {
            if (!(comp instanceof JPanel)) continue;
            JPanel swatch = (JPanel) comp;
            Element swatchElem = doc.createElement("Swatch");
            swatchElem.setAttribute("color", getColorName(swatch.getBackground()));
            swatchElem.setAttribute("x", String.valueOf(swatch.getX()));
            swatchElem.setAttribute("y", String.valueOf(swatch.getY()));
            String colorName = getColorName(swatch.getBackground());
            
            if ("RED".equals(colorName) || "ORANGE".equals(colorName)) {
                Element propElem = doc.createElement("property");
                propElem.setAttribute("name", "alpha");
                Element valueElem = doc.createElement("value");
                valueElem.setTextContent(String.valueOf(swatch.getBackground().getAlpha()));
                propElem.appendChild(valueElem);
                swatchElem.appendChild(propElem);
            } else if ("BLUE".equals(colorName)) {
                Element propElem = doc.createElement("property");
                propElem.setAttribute("name", "coefficient_table");
                Element tableElem = doc.createElement("table");
                
                Object[][] tableData = (Object[][]) swatch.getClientProperty("tableData");
                if (tableData != null) {
                    tableElem.setAttribute("rows", String.valueOf(tableData.length));
                    for (Object[] row : tableData) {
                        Element rowElem = doc.createElement("row");
                        rowElem.setAttribute("x", String.valueOf(row[0]));
                        rowElem.setAttribute("y", String.valueOf(row[1]));
                        tableElem.appendChild(rowElem);
                    }
                }
                propElem.appendChild(tableElem);
                swatchElem.appendChild(propElem);
            }
            
            swatchesElement.appendChild(swatchElem);
        }

        Element connectionsElement = doc.createElement("Connections");
        tabElement.appendChild(connectionsElement);

        for (ComponentConnections conn : connections) {
            int sourceIdx = swatchList.indexOf(conn.getSource());
            int targetIdx = swatchList.indexOf(conn.getTarget());

            if (sourceIdx != -1 && targetIdx != -1) {
                Element connElem = doc.createElement("Connection");
                connElem.setAttribute("sourceIndex", String.valueOf(sourceIdx));
                connElem.setAttribute("targetIdx", String.valueOf(targetIdx));
                connectionsElement.appendChild(connElem);
            }
        }
    }  

    public void loadTabFromXML(Element tabElement) {
        removeAll();
        connections.clear();
        canvasComponents.clear();

        NodeList paletteList = tabElement.getElementsByTagName("Palette");
        if (paletteList.getLength() > 0) {
            Element paletteElem = (Element) paletteList.item(0);
            NodeList colorNodeList = paletteElem.getElementsByTagName("Color");
            List<Color> loadedColors = new ArrayList<>();
            for (int i = 0; i < colorNodeList.getLength(); i++) {
                Node node = colorNodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element colorElem = (Element) node;
                    loadedColors.add(parseColorName(colorElem.getAttribute("name")));
                }
            }
            if (!loadedColors.isEmpty()) {
                this.customColors = loadedColors.toArray(new Color[0]);
            }
        }

        palettePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        palettePanel.setBorder(BorderFactory.createTitledBorder("Drag a Color Swatch"));
        for (Color color : customColors) {
            palettePanel.add(createColorSwatch(color));
        }
        palettePanel.setBounds(0, 0, getWidth() > 0 ? getWidth() : 900, PALETTE_HEIGHT);
        add(palettePanel);
        setTransferHandler(new ColorDropHandler(this));

        NodeList swatchList = tabElement.getElementsByTagName("Swatch");
        for (int i = 0; i < swatchList.getLength(); i++) {
            Node node = swatchList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String colorName = element.getAttribute("color");
                int x = Integer.parseInt(element.getAttribute("x"));
                int y = Integer.parseInt(element.getAttribute("y"));

                int alpha = 255;
                Object[][] loadedTableData = null;
                NodeList propertyList = element.getElementsByTagName("property");
                for (int j = 0; j < propertyList.getLength(); j++) {
                    Element propElem = (Element) propertyList.item(j);
                    if ("alpha".equals(propElem.getAttribute("name"))) {
                        NodeList valueList = propElem.getElementsByTagName("value");
                        if (valueList.getLength() > 0) {
                            alpha = Integer.parseInt(valueList.item(0).getTextContent().trim());
                        }
                    } else if ("coefficient_table".equals(propElem.getAttribute("name"))) {
                        NodeList rowList = propElem.getElementsByTagName("row");
                        loadedTableData = new Object[rowList.getLength()][2];
                        for (int r = 0; r < rowList.getLength(); r++) {
                            Element rowElem = (Element) rowList.item(r);
                            loadedTableData[r][0] = rowElem.getAttribute("x");
                            loadedTableData[r][1] = rowElem.getAttribute("y");
                        }
                    }
                }

                Color baseColor = parseColorName(colorName);
                Color color = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);

                JPanel swatch = new JPanel();
                swatch.setBackground(color);
                swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                swatch.setBounds(x, y, 40, 40);

                if (loadedTableData != null) {
                    swatch.putClientProperty("tableData", loadedTableData);
                }

                makeInteractive(swatch);
                add(swatch);
                canvasComponents.add(swatch);
            }
        }

        NodeList connList = tabElement.getElementsByTagName("Connection");
        for (int i = 0; i < connList.getLength(); i++) {
            Node node = connList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                int sourceIdx = Integer.parseInt(element.getAttribute("sourceIndex"));
                int targetIdx = Integer.parseInt(element.getAttribute("targetIdx"));

                if (sourceIdx < canvasComponents.size() && targetIdx < canvasComponents.size()) {
                    JComponent sourceSwatch = canvasComponents.get(sourceIdx);
                    JComponent targetSwatch = canvasComponents.get(targetIdx);
                    addConnection(sourceSwatch, targetSwatch);
                }
            }
        }
        revalidate();
        repaint();
    }

    private static String getColorName(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        
        if (r == Color.RED.getRed() && g == Color.RED.getGreen() && b == Color.RED.getBlue()) return "RED";
        if (r == Color.BLUE.getRed() && g == Color.BLUE.getGreen() && b == Color.BLUE.getBlue()) return "BLUE";
        if (r == Color.GREEN.getRed() && g == Color.GREEN.getGreen() && b == Color.GREEN.getBlue()) return "GREEN";
        if (r == Color.ORANGE.getRed() && g == Color.ORANGE.getGreen() && b == Color.ORANGE.getBlue()) return "ORANGE";
        if (r == Color.MAGENTA.getRed() && g == Color.MAGENTA.getGreen() && b == Color.MAGENTA.getBlue()) return "MAGENTA";
        return "BLACK";
    }

    private static Color parseColorName(String name) {
        switch (name) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "ORANGE": return Color.ORANGE;
            case "MAGENTA": return Color.MAGENTA;
            default: return Color.BLACK;
        }
    }

    private void openTableEditor(JPanel swatch) {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        CoefficientTableDialog dialog = new CoefficientTableDialog(parentFrame, swatch);
        dialog.setVisible(true);
    }
}