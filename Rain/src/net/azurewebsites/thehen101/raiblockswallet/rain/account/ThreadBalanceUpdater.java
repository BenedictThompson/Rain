package net.azurewebsites.thehen101.raiblockswallet.rain.account;

import java.math.BigInteger;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;

public class ThreadBalanceUpdater extends Thread {
	private final Rain rain;
	private boolean alive, initial;
	
	public ThreadBalanceUpdater(Rain rain) {
		this.rain = rain;
		this.alive = true;
	}
	
	@Override
	public void run() {
		while (this.alive) {
			for (Account account : this.rain.getAccounts()) {
				int max = account.getAddressesCount();
				for (int i = 0; i < max; i++) {
					Address address = account.getAddressForIndex(i);
					this.setBal(address, this.rain.getAddressBalance(address));
					try {
						if (this.initial) {
							Thread.sleep(8000);
						} else {
							Thread.sleep(55);
							this.updateOpenStatus(address);
							Thread.sleep(55);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if (!this.initial) {
				System.out.println("Finished initial address balance updating");
				this.initial = true;
			}
		}
	}
	
	private void updateOpenStatus(Address address) {
		String prevBlock = this.rain.getPreviousHash(address);
		if (prevBlock.equals("")) {
			address.setIsOpened(false);
			System.out.println("Address " + address.getAddress() + " needs to be opened");
		}
	}
	
	public void updateBalance(final Address address) {
		Thread updater = new Thread() {
			@Override
			public void run() {
				try {
					//we sleep to let the block propagate through the network
					Thread.sleep(2500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setBal(address, rain.getAddressBalance(address));
			}
		};
		updater.start();
	}
	
	private void setBal(Address a, BigInteger[] result) {
		BigInteger bal = result[0];
		BigInteger pend = result[1];
		a.setBalance(bal);
		a.setPending(pend);
		a.setTotalBalance(bal.add(pend));
	}
	
	public boolean hasInitiallyCheckedAccounts() {
		return this.initial;
	}
	
	public boolean getAlive() {
		return this.alive;
	}
	
	public void setAlive(boolean shouldBeAlive) {
		this.alive = shouldBeAlive;
	}
}
