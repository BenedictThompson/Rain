package net.azurewebsites.thehen101.raiblockswallet.rain.gui;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

public abstract class RainFrame {
	
	public abstract void show();
	
	public abstract void destroy();
	
	List<? extends Image> getIcons() {
		List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(RainFrameSplash.class.getClassLoader().getResource("images/Rain16x.png")).getImage());
		icons.add(new ImageIcon(RainFrameSplash.class.getClassLoader().getResource("images/Rain32x.png")).getImage());
		icons.add(new ImageIcon(RainFrameSplash.class.getClassLoader().getResource("images/Rain64x.png")).getImage());
		return icons;
	}
}
