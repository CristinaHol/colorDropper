package workspace.core;

import javax.swing.JComponent;

	public class ComponentConnections{
        private final JComponent source;
        private final JComponent target;

        ComponentConnections(JComponent source, JComponent target){
            this.source = source;
            this.target = target;

        }

        public JComponent getSource(){
        	return source;
        }

        public JComponent getTarget(){
        	return target;
        }
    }