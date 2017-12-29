package net.azurewebsites.thehen101.raiblockswallet.rain.account;

import java.math.BigInteger;
import java.util.ArrayList;

public final class Address {
	private final Account parent;
	private final int index;
	private final byte[] publicKey;
	private final byte[] privateKey;
	private final String address;
	
	private String representative;
	private BigInteger rawBalance;
	private final ArrayList<String> unpocketedTransactions = new ArrayList<String>();
	
	private String nextPow;
	
	public Address(Account parent, int index, byte[] pub, byte[] priv, String address, String representative, String nextPOW) {
		this.parent = parent;
		this.index = index;
		this.publicKey = pub;
		this.privateKey = priv;
		this.address = address;
		this.representative = representative;
		this.nextPow = nextPOW;
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
	
	public String getNextPOW() {
		return this.nextPow;
	}
	
	public void setNextPOW(String newPOW) {
		this.nextPow = newPOW;
	}
	
	public ArrayList<String> getUnpocketedTransactions() {
		return this.unpocketedTransactions;
	}
}
