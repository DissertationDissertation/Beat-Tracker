package mic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public class VisualiserPanelMic extends JPanel{
	private static final long serialVersionUID = 1L;

	final int PANEL_WIDTH = 500;
	final int PANEL_HEIGHT = 500;
	public  int circleWidth = 100;
	public  int circleHeight = 100;

	VisualiserPanelMic() {
		this.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
		this.setBackground(Color.white);
	
	}

	public void paint(Graphics g) {
		super.paint(g); // paint background
		Graphics2D g2d = (Graphics2D) g;

		g2d.setColor(Color.red);
		g2d.fillOval(PANEL_WIDTH / 2 - circleWidth / 2, PANEL_HEIGHT / 2 - circleHeight / 2, circleWidth, circleHeight);
		g2d.fillOval(PANEL_WIDTH / 2 - 50 / 2, PANEL_HEIGHT / 2 - 50 / 2, 50, 50);
	}
	
}
