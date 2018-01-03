package net.azurewebsites.thehen101.raiblockswallet.rain.account;

import java.math.BigInteger;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.EventBalanceUpdate;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.EventOurBlockReceived;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Event;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Listener;

public class BalanceUpdater implements Listener {
	private final Rain rain;
	
	public BalanceUpdater(Rain rain) {
		this.rain = rain;
		this.rain.getEventManager().addListener(this);
	}

	@Override
	public void onEvent(Event event) {
		if (event instanceof EventOurBlockReceived) {
			EventOurBlockReceived eobr = (EventOurBlockReceived) event;
			Thread updater = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(500); //grace time
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					updateBalance(eobr.getAdd());
				}
			};
			updater.start();
		}
	}
	
	public void updateBalance(Address address) {
		this.setBal(address, rain.getAddressBalance(address));
		this.rain.getEventManager().callEvent(new EventBalanceUpdate(address));
	}

	private void setBal(Address a, BigInteger[] result) {
		BigInteger bal = result[0];
		BigInteger pend = result[1];
		a.setBalance(bal);
		a.setPending(pend);
		a.setTotalBalance(bal.add(pend));
	}
}
