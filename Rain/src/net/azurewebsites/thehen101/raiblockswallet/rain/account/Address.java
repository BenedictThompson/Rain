package net.azurewebsites.thehen101.raiblockswallet.rain.account;

import java.math.BigInteger;

public final class Address {
	private final Account parent;
	private final int index;
	private final byte[] publicKey;
	private final byte[] privateKey;
	private final String address;
	
	private String representative;
	private BigInteger rawBalance;
	//private final ArrayList<Transaction> unpocketedTransactions = new ArrayList<Transaction>();
	
	public Address(Account parent, int index, byte[] pub, byte[] priv, String address, String representative) {
		this.parent = parent;
		this.index = index;
		this.publicKey = pub;
		this.privateKey = priv;
		this.address = address;
		this.representative = representative;
	}
	
	public Account getParent() {
		return this.parent;
	}

	public int getIndex() {
		return index;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public byte[] getPrivateKey() {
		return privateKey;
	}

	public String getAddress() {
		return address;
	}
	
	public String getRepresentative() {
		return this.representative;
	}
	
	public void setRepresentative(String newRep) {
		this.representative = newRep;
	}
}
