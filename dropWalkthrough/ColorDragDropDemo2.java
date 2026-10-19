import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * This version allows users to drop the swatches into the 
 * drop zone in the position they want
 * */

public class ColorDragDropDemo2 extends JFrame {

    /** 
     * DataFlavor defines the "type" or "format" o fdata being transferred during drag and drop
     * Standar flavors exist for text or files, but custom java objects use JVM local object transfer.
     * This tells swing: we are transferring an in-memory java.awt.Color object. 
     * */
    public static final DataFlavor COLOR_FLAVOR = 
        new DataFlavor(Color.class, "Java Color Object");

    public ColorDragDropDemo2() {
        // -- 1. WIndow Setup __
        setTitle("Swing Drag and Drop - Color Palette Sample");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centers frame on the screen
        setLayout(new BorderLayout(10, 10));

        // -- 2. Drop Target Area --
        // This panel will recieve the color when dropped
        /** ----- KEY CHANGE ONE ------
         * remove the flowlayout because you want to specify the point at which the user 
         * drops the swatch
        */

        JPanel dropTargetPanel = new JPanel();
        dropTargetPanel.setLayout(null);
        dropTargetPanel.setBackground(Color.LIGHT_GRAY);
        JLabel targetLabel = new JLabel("Drop Color Here", SwingConstants.CENTER);
        targetLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
      

        // Attach a TransferHandler to handle receiving/accepting incoming drops
        dropTargetPanel.setTransferHandler(new ColorDropHandler(dropTargetPanel));

        // --- 3. Drag Source Palette Area
        // Container for the selectable color swatches
        JPanel palettePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        palettePanel.setBorder(BorderFactory.createTitledBorder("Drag a Color Swatch"));

        //Generate swatched for each color option
        Color[] colors = { Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA };
        for (Color color : colors) {
            palettePanel.add(createColorSwatch(color));
        }

        //Add both main panels to the fram
        add(palettePanel, BorderLayout.NORTH);
        add(dropTargetPanel, BorderLayout.CENTER);
    }

    /**
     * Creates an individual colored panel (swatch) configured to support drag initiation
     */

    private JPanel createColorSwatch(Color color) {
        JPanel swatch = new JPanel();
        swatch.setBackground(color);
        swatch.setPreferredSize(new Dimension(50, 50));
        swatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        // Attach a custom TransferHandler that knows how to package this specific color
        swatch.setTransferHandler(new ColorSourceHandler(color));
        // Dragging doesn't start automatically; we listen for a mouse press to trigger the export
        swatch.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                JComponent comp = (JComponent) e.getSource();
                TransferHandler handler = comp.getTransferHandler();

                // trigers the swing drag-and-drop mechanism in Copy mode
                handler.exportAsDrag(comp, e, TransferHandler.COPY);
            }
        });

        return swatch;
    }

    /**
     * Drag source handle
     * Responsible for packaging data into a transferable container when a drag begins
     * */
    private static class ColorSourceHandler extends TransferHandler {
        private final Color color;

        public ColorSourceHandler(Color color) {
            this.color = color;
        }


        /**
         * Defines allowed actions(COPY MOVE LINK)
         * COPY keeps the source color intact on the swatch panel after drag completion.
         * */
        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }
       
        /**
         * Wraps the payload (the color object) inside a transferable interface,
         * which swing usees behind the scenes during the drag process
         * */

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new Transferable() {

                //Indicates which formats/flavoors this payload supports
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{ COLOR_FLAVOR };
                }
                //checks if a requested dataflavor matches our custom color flavor
                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return COLOR_FLAVOR.equals(flavor);
                }
                // returns the actual payload(the color object) if requesting in a matching flavor
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


    /**
     * Drop target handler:
     * responsible for evaluating dragged incoming data and processing the drop.
     * */
    private static class ColorDropHandler extends TransferHandler {

        /**
         * Called continuously while dragging over the target.
         * returns true if the target accepts the dragged dataflavot (shows green light cursor
         * */
        private final JPanel targetPanel;
        private static final int SWATCH_SIZE = 40;

        public ColorDropHandler(JPanel targetPanel){
            this.targetPanel = targetPanel;
        }


        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(COLOR_FLAVOR);
        }

        /**
         * Executed when the mouse button is released over the drop targer
         * extracts the data payload and updates the ui
         * */

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
               

                //3. Create a new color square swatch
                JPanel droppedSwatch = new JPanel();
                droppedSwatch.setBackground(newColor);
                droppedSwatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                // this centers it around thepoint it was dropped on
                int x = dropPoint.x - (SWATCH_SIZE / 2);
                int y = dropPoint.y - (SWATCH_SIZE/2);
                droppedSwatch.setBounds(x,y,SWATCH_SIZE,SWATCH_SIZE);

                // 4. Add the new swatch to the target panel
                targetPanel.add(droppedSwatch);

                targetPanel.setComponentZOrder(droppedSwatch, 0);

                // 5. Revalidate and repaint so swing redrawws the new component immediately
                targetPanel.revalidate();
                targetPanel.repaint();

                return true; // return true to indicate successful drop handling
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
                return false; // return false if transfer fails
            }
        }
    }

    public static void main(String[] args) {
        // run ui
        SwingUtilities.invokeLater(() -> {
            new ColorDragDropDemo2().setVisible(true);
        });
    }
}