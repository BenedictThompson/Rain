package net.azurewebsites.thehen101.raiblockswallet.rain.gui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.SettingsLoader;

public class RainFramePassword extends RainFrame {
	private JFrame frame;
	private JPanel panel;
	private JPasswordField password;
	private JPasswordField confirm;
	private JButton confirmButton;
	
	private boolean hasPasswordBeenSet;
	
	public RainFramePassword() {
		this.frame = new JFrame("Enter Rain Password");
		this.panel = new JPanel();
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(null);
		this.frame.setSize(300, 180);
	}

	@Override
	public void show() {
		JLabel enterPass = new JLabel("Please enter your wallet password:");
		enterPass.setFont(new Font("Tahoma", Font.BOLD, 12));
		enterPass.setBounds(12, 12, 268, 15);
		frame.getContentPane().add(enterPass);
		
		password = new JPasswordField();
		password.setBounds(72, 39, 208, 25);
		password.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				confirmButton.setEnabled(validPassword(confirm.getPassword(), password.getPassword()));
				
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					confirmButton.doClick();
			}
		});
		frame.getContentPane().add(password);
		
		JLabel lblPassword = new JLabel("Password:");
		lblPassword.setBounds(12, 44, 72, 15);
		frame.getContentPane().add(lblPassword);
		
		JLabel lblConfirm = new JLabel("Confirm:");
		lblConfirm.setBounds(12, 76, 58, 15);
		frame.getContentPane().add(lblConfirm);
		
		confirm = new JPasswordField();
		confirm.setBounds(63, 71, 217, 25);
		confirm.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				confirmButton.setEnabled(validPassword(confirm.getPassword(), password.getPassword()));
				
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					confirmButton.doClick();
			}
		});
		frame.getContentPane().add(confirm);
		
		JLabel warning = new JLabel("<html>DO NOT forget this password!<br>It can't yet be changed, so back it up!</html>");
		warning.setFont(new Font("Tahoma", Font.PLAIN, 10));
		warning.setBounds(12, 118, 184, 24);
		frame.getContentPane().add(warning);
		
		confirmButton = new JButton("Confirm");
		confirmButton.setEnabled(false);
		confirmButton.setBounds(195, 117, 85, 25);
		confirmButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SettingsLoader.INSTANCE.setPassword(new String(password.getPassword()));
				hasPasswordBeenSet = true;
				destroy();
			}
		});
		frame.getContentPane().add(confirmButton);
		
		this.frame.setLocationRelativeTo(null); //centre
		this.frame.setAlwaysOnTop(true);
		this.frame.setIconImages(this.getIcons());
		this.frame.getContentPane().add(this.panel);
		this.frame.setVisible(true);
	}
	
	private boolean validPassword(char[] a, char[] b) {
		if (Arrays.equals(a, b))
			if (a.length >= 3)
				return true;
		
		return false;
	}
	
	public boolean passwordSet() {
		return this.hasPasswordBeenSet;
	}

	@Override
	public void destroy() {
		this.frame.setVisible(false);
		this.panel.removeAll();
		this.frame.removeAll();
		this.panel = null;
		this.frame = null;
	}
}
