package net.azurewebsites.thehen101.raiblockswallet.rain.gui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.alee.extended.statusbar.WebStatusBar;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuBar;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.progressbar.WebProgressBar;
import com.google.gson.GsonBuilder;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.EventBalanceUpdate;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.EventOurBlockReceived;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Event;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Listener;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerManager.ServerConnectionWithInfo;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DenominationConverter;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.SettingsLoader;

public class RainFrameWallet extends RainFrame implements Listener {
	private final Rain rain;
	private JFrame frame;
	private JPanel panel;
	private RainTabPanel[] webPanels;
	
	public RainFrameWallet(Rain rain) {
		this.rain = rain;
		webPanels = this.getPanelsForAccounts(this.rain.getAccounts());
		this.rain.getEventManager().addListener(this);
		this.frame = new JFrame("Rain " + Rain.VERSION);
		this.panel = new JPanel();
		
        // Simple status bar
        WebStatusBar statusBar = new WebStatusBar ();
        
        ServerConnectionWithInfo scwi = rain.getServerManager().getConnectionsWithInfo().get(rain.getServerManager().getBestServer());
        
        WebLabel simpleLabel = new WebLabel ("Connected: " + scwi.getConnection().getIP() 
        		+ " (" + scwi.getResponseTime() + "ms, " + scwi.getConnectedCount() + " users)");
        
        // Simple indetrminate progress bar
        WebProgressBar progressBar3 = new WebProgressBar ();
        progressBar3.setPreferredHeight(22);
        progressBar3.setPreferredProgressWidth(200);
        progressBar3.setIndeterminate ( true );
        progressBar3.setStringPainted ( true );
        progressBar3.setString ( "Generating POW..." );
        ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("pow")) {
					progressBar3.setEnabled(e.getID() == 1 ? true : false);
				}
			}
        };
        rain.getPOWFinder().addActionListener(al);
       // statusBar.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
        //statusBar.add(new JLabel("left"));
        statusBar.add(simpleLabel);
        statusBar.add(Box.createHorizontalGlue());
        //statusBar.add(new JLabel("right"));
        
        
        statusBar.add(progressBar3);
        
		this.frame.setSize(600, 400);
		this.frame.setIconImages(this.getIcons());
		

		this.frame.getContentPane().add(this.panel);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		for (int i = 0; i < webPanels.length; i++) {
			WebPanel wp = webPanels[i];
			tabbedPane.addTab("Account #" + (i + 1), wp);
		}
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addComponent(statusBar, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE)
				.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE)
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(statusBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		panel.setLayout(gl_panel);
		this.frame.setLocationRelativeTo(null); //centre
		
		WebMenuBar menuBar = new WebMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnNewMenu = new JMenu("File");
		menuBar.add(mnNewMenu);
		
		JMenuItem mntmOpenRainDirectory = new JMenuItem("Open Rain directory...");
		mntmOpenRainDirectory.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					Desktop.getDesktop().open(SettingsLoader.INSTANCE.getRainDirectory());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		mnNewMenu.add(mntmOpenRainDirectory);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		});
		mnNewMenu.add(mntmExit);
		
		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		
		JMenuItem mntmCopyAddressTo = new JMenuItem("Import seed from clipboard");
		mntmCopyAddressTo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					String clipboard = (String) Toolkit.getDefaultToolkit()
							.getSystemClipboard().getData(DataFlavor.stringFlavor); 
					rain.addAccount(new Account( 
							DataManipulationUtil.hexStringToByteArray(clipboard), 
							rain.getDefaultRepresentative()));
					System.out.println("Added account from clipboard");
					
					tabbedPane.removeAll();
					
					webPanels = getPanelsForAccounts(rain.getAccounts());
					for (int i = 0; i < webPanels.length; i++) {
						WebPanel wp = webPanels[i];
						tabbedPane.addTab("Account #" + (i + 1), wp);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		mnEdit.add(mntmCopyAddressTo);
		
		JMenuItem clipExport = new JMenuItem("Export seeds to clipboard");
		clipExport.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					ArrayList<String> seeds = new ArrayList<String>();
					for (int i = 0; i < rain.getAccounts().size(); i++)
						seeds.add(DataManipulationUtil.bytesToHex(rain.getAccounts().get(i).getSeed()));
					
					System.out.println(seeds.size() + " seeds");
					
					String json = new GsonBuilder().setPrettyPrinting().create().toJson(seeds);
					
				    StringSelection selection = new StringSelection(json);
				    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		mnEdit.add(clipExport);
		
		//update balance for selected address
		RainTabPanel rtp = (RainTabPanel) tabbedPane.getSelectedComponent();
		Address toUpdate = (Address) rtp.addresses.getSelectedItem();
		rain.getBalanceUpdater().updateBalance(toUpdate);
		
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.setResizable(false);
		this.frame.setVisible(true);
	}
	
	private RainTabPanel[] getPanelsForAccounts(ArrayList<Account> accounts) {
		RainTabPanel[] panels = new RainTabPanel[accounts.size()];
		for (int i = 0; i < accounts.size(); i++) {
			panels[i] = new RainTabPanel(accounts.get(i));
		}
		return panels;
	}
	
	@Override
	public void onEvent(Event event) {
		if (event instanceof EventBalanceUpdate) {
			EventBalanceUpdate ebu = (EventBalanceUpdate) event;
			for (int i = 0; i < this.webPanels.length; i++) {
				RainTabPanel rtp = this.webPanels[i];
				Account account = rtp.getAccount();
				for (int ii = 0; ii < account.getMaxAddressIndex(); ii++) {
					boolean isAddressAtIndex = account.isAddressAtIndex(ii);
					if (isAddressAtIndex) {
						Address address = account.getAddressAtIndex(ii);
						if (address.equals(ebu.getAddress())) {
							rtp.updateBalance(address);
						}
					}
				}
			}
		}
		
		if (event instanceof EventOurBlockReceived) {
			EventOurBlockReceived eobr = (EventOurBlockReceived) event;
			for (int i = 0; i < this.webPanels.length; i++) {
				RainTabPanel rtp = this.webPanels[i];
				Account account = rtp.getAccount();
				for (int ii = 0; ii < account.getMaxAddressIndex(); ii++) {
					boolean isAddressAtIndex = account.isAddressAtIndex(ii);
					if (isAddressAtIndex) {
						Address address = account.getAddressAtIndex(ii);
						if (address.equals(eobr.getAdd())) {
							String amount = DenominationConverter.convert(new BigDecimal(eobr.getAmount()), 
									DenominationConverter.RAW, DenominationConverter.MRAI).toPlainString();
							amount = amount.indexOf(".") < 0 ? amount
									: amount.replaceAll("0*$", "").replaceAll("\\.$", "");
							System.out.println("Table row updated");
							rtp.addTableRow(new String[] { eobr.getType().toString(), amount, eobr.getAdd().getAddress() });
						}
					}
				}
			}
		}
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	
	public class RainTabPanel extends WebPanel {
		private final Account acc;
		private JTable table;
		private WebLabel selectedAddress;
		private WebButton removeAddress;
		private WebLabel status;
		private WebButton copyAddress;
		private JScrollPane scrollPane;
		private WebLabel addressXRBBalance;
		private WebLabel addressUSDBalance;
		private WebButton sendXRBButton;
		private WebComboBox addresses;
		private WebButton createAddress;
		private long fadeTime;
		
		public RainTabPanel(Account account) {
			this.acc = account;
			this.setup();
		}
		
		public Account getAccount() {
			return this.acc;
		}
		
		public void updateBalance(Address address) {
			try {
				if (!address.equals((Address) addresses.getSelectedItem()))
					return;
				BigDecimal amount = DenominationConverter.convert(new BigDecimal(address.getRawTotalBalance()),
						DenominationConverter.RAW, DenominationConverter.MRAI);

				String amountString = amount.toPlainString();

				amountString = amountString.indexOf(".") < 0 ? amountString
						: amountString.replaceAll("0*$", "").replaceAll("\\.$", "");
				
				boolean hasUnpocketed = !address.getRawPending().equals(BigInteger.ZERO);
				
				BigDecimal amountUnpocketed = DenominationConverter.convert(new BigDecimal(address.getRawPending()),
						DenominationConverter.RAW, DenominationConverter.MRAI);

				String amountStringUnpocketed = amountUnpocketed.toPlainString();

				amountStringUnpocketed = amountStringUnpocketed.indexOf(".") < 0 ? amountStringUnpocketed
						: amountStringUnpocketed.replaceAll("0*$", "").replaceAll("\\.$", "");

				addressXRBBalance.setText("XRB: " + amountString 
						+ (hasUnpocketed ? (" (" + amountStringUnpocketed + " XRB unpocketed)") : ""));

				String usdString = amount.multiply(new BigDecimal(rain.getPriceUpdater().getPrice())).toPlainString();

				usdString = usdString.indexOf(".") < 0 ? usdString
						: usdString.replaceAll("0*$", "").replaceAll("\\.$", "");

				addressUSDBalance.setText("USD: " + usdString);
				
				this.enableSendButton();
			} catch (Exception e) {
				addressXRBBalance.setText("XRB: Not yet known");
				addressUSDBalance.setText("USD: Not yet known");
				//e.printStackTrace();
			}
		}
		
		public void enableSendButton() {
			this.sendXRBButton.setEnabled(true);
		}
		
		public void addTableRow(String[] newRow) {
			if (newRow.length != 3)
				return;
			
			((DefaultTableModel) this.table.getModel()).insertRow(0, newRow);
		}
		
		private void notifyUser(String s, Color c) {
			status.setForeground(c);
			status.setText(s);
			fadeTime = System.currentTimeMillis() + 1000L;
			Thread notify = new Thread() {
				@Override
				public void run() {
					try {
						final long ft = fadeTime;
						while (System.currentTimeMillis() < fadeTime)
							Thread.sleep(1);
						
						if (ft != fadeTime)
							return;
						
						Color prev = status.getForeground();
						for (int opacity = 255; opacity > 0; opacity--) {
							status.setForeground(
									new Color(prev.getRed(), prev.getGreen(), prev.getBlue(), opacity));
							Thread.sleep(1);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			notify.start();
		}
		
		private void setup() {
			this.setLayout(null);
			
			//label to notify the user the address has been copied
			status = new WebLabel("");
			status.setForeground(new Color(0, 0, 0, 0));
			status.setFont(new Font("Tahoma", Font.PLAIN, 10));
			status.setBounds(129, 46, 445, 15);
			this.add(status);
			
			//button to copy selected Address
			copyAddress = new WebButton("Copy");
			copyAddress.setBounds(63, 40, 58, 25);
			copyAddress.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					StringSelection selection = new StringSelection(selectedAddress.getText());
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
					
					notifyUser("Address copied to clipboard", new Color(0, 100, 0));
				}
			});
			this.add(copyAddress);
			
			//JTable that contains recent transactions
			Object[] columnNames = { "Type", "Amount", "Address" };

			table = new JTable(new DefaultTableModel(null, columnNames) {
			    @Override
			    public boolean isCellEditable(int row, int column) {
			       return false;
			    }
			});
			scrollPane = new JScrollPane(table);
			scrollPane.setBounds(12, 118, 562, 144);
			
			Dimension tableSize = scrollPane.getPreferredSize();
			table.getColumnModel().getColumn(0).setPreferredWidth(Math.round(tableSize.width * 0.06f));
			table.getColumnModel().getColumn(1).setPreferredWidth(Math.round(tableSize.width * 0.08f));
			table.getColumnModel().getColumn(2).setPreferredWidth(Math.round(tableSize.width * 0.86f));
			
			DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
			centerRenderer.setHorizontalAlignment(JLabel.CENTER);

			for (int x = 0; x < table.getColumnCount(); x++) {
				table.getColumnModel().getColumn(x).setCellRenderer(centerRenderer);
			}

			table.setBounds(47, 132, 463, 100);
			this.add(scrollPane);
			
			//label to show address balance in XRB
			addressXRBBalance = new WebLabel("XRB: ", 
					new ImageIcon(RainFrameSplash.class.getClassLoader().getResource("images/rai.png")));
			addressXRBBalance.setBounds(12, 77, 350, 15);
			this.add(addressXRBBalance);
			
			//label to show address balance in USD
			addressUSDBalance = new WebLabel("USD: ", 
					new ImageIcon(RainFrameSplash.class.getClassLoader().getResource("images/dollar.png")));
			addressUSDBalance.setBounds(12, 98, 350, 15);
			this.add(addressUSDBalance);
			
			//button to open send prompt
			sendXRBButton = new WebButton("Send...");
			sendXRBButton.setBounds(453, 85, 121, 30);
			sendXRBButton.setEnabled(false);
			sendXRBButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					RainFrameSend rfs = new RainFrameSend(rain, acc, (Address) addresses.getSelectedItem());
					rfs.show();
				}
			});
			this.add(sendXRBButton);
			
			
			//combobox to choose address from
			addresses = new WebComboBox();
			
			for (int o = 0; o < acc.getMaxAddressIndex(); o++) {
				boolean valid = acc.isAddressAtIndex(o);
				if (valid) {
					addresses.addItem(acc.getAddressAtIndex(o));
				}
			}
			
			addresses.setBounds(11, 12, 110, 25);
			addresses.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent event) {
					if (event.getStateChange() == ItemEvent.SELECTED) {
						sendXRBButton.setEnabled(false);
						Address ad = (Address) event.getItem();
						updateBalance(ad);
						Thread updateBalance = new Thread() {
							@Override
							public void run() {
								rain.getBalanceUpdater().updateBalance(ad);
							}
						};
						updateBalance.start();
						selectedAddress.setText(ad.getAddress());
						
						notifyUser("Address switched", new Color(0, 0, 0));
					}
				}
			});
			addresses.setSelectedIndex(0);
			this.add(addresses);
			
			//create new address for account
			createAddress = new WebButton("+");
			createAddress.setBounds(37, 40, 26, 25);
			createAddress.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					Address aaa = null;
					for (int a = 0; a < Integer.MAX_VALUE; a++) {
						boolean isAddressAlreadyThere = acc.isAddressAtIndex(a);
						System.out.println(isAddressAlreadyThere);
						if (!isAddressAlreadyThere) {
							acc.addAddress(a);
							aaa = acc.getAddressAtIndex(a);
							break;
						}
					}
					System.out.println(aaa.getAddress());
					aaa.setIsOpened(false);
					addresses.addItem(aaa);
					addresses.setSelectedItem(aaa);
					SettingsLoader.INSTANCE.saveAccounts(rain.getAccounts());
					rain.getPOWFinder().syncArrayAndMap();
					SettingsLoader.INSTANCE.cachePOW(rain.getPOWFinder());
					
					notifyUser("Address created", new Color(0, 100, 0));
				}
			});
			this.add(createAddress);
			
			//remove address for account
			removeAddress = new WebButton("-");
			removeAddress.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					if (addresses.getItemCount() <= 1) {
						notifyUser("Can't remove last address", new Color(200, 0, 0));
						return;
					}
					
					Address addressToRemove = (Address) addresses.getSelectedItem();
					int removeIndex = -1;
					for (int b = 0; b < acc.getMaxAddressIndex(); b++) {
						boolean valid = acc.isAddressAtIndex(b);
						if (valid) {
							Address az = acc.getAddressAtIndex(b);
							if (az.equals(addressToRemove)) {
								removeIndex = b;
							}
						}
					}
					acc.removeAddress(removeIndex);
					addresses.removeItem(addressToRemove);
					addresses.setSelectedIndex(0);
					SettingsLoader.INSTANCE.saveAccounts(rain.getAccounts());
					rain.getPOWFinder().syncArrayAndMap();
					SettingsLoader.INSTANCE.cachePOW(rain.getPOWFinder());
					
					notifyUser("Address removed", new Color(200, 0, 0));
				}
			});
			removeAddress.setBounds(11, 40, 26, 25);
			this.add(removeAddress);
			
			//selected Address address
			selectedAddress = new WebLabel("selectedAddress");
			selectedAddress.setText(((Address)addresses.getSelectedItem()).getAddress());
			selectedAddress.setBounds(129, 17, 445, 15);
			this.add(selectedAddress);
		}
	}
}
