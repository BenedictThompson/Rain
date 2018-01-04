package net.azurewebsites.thehen101.raiblockswallet.rain.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DenominationConverter;

public class RainFrameSend extends RainFrame {
	private final Rain rain;
	private final Account acc;
	private final Address sendFrom;
	private JFrame frame;
	private JPanel panel;
	private JTextField sendToAddress;
	private JFormattedTextField amount;
	private JButton btnSend;
	private JLabel labelAmount;
	
	private boolean amountValid;
	private boolean addressValid;
	private JButton btnSet;
	private JLabel lblTotalBalance;
	private JLabel lblAvailableBalance;
	private JLabel lblRemainingBalance;
	private JLabel availableXRB;
	private JLabel sendingXRB;
	private JLabel remainingXRB;
	
	public RainFrameSend(Rain rain, Account acc, Address sendFrom) {
		this.rain = rain;
		this.acc = acc;
		this.sendFrom = sendFrom;
		this.frame = new JFrame("Send");
		this.panel = new JPanel();
		this.frame.getContentPane().setLayout(null);
		this.frame.setSize(550, 180);		
		JLabel lblSendingFrom = new JLabel("Sending from:");
		lblSendingFrom.setBounds(12, 12, 77, 15);
		frame.getContentPane().add(lblSendingFrom);
		
		JLabel lblSendingTo = new JLabel("Sending to:");
		lblSendingTo.setBounds(12, 39, 64, 15);
		frame.getContentPane().add(lblSendingTo);
		
		sendToAddress = new JTextField();
		sendToAddress.setBounds(82, 34, 448, 25);
		sendToAddress.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) { }

			@Override
			public void keyReleased(KeyEvent arg0) {
				addressValid = acc.addressToPublicKey(sendToAddress.getText()) != null;
				enableSend();
			}

			@Override
			public void keyTyped(KeyEvent arg0) { }
		});
		frame.getContentPane().add(sendToAddress);
		
		btnSend = new JButton("Send");
		btnSend.setEnabled(false);
		btnSend.setBounds(445, 121, 85, 25);
		btnSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BigDecimal bd = new BigDecimal(amount.getValue().toString());
				BigInteger send = DenominationConverter
						.convert(bd, DenominationConverter.MRAI, DenominationConverter.RAW).toBigIntegerExact();
				rain.sendXRBRaw(sendFrom, sendToAddress.getText(), send);
				frame.setVisible(false);
			}
		});
		frame.getContentPane().add(btnSend);
		
		labelAmount = new JLabel("$0.00");
		labelAmount.setHorizontalAlignment(SwingConstants.RIGHT);
		labelAmount.setBounds(332, 126, 109, 15);
		frame.getContentPane().add(labelAmount);
		
		JLabel lblNewLabel_1 = new JLabel("Amount (XRB):");
		lblNewLabel_1.setBounds(12, 126, 85, 15);
		frame.getContentPane().add(lblNewLabel_1);
		
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(30);
		df.setMaximumIntegerDigits(9);
		NumberFormatter dnff = new NumberFormatter(df);
		dnff.setMinimum(new BigDecimal("0.00000000000000000000000000001"));
		DefaultFormatterFactory factory = new DefaultFormatterFactory(dnff);
		
		amount = new JFormattedTextField(factory);
		amount.setFont(new Font("Tahoma", Font.PLAIN, 10));
		amount.setBounds(98, 122, 177, 25);
		frame.getContentPane().add(amount);
		amount.setColumns(10);
		
		JLabel lblNewLabel_2 = new JLabel(this.sendFrom.getAddress());
		lblNewLabel_2.setBounds(98, 12, 432, 15);
		frame.getContentPane().add(lblNewLabel_2);
		
		btnSet = new JButton("Set");
		btnSet.setBounds(278, 122, 43, 25);
		btnSet.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (amount == null)
					return;
				if (amount.getValue() == null)
					return;
				System.out.println(amount.getValue().toString());
				if (!amount.getValue().toString().matches("^-?\\d+\\.?\\d*$")) {
					amount.setText("");
					amountValid = false;
				} else {
					BigDecimal bd = new BigDecimal(amount.getValue().toString());
					if (bd != null) {
						amountValid = true;
						sendingXRB.setText(bd.toPlainString() + " XRB");
						
						BigInteger remainingRaw = sendFrom.getRawBalance().subtract(DenominationConverter
								.convert(bd, DenominationConverter.MRAI, DenominationConverter.RAW).toBigIntegerExact());
						if (remainingRaw.compareTo(BigInteger.ZERO) < 0) {
							amountValid = false;
							remainingXRB.setForeground(new Color(255, 0, 0));
						} else {
							remainingXRB.setForeground(new Color(0, 0, 0));
						}
						
						remainingXRB.setText(DenominationConverter
								.convert(new BigDecimal(remainingRaw),
										DenominationConverter.RAW, DenominationConverter.MRAI)
								.toPlainString() + " XRB");
					}
				}
				enableSend();
			}
		});
		frame.getContentPane().add(btnSet);
		
		lblTotalBalance = new JLabel("Available:");
		lblTotalBalance.setBounds(15, 66, 50, 15);
		frame.getContentPane().add(lblTotalBalance);
		
		lblAvailableBalance = new JLabel("Sending:");
		lblAvailableBalance.setBounds(15, 83, 50, 15);
		frame.getContentPane().add(lblAvailableBalance);
		
		lblRemainingBalance = new JLabel("Remaining:");
		lblRemainingBalance.setBounds(15, 100, 59, 15);
		frame.getContentPane().add(lblRemainingBalance);
		
		availableXRB = new JLabel(DenominationConverter.convert(new BigDecimal(sendFrom.getRawBalance()),
				DenominationConverter.RAW, DenominationConverter.MRAI).toPlainString() + " XRB");
		availableXRB.setBounds(75, 66, 455, 15);
		frame.getContentPane().add(availableXRB);
		
		sendingXRB = new JLabel("0 XRB");
		sendingXRB.setBounds(75, 83, 455, 15);
		frame.getContentPane().add(sendingXRB);
		
		remainingXRB = new JLabel(DenominationConverter.convert(new BigDecimal(sendFrom.getRawBalance()),
				DenominationConverter.RAW, DenominationConverter.MRAI).toPlainString() + " XRB");
		remainingXRB.setBounds(85, 100, 445, 15);
		frame.getContentPane().add(remainingXRB);
		
		//if we are still pending
		if (!sendFrom.getRawPending().equals(BigInteger.ZERO)
				|| !sendFrom.getRawTotalBalance().equals(sendFrom.getRawBalance())) {
			this.btnSet.setEnabled(false);
			this.amount.setEnabled(false);
			availableXRB.setText("Cannot send with a pending balance");
		}
	}
	
	private void enableSend() {
		if (this.addressValid && this.amountValid) {
			this.btnSend.setEnabled(true);
			BigDecimal bd = new BigDecimal(amount.getText());
			String usdString = bd.multiply(new BigDecimal(rain.getPriceUpdater().getPrice())).toPlainString();
			usdString = usdString.indexOf(".") < 0 ? usdString
					: usdString.replaceAll("0*$", "").replaceAll("\\.$", "");
			this.labelAmount.setText("$" + usdString);
		} else
			this.btnSend.setEnabled(false);
	}
	
	@Override
	public void show() {
		this.frame.setLocationRelativeTo(null); //centre
		this.frame.setIconImages(this.getIcons());
		this.frame.getContentPane().add(this.panel);
		this.frame.setResizable(false);
		this.frame.setVisible(true);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
