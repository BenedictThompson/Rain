package net.azurewebsites.thehen101.raiblockswallet.rain.transaction;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.RequestWithHeader;

public class ThreadTransactionPocketer extends Thread {
	private final Rain rain;
	private boolean alive;
	
	public ThreadTransactionPocketer(Rain rainInstance) {
		this.rain = rainInstance;
		this.alive = true;
	}
	
	@Override
	public void run() {
		//pockets previously unpocketed (historic) transactions
		Thread pocketer = new Thread() {
			@Override
			public void run() {
				while (alive) {
					for (Account account : rain.getAccounts()) {
						int max = account.getAddressesCount();
						for (int i = 0; i < max; i++) {
							Address address = account.getAddressForIndex(i);
							String[] unpocketed = rain.getUnpocketedForAddress(address);
							for (String hash : unpocketed) {
								if (hash != null)
									if (!hash.equals("")) {
										if (!address.getUnpocketedTransactions().contains(hash)) {
											System.out.println("Adding transaction to pocket: " 
													+ address.getAddress() + " " +hash);
											address.getUnpocketedTransactions().add(hash);
										}
									}
							}
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		pocketer.start();
		while (this.alive) {
			//pockets live transactions
			for (Account account : this.rain.getAccounts()) {
				int max = account.getAddressesCount();
				for (int i = 0; i < max; i++) {
					Address address = account.getAddressForIndex(i);
					for (int ii = 0; ii < address.getUnpocketedTransactions().size(); ii++) {
						String unpocketedHash = address.getUnpocketedTransactions().get(ii);
						System.out.println("Pocketing for address: " + address.getAddress() + " " + unpocketedHash);
						this.pocket(address, unpocketedHash);
						address.getUnpocketedTransactions().remove(unpocketedHash);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void pocket(Address address, String hash) {
		RequestWithHeader rwh = null;
		if (address.getIsOpened()) {
			TransactionReceive receive = new TransactionReceive(this.rain.getPOWFinder()
					.getPowBlocking(address), address, this.rain.getPreviousHash(address), hash);
			rwh = new RequestWithHeader(false, receive.getAsJSON());
		} else {
			TransactionOpen open = new TransactionOpen(
					this.rain.getPOWFinder().getPowBlocking(address), hash, address);
			rwh = new RequestWithHeader(false, open.getAsJSON());
			address.setIsOpened(true);
		}
		this.rain.getServerManager().addToConnectedServerQueue(rwh);
		this.rain.getBalanceUpdater().updateBalance(address);
	}
	
	public boolean getAlive() {
		return this.alive;
	}
	
	public void setAlive(boolean shouldBeAlive) {
		this.alive = shouldBeAlive;
	}
}
