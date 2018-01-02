package net.azurewebsites.thehen101.raiblockswallet.rain.event;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Event;

public class EventBalanceUpdate extends Event {
	private final Address address;
	
	public EventBalanceUpdate(Address address) {
		this.address = address;
	}
	
	public Address getAddress() {
		return this.address;
	}
}
