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
		//queues transactions to be pocketed
		Thread pocketer = new Thread() {
			@Override
			public void run() {
				while (alive) {
					for (int i = 0; i < rain.getAccounts().size(); i++) {
						Account a = rain.getAccounts().get(i);
						for (int ii = 0; ii < a.getMaxAddressIndex(); ii++) {
							boolean addressAtIndex = a.isAddressAtIndex(ii);
							if (addressAtIndex) {
								Address address = a.getAddressAtIndex(ii);
								String[] unpocketedHashes = rain.getUnpocketedForAddress(address);
								for (int iii = 0; iii < unpocketedHashes.length; iii++) {
									String unpocketedHash = unpocketedHashes[iii];
									if (unpocketedHash != null)
										if (!unpocketedHash.equals(""))
											if (!address.getUnpocketedTransactions().contains(unpocketedHash))
												address.getUnpocketedTransactions().add(unpocketedHash);
								}
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					}
					try {
						Thread.sleep(20000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		pocketer.start();
		while (this.alive) {
			//pockets transactions
			for (int i = 0; i < this.rain.getAccounts().size(); i++) {
				Account a = rain.getAccounts().get(i);
				for (int ii = 0; ii < a.getMaxAddressIndex(); ii++) {
					boolean addressAtIndex = a.isAddressAtIndex(ii);
					if (addressAtIndex) {
						Address address = a.getAddressAtIndex(ii);
						if (this.rain.getPOWFinder().isPOWForAddress(address)) {
							for (int iii = 0; iii < address.getUnpocketedTransactions().size(); iii++) {
								String unpocketedHash = address.getUnpocketedTransactions().get(iii);
								System.out.println("Pocketing for address: " + address.getAddress() + " " + unpocketedHash);
								this.pocket(address, unpocketedHash);
								address.getUnpocketedTransactions().remove(unpocketedHash);
								try {
									Thread.sleep(200);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void pocket(Address address, String hash) {
		RequestWithHeader rwh = null;
		String prevHash = this.rain.getPreviousHash(address);
		if (address.getIsOpened()) {
			TransactionReceive receive = new TransactionReceive(this.rain.getPOWFinder()
					.getPowBlocking(address), address, prevHash, hash);
			rwh = new RequestWithHeader(false, receive.getAsJSON());
		} else {
			TransactionOpen open = new TransactionOpen(
					this.rain.getPOWFinder().getPowBlocking(address), hash, address);
			rwh = new RequestWithHeader(false, open.getAsJSON());
			address.setIsOpened(true);
		}
		System.out.println(new String(rwh.getRequestBytes()));
		this.rain.getServerManager().addToConnectedServerQueue(rwh);
	}
	
	public boolean getAlive() {
		return this.alive;
	}
	
	public void setAlive(boolean shouldBeAlive) {
		this.alive = shouldBeAlive;
	}
}
