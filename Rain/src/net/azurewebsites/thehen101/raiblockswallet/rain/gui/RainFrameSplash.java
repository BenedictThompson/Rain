package net.azurewebsites.thehen101.raiblockswallet.rain.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class RainFrameSplash extends RainFrame {
	private JFrame frame;
	private JPanelImage panel;
	
	public RainFrameSplash() {
		this.frame = new JFrame("Rain Client");
		this.panel = new JPanelImage(RainFrameSplash.class.getClassLoader().getResource("images/RainSplash.png"));
	}

	@Override
	public void show() {
		this.panel.setOpaque(false);
		this.panel.setBackground(new Color(0.0F, 0.0F, 0.0F, 0.0F));
		this.frame.setUndecorated(true);
		this.frame.setOpacity(0.96F);
		this.frame.setBackground(new Color(0.0F, 0.0F, 0.0F, 0.0F));
		this.frame.setSize(this.panel.getWidth(), this.panel.getHeight());
		this.frame.add(this.panel);
		this.frame.setLocationRelativeTo(null); //centre
		this.frame.setAlwaysOnTop(true);
		this.frame.setIconImages(this.getIcons());
		this.frame.setVisible(true);
	}

	@Override
	public void destroy() {
		this.frame.setVisible(false);
		this.panel.removeAll();
		this.frame.removeAll();
		this.panel = null;
		this.frame = null;
	}
	
	public class JPanelImage extends JPanel {
		private static final long serialVersionUID = 6732002468969737185L;
		private BufferedImage image;

		public JPanelImage(URL pic) {
			try {
				image = ImageIO.read(pic);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			this.setSize(image.getWidth() + 5, image.getHeight() + 10); //system UI bars
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, this); // see javadoc for more info on the parameters
		}
	}
}
