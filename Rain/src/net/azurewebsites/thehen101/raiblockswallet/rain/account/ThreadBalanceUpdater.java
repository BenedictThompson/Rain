package net.azurewebsites.thehen101.raiblockswallet.rain.account;

import java.math.BigInteger;
import java.util.ArrayList;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.EventBalanceUpdate;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.EventRequestBalanceUpdate;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Event;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Listener;

public class ThreadBalanceUpdater extends Thread implements Listener {
	private final ArrayList<Address> urgentUpdateQueue = new ArrayList<Address>();
	private final Rain rain;
	private boolean alive, initial;
	
	public ThreadBalanceUpdater(Rain rain) {
		this.rain = rain;
		this.rain.getEventManager().addListener(this);
		this.alive = true;
	}
	
	@Override
	public void run() {
		while (this.alive) {
			try {
				for (Account account : this.rain.getAccounts()) {
					int max = account.getAddressesCount();
					for (int i = 0; i < max; i++) {
						while (this.urgentUpdateQueue.size() > 0) {
							Address urgent = this.urgentUpdateQueue.get(0);
							this.urgentUpdateQueue.remove(0);
							this.setBal(urgent, this.rain.getAddressBalance(urgent));
							this.rain.getEventManager().callEvent(new EventBalanceUpdate(urgent));
						}
						
						Address address = account.getAddressForIndex(i);
						boolean wasNull = address.getRawTotalBalance() == null 
								|| address.getRawBalance() == null
								|| address.getRawPending() == null;
						this.setBal(address, this.rain.getAddressBalance(address));
						this.rain.getEventManager().callEvent(new EventBalanceUpdate(address));
						try {
							if (this.initial && !wasNull) {
								Thread.sleep(8000);
							} else {
								Thread.sleep(55);
								this.updateOpenStatus(address);
								Thread.sleep(55);
							}
						} catch (InterruptedException e) {
						}
					}
				}
				if (!this.initial) {
					System.out.println("Finished initial address balance updating");
					this.initial = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
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
					Thread.sleep(3000);
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

	@Override
	public void onEvent(Event event) {
		if (event instanceof EventRequestBalanceUpdate) {
			EventRequestBalanceUpdate erbe = (EventRequestBalanceUpdate) event;
			this.interrupt();
			this.urgentUpdateQueue.add(erbe.getAddress());
		}
	}
}
