package net.azurewebsites.thehen101.raiblockswallet.rain.event;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Event;

public class EventGUIAddressChanged extends Event {
	private final Address newAddress;
	
	public EventGUIAddressChanged(Address newAddress) {
		this.newAddress = newAddress;
	}
	
	public Address getAddress() {
		return this.newAddress;
	}
}
