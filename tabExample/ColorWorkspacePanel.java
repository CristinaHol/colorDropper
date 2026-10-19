import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This version allows users to drop the swatches into the 
 * drop zone in the position they want and adds the arrow connection function
 * There is a major version change from 2 to 3 as swing does not support the arrow
 * functions natively so you will need to make a custom panel for the drop panel to
 * support the arrows
 * */

public class ColorWorkspacePanel extends JPanel {

    
    public static final DataFlavor COLOR_FLAVOR = 
        new DataFlavor(Color.class, "Java Color Object");

    private final DropCanvasPanel dropTargetPanel;

    public ColorWorkspacePanel(){
        this(new Color[] {Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA});
    }

    public ColorWorkspacePanel(Color[] customColors) {
       
        setLayout(new BorderLayout(10, 10));

        dropTargetPanel = new DropCanvasPanel();
        dropTargetPanel.setTransferHandler(new ColorDropHandler(dropTargetPanel));

        
        JPanel palettePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        palettePanel.setBorder(BorderFactory.createTitledBorder("Drag a Color Swatch"));

        
        
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

    // ------- KEY ADDITIONS -------

    private static class DropCanvasPanel extends JPanel{
        private final List<SwatchConnection> connections = new ArrayList<>();
        private JPanel connectionSource = null;
        Point dragCurrentPoint = null;

        public DropCanvasPanel(){
            setLayout(null);
            setBackground(Color.LIGHT_GRAY);
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
        
        /**
         * Overrides the paint lifecycle to render two types of arrows
         * established connections, live preview
         * */
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
    }

    private static class SwatchConnection{
        final JPanel source;
        final JPanel target;

        SwatchConnection(JPanel source, JPanel target){
            this.source = source;
            this.target = target;

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
                // 1. unpackage the color object from the drag payload
                Color newColor = (Color) support.getTransferable().getTransferData(COLOR_FLAVOR);
                /**
                 * ------ KEY CHANGE TWO ------
                 * This is where the information is gotten for the point at which the 
                 * user drops the swatch
                 * a component is made for the reason that you want to be able to make 
                 * another swatch if the user drops it on another existing swatch or if
                 * there is anything like labels made that are for the dropping it still 
                 * works
                 * */
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

               makeInteractive(droppedSwatch, targetPanel);

               
                targetPanel.add(droppedSwatch);

                targetPanel.setComponentZOrder(droppedSwatch, 0);

                
                targetPanel.revalidate();
                targetPanel.repaint();

                return true; 
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
                return false; 
            }
        }

        /**
         * ========================================
         * VERSION 3 INTERACTION UPDATE: Dual-mode mouse Handling
         * ========================================
         * upgrades this into dual-mode workflor handled by modifer keys
         * 
         * - Regular drag: if shift not pressed, it calculates position deltas and updates the swatch bounds
         * 
         * - Shift + Drag: "Connection mode". It records a connectionsource, renders
         * a live blue preview line during 'mouseDragged', and checks
         * for a valid target via 'findSnapTarget' upon "mouseReleased".
         * 
         * */
        private void makeInteractive(JPanel swatch, DropCanvasPanel parentPanel){
        MouseAdapter dragAdapter = new MouseAdapter(){
            private Point initialMouseParent;
            private Point initialSwatchLoc;

            @Override
            public void  mousePressed(MouseEvent e){
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
                    }else{
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

   
}