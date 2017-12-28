package net.azurewebsites.thehen101.raiblockswallet.rain.account;

public final class Address {
	private final int index;
	private final byte[] publicKey;
	private final byte[] privateKey;
	private final String address;
	
	//private final String representative;
	//private final BigInteger rawBalance;
	//private final ArrayList<Transaction> unpocketedTransactions = new ArrayList<Transaction>();
	
	public Address(int index, byte[] pub, byte[] priv, String address) {
		this.index = index;
		this.publicKey = pub;
		this.privateKey = priv;
		this.address = address;
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
}
