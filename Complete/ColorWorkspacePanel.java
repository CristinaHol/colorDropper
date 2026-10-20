import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import javax.xml.transform.dom.DOMSource;
import java.awt.geom.Line2D;

public class ColorWorkspacePanel extends JPanel {

    

    public static final DataFlavor COLOR_FLAVOR = new DataFlavor(Color.class, "Java Color Object");

    private final DropCanvasPanel dropTargetPanel;
    private Color[] customColors;
    JPanel palettePanel;

    public ColorWorkspacePanel(){
        this(new Color[] {Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA});
    }

    public ColorWorkspacePanel(Color[] customColors) {
       this.customColors = customColors;
        setLayout(new BorderLayout(10, 10));

        dropTargetPanel = new DropCanvasPanel();
        dropTargetPanel.setTransferHandler(new ColorDropHandler(dropTargetPanel));
      
        
        palettePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        palettePanel.setBorder(BorderFactory.createTitledBorder("Drag a Color Swatch - Hold shift to add a connection - Right click to delete"));

                
        for (Color color : customColors) {
            palettePanel.add(createColorSwatch(color));
        }

        add(palettePanel, BorderLayout.NORTH);
        add(dropTargetPanel, BorderLayout.CENTER);
        
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


     public static class SwatchConnection{
        final JPanel source;
        final JPanel target;

        SwatchConnection(JPanel source, JPanel target){
            this.source = source;
            this.target = target;

        }
    }


    private static class DropCanvasPanel extends JPanel{
        private final List<JPanel> swatches = new ArrayList<>();
        private final List<SwatchConnection> connections = new ArrayList<>();
        private JPanel connectionSource = null;
        Point dragCurrentPoint = null;

        public DropCanvasPanel(){
            setLayout(null);
            setBackground(Color.LIGHT_GRAY);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        // Check if they clicked on a connection line
                        SwatchConnection clickedConn = findConnectionAt(e.getPoint());
                        if (clickedConn != null) {
                            JPopupMenu menu = new JPopupMenu();
                            JMenuItem deleteItem = new JMenuItem("Delete Connection");
                            deleteItem.addActionListener(ev -> removeConnection(clickedConn));
                            menu.add(deleteItem);
                            menu.show(DropCanvasPanel.this, e.getX(), e.getY());
                        }
                    }
                }
            });
        }

        public void addConnection(JPanel source, JPanel target){
            if(source != target){
                for(SwatchConnection conn : connections){
                    if(conn.source == source && conn.target == target){
                        return;
                    }
                }
                connections.add(new SwatchConnection(source, target));
            }
            repaint();
        }
        
        
        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.setColor(Color.BLACK);
            
            for(SwatchConnection conn : connections){
                Point p1 = getCenterPoint(conn.source);
                Point p2 = getCenterPoint(conn.target);
                drawArrow(g2d, p1.x, p1.y, p2.x, p2.y);
            }

            if(connectionSource != null && dragCurrentPoint != null){
                Point p1 = getCenterPoint(connectionSource);
                g2d.setColor(Color.BLUE);
                drawArrow(g2d, p1.x, p1.y, dragCurrentPoint.x, dragCurrentPoint.y);
            }
            g2d.dispose();
        }

        private Point getCenterPoint(JPanel swatch){
            Rectangle r = swatch.getBounds();
            return new Point(r.x +r.width / 2, r.y +r.height / 2);
        }

        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2){
            double angle = Math.atan2(y2-y1, x2-x1);
            int arrowSize = 12;

            g2.drawLine(x1,y1,x2,y2);

            int x3 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI/ 6));
            int y3 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI/ 6));
            int x4 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI/ 6));
            int y4 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI/ 6));

            g2.fillPolygon(new int[]{x2,x3,x4}, new int[]{y2,y3,y4}, 3);
        }

        public JPanel findSnapTarget(Point p, JPanel sourceSwatch){
            int snapRadius = 25;
            for(Component comp : getComponents()){
                if(comp instanceof JPanel && comp != sourceSwatch){
                    Rectangle bounds = comp.getBounds();
                    bounds.grow(snapRadius, snapRadius);
                    if(bounds.contains(p)){
                        return (JPanel) comp;
                    }
                }
            }
            return null;
        }

        // Remove a specific swatch and any connections attached to it
        public void removeSwatch(JPanel swatch) {
            // Remove any connections referencing this swatch
            connections.removeIf(conn -> conn.source == swatch || conn.target == swatch);
            // Remove from panel and list
            remove(swatch);
            swatches.remove(swatch);
            revalidate();
            repaint();
        }

        // Check if a point is close to a line segment (useful for clicking a connection)
        public SwatchConnection findConnectionAt(Point p) {
            int tolerance = 8; // click tolerance in pixels
            for (SwatchConnection conn : connections) {
                Point p1 = getCenterPoint(conn.source);
                Point p2 = getCenterPoint(conn.target);
                
                // Calculate distance from point p to line segment (p1, p2)
                double dist = Line2D.ptSegDist(p1.x, p1.y, p2.x, p2.y, p.x, p.y);
                if (dist <= tolerance) {
                    return conn;
                }
            }
            return null;
        }

        public void removeConnection(SwatchConnection conn) {
            connections.remove(conn);
            repaint();
        }


        public void makeInteractive(JPanel swatch, DropCanvasPanel parentPanel){
            MouseAdapter dragAdapter = new MouseAdapter(){
                private Point initialMouseParent;
                private Point initialSwatchLoc;

                @Override
                public void  mousePressed(MouseEvent e){
                    if (SwingUtilities.isRightMouseButton(e)) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem deleteItem = new JMenuItem("Delete Swatch");
                        deleteItem.addActionListener(ev -> parentPanel.removeSwatch(swatch));
                        menu.add(deleteItem);
                        menu.show(swatch, e.getX(), e.getY());
                        return;
                    }

                    if(e.isShiftDown()){
                        parentPanel.connectionSource = swatch;
                        parentPanel.dragCurrentPoint = SwingUtilities.convertPoint(swatch, e.getPoint(), parentPanel);
                    } else{
                    initialMouseParent = SwingUtilities.convertPoint(swatch, e.getPoint(), parentPanel);
                    initialSwatchLoc = swatch.getLocation();
                    parentPanel.setComponentZOrder(swatch, 0);
                    }
                    parentPanel.repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e){
                    if(parentPanel.connectionSource == swatch){
                        parentPanel.dragCurrentPoint = SwingUtilities.convertPoint(swatch, e.getPoint(), parentPanel);
                        parentPanel.repaint();
                    } else if(initialMouseParent != null && initialSwatchLoc != null){
                        Point parentPoint = SwingUtilities.convertPoint(swatch, e.getPoint(), parentPanel);

                        int dx = parentPoint.x - initialMouseParent.x;
                        int dy = parentPoint.y - initialMouseParent.y;


                        int newX = initialSwatchLoc.x + dx;
                        int newY = initialSwatchLoc.y + dy;

                        newX = Math.max(0, Math.min(newX, parentPanel.getWidth() - swatch.getWidth()));
                        newY = Math.max(0, Math.min(newY, parentPanel.getHeight() - swatch.getHeight()));

                        swatch.setLocation(newX, newY);
                        parentPanel.repaint();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e){
                    if(parentPanel.connectionSource == swatch){
                        Point dropPoint = SwingUtilities.convertPoint(swatch, e.getPoint(), parentPanel);
                        JPanel targetSwatch = parentPanel.findSnapTarget(dropPoint, swatch);

                        if(targetSwatch != null){
                            
                            parentPanel.addConnection(swatch, targetSwatch);
                        }
                        

                        parentPanel.connectionSource = null;
                        parentPanel.dragCurrentPoint = null;
                        parentPanel.repaint();
                    }
                }

            };

            swatch.addMouseListener(dragAdapter);
            swatch.addMouseMotionListener(dragAdapter);
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

      
        private final DropCanvasPanel targetPanel;
        private static final int SWATCH_SIZE = 40;

        public ColorDropHandler(DropCanvasPanel targetPanel){
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

                if(dropComponent != targetPanel){
                    dropPoint = SwingUtilities.convertPoint(dropComponent, dropPoint, targetPanel);
                }
               

                
                JPanel droppedSwatch = new JPanel();
                droppedSwatch.setBackground(newColor);
                droppedSwatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                // this centers it around thepoint it was dropped on
                int x = dropPoint.x - (SWATCH_SIZE / 2);
                int y = dropPoint.y - (SWATCH_SIZE/2);
                droppedSwatch.setBounds(x,y,SWATCH_SIZE,SWATCH_SIZE);

               targetPanel.makeInteractive(droppedSwatch, targetPanel);
                targetPanel.add(droppedSwatch);
                targetPanel.swatches.add(droppedSwatch);
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
            List<JPanel> swatchList = dropTargetPanel.swatches;

            for(JPanel swatch : swatchList){
                    Element swatchElem = doc.createElement("Swatch");
                    swatchElem.setAttribute("color", getColorName(swatch.getBackground()));
                    swatchElem.setAttribute("x", String.valueOf(swatch.getX()));
                    swatchElem.setAttribute("y", String.valueOf(swatch.getY()));
                    swatchesElement.appendChild(swatchElem);
            }

            Element connectionsElement = doc.createElement("Connections");
            tabElement.appendChild(connectionsElement);

            for(SwatchConnection conn : dropTargetPanel.connections ){
                int sourceIdx = swatchList.indexOf(conn.source);
                int targetIdx = swatchList.indexOf(conn.target);

                if(sourceIdx != -1 && targetIdx != -1){
                    Element connElem = doc.createElement("Connection");
                    connElem.setAttribute("sourceIndex", String.valueOf(sourceIdx));
                    connElem.setAttribute("targetIdx", String.valueOf(targetIdx));
                    connectionsElement.appendChild(connElem);

                }
            }
    }
    public void loadTabFromXML(Element tabElement){
        dropTargetPanel.removeAll();
        dropTargetPanel.connections.clear();
        dropTargetPanel.swatches.clear();


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
                if(palettePanel != null){
                    remove(palettePanel);
                }
                palettePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
                palettePanel.setBorder(BorderFactory.createTitledBorder("Drag a Color Swatch"));
                for (Color color : customColors) {
                    palettePanel.add(createColorSwatch(color));
                }
                add(palettePanel, BorderLayout.NORTH);
            }
        }
        NodeList swatchList = tabElement.getElementsByTagName("Swatch");
            for(int i = 0; i < swatchList.getLength(); i++){
                Node node = swatchList.item(i);
                if(node.getNodeType() == Node.ELEMENT_NODE){
                    Element element = (Element) node;
                    String colorName = element.getAttribute("color");
                    int x = Integer.parseInt(element.getAttribute("x"));
                    int y = Integer.parseInt(element.getAttribute("y"));

                    Color color = parseColorName(colorName);
                    JPanel swatch = new JPanel();
                    swatch.setBackground(color);
                    swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    swatch.setBounds(x,y,40,40);

                    dropTargetPanel.makeInteractive(swatch, dropTargetPanel);
                    dropTargetPanel.add(swatch);
                    dropTargetPanel.swatches.add(swatch);

                }
            }

            NodeList connList = tabElement.getElementsByTagName("Connection");
            for (int i = 0; i < connList.getLength(); i++) {
                Node node = connList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    int sourceIdx = Integer.parseInt(element.getAttribute("sourceIndex"));
                    int targetIdx = Integer.parseInt(element.getAttribute("targetIdx"));

                    if (sourceIdx < dropTargetPanel.swatches.size() && targetIdx < dropTargetPanel.swatches.size()) {
                        JPanel sourceSwatch = dropTargetPanel.swatches.get(sourceIdx);
                        JPanel targetSwatch = dropTargetPanel.swatches.get(targetIdx);
                        dropTargetPanel.addConnection(sourceSwatch, targetSwatch);
                    }
                }
            }
            dropTargetPanel.revalidate();
            dropTargetPanel.repaint();
    }
    private String getColorName(Color color) {
        if (color.equals(Color.RED)) return "RED";
        if (color.equals(Color.BLUE)) return "BLUE";
        if (color.equals(Color.GREEN)) return "GREEN";
        if (color.equals(Color.ORANGE)) return "ORANGE";
        if (color.equals(Color.MAGENTA)) return "MAGENTA";
        return "BLACK";
    }

    private Color parseColorName(String name) {
        switch (name) {
            case "RED": return Color.RED;
            case "BLUE": return Color.BLUE;
            case "GREEN": return Color.GREEN;
            case "ORANGE": return Color.ORANGE;
            case "MAGENTA": return Color.MAGENTA;
            default: return Color.BLACK;
        }
    }

}
   
