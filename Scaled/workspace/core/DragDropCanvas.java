package workspace.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class DragDropCanvas extends JPanel{
        protected final List<JComponent> canvasComponents = new ArrayList<>();
        protected final List<ComponentConnections> connections = new ArrayList<>();
        
        protected JComponent connectionSource = null;
        protected Point dragCurrentPoint = null;

        public DragDropCanvas(){
            setLayout(null);
            setBackground(Color.LIGHT_GRAY);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        // Check if they clicked on a connection line
                        ComponentConnections clickedConn = findConnectionAt(e.getPoint());
                        if (clickedConn != null) {
                            JPopupMenu menu = new JPopupMenu();
                            JMenuItem deleteItem = new JMenuItem("Delete Connection");
                            deleteItem.addActionListener(ev -> removeConnection(clickedConn));
                            menu.add(deleteItem);
                            menu.show(DragDropCanvas.this, e.getX(), e.getY());
                        }
                    }
                }
            });
        }

        public void addConnection(JComponent source, JComponent target){
            if(source != target){
                for(ComponentConnections conn : connections){
                    if(conn.getSource() == source && conn.getTarget() == target){
                        return;
                    }
                }
                connections.add(new ComponentConnections(source, target));
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
            
            for(ComponentConnections conn : connections){
                Point p1 = getCenterPoint(conn.getSource());
                Point p2 = getCenterPoint(conn.getTarget());
                drawArrow(g2d, p1.x, p1.y, p2.x, p2.y);
            }

            if(connectionSource != null && dragCurrentPoint != null){
                Point p1 = getCenterPoint(connectionSource);
                g2d.setColor(Color.BLUE);
                drawArrow(g2d, p1.x, p1.y, dragCurrentPoint.x, dragCurrentPoint.y);
            }
            g2d.dispose();
        }

        protected Point getCenterPoint(JComponent swatch){
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

        public JComponent findSnapTarget(Point p, JComponent sourceComp){
            int snapRadius = 25;
            for(Component comp : getComponents()){
                if(comp instanceof JComponent && comp != sourceComp){
                    Rectangle bounds = comp.getBounds();
                    bounds.grow(snapRadius, snapRadius);
                    if(bounds.contains(p)){
                        return (JComponent) comp;
                    }
                }
            }
            return null;
        }

        // Remove a specific swatch and any connections attached to it
        public void removeConnection(ComponentConnections conn){
        	connections.remove(conn);
        	repaint();
        }

        public void removeComponent(JComponent comp){
        	connections.removeIf(conn -> conn.getSource() == comp || conn.getTarget() == comp);
        	remove(comp);
        	canvasComponents.remove(comp);
        	revalidate();
        	repaint();
        }

        // Check if a point is close to a line segment (useful for clicking a connection)
        public ComponentConnections findConnectionAt(Point p) {
            int tolerance = 8; // click tolerance in pixels
            for (ComponentConnections conn : connections) {
                Point p1 = getCenterPoint(conn.getSource());
                Point p2 = getCenterPoint(conn.getTarget());
                
                // Calculate distance from point p to line segment (p1, p2)
                double dist = Line2D.ptSegDist(p1.x, p1.y, p2.x, p2.y, p.x, p.y);
                if (dist <= tolerance) {
                    return conn;
                }
            }
            return null;
        }


        public void makeInteractive(JComponent comp){
            MouseAdapter dragAdapter = new MouseAdapter(){
                private Point initialMouseParent;
                private Point initialCompLoc;

                @Override
                public void  mousePressed(MouseEvent e){
                    if(e.getClickCount() == 2){
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem deleteItem = new JMenuItem("Delete Swatch");
                        deleteItem.addActionListener(ev -> removeComponent(comp));
                        menu.add(deleteItem);
                        menu.show(comp, e.getX(), e.getY());
                        return;
                    }

                    if (SwingUtilities.isRightMouseButton(e)) {
                        onRightClick(comp, e);
                        return;
                    }
                    

                    if(e.isShiftDown()){
                        connectionSource = comp;
                        dragCurrentPoint = SwingUtilities.convertPoint(comp, e.getPoint(), DragDropCanvas.this);
                    } else{
                    initialMouseParent = SwingUtilities.convertPoint(comp, e.getPoint(), DragDropCanvas.this);
                    initialCompLoc = comp.getLocation();
                    setComponentZOrder(comp, 0);
                    }


                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e){
                    if(connectionSource == comp){
                        dragCurrentPoint = SwingUtilities.convertPoint(comp, e.getPoint(), DragDropCanvas.this);
                        repaint();
                    } else if(initialMouseParent != null && initialCompLoc != null){
                        Point parentPoint = SwingUtilities.convertPoint(comp, e.getPoint(), DragDropCanvas.this);

                        int dx = parentPoint.x - initialMouseParent.x;
                        int dy = parentPoint.y - initialMouseParent.y;


                        int newX = initialCompLoc.x + dx;
                        int newY = initialCompLoc.y + dy;

                        newX = Math.max(0, Math.min(newX, getWidth() - comp.getWidth()));
                        newY = Math.max(0, Math.min(newY, getHeight() - comp.getHeight()));

                        comp.setLocation(newX, newY);
                        repaint();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e){
                    if(connectionSource == comp){
                        Point dropPoint = SwingUtilities.convertPoint(comp, e.getPoint(), DragDropCanvas.this);
                        JComponent targetComp = findSnapTarget(dropPoint, comp);

                        if(targetComp != null){
                            
                            addConnection(comp, targetComp);
                        }
                        

                        connectionSource = null;
                        dragCurrentPoint = null;
                        repaint();
                    }
                }

            };

            comp.addMouseListener(dragAdapter);
            comp.addMouseMotionListener(dragAdapter);
        }

        protected void onRightClick(JComponent comp, MouseEvent e){}


    }
