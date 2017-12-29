package net.azurewebsites.thehen101.raiblockswallet.rain.transaction;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;

public class ThreadTransactionPocketer extends Thread {
	private final Rain rain;
	private boolean alive;
	
	public ThreadTransactionPocketer(Rain rainInstance) {
		this.rain = rainInstance;
		this.alive = true;
	}
	
	@Override
	public void run() {
		while (this.alive) {
			//for ()
		}
	}
	
	public boolean getAlive() {
		return this.alive;
	}
	
	public void setAlive(boolean shouldBeAlive) {
		this.alive = shouldBeAlive;
	}
}
