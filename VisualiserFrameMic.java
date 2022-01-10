package mic;

import javax.swing.JFrame;

public class VisualiserFrameMic extends JFrame {
	private static final long serialVersionUID = 1L;

	VisualiserPanelMic visualiserPanel;
	
	VisualiserFrameMic(){
		
		visualiserPanel = new VisualiserPanelMic();
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.add(visualiserPanel);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		
	}

}

