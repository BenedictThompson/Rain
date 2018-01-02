package net.azurewebsites.thehen101.raiblockswallet.rain.event;

import java.math.BigInteger;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Event;
import net.azurewebsites.thehen101.raiblockswallet.rain.transaction.Transaction;

public class EventOurBlockReceived extends Event {
	private final Address add;
	private final Transaction.Type type;
	private final BigInteger amount;
	private final String address;
	
	public EventOurBlockReceived(Address add, Transaction.Type type, BigInteger amount, String address) {
		this.add = add;
		this.type = type;
		this.amount = amount;
		this.address = address;
	}

	public Address getAdd() {
		return add;
	}

	public Transaction.Type getType() {
		return type;
	}

	public BigInteger getAmount() {
		return amount;
	}

	public String getAddress() {
		return address;
	}
}
