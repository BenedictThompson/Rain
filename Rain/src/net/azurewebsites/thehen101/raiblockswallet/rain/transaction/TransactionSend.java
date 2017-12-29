package net.azurewebsites.thehen101.raiblockswallet.rain.transaction;

import java.math.BigInteger;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.Blake2b;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.ED25519;

public class TransactionSend extends Transaction {
	private final Address address;
	private final String previous, sendAddress, signature, amountHex;
	private final BigInteger amount;

	public TransactionSend(String work, String previous, Address address, String sendToAddress,
			BigInteger finalAmountLeft) {
		super(Transaction.Type.SEND, work);
		this.address = address;
		this.previous = previous;
		this.sendAddress = sendToAddress;
		this.amount = finalAmountLeft;
		String raw = amount.toString(16).toUpperCase();
		while (raw.length() < 32)
			raw = "0" + raw;
		this.amountHex = raw;
		this.signature = calculateSignature();
	}
	
	public String getSendAddress() {
		return this.sendAddress;
	}
	
	public BigInteger getSendAmount() {
		return this.amount;
	}

	@Override
	String calculateSignature() {
		final Blake2b blake = Blake2b.Digest.newInstance(32);
		blake.update(DataManipulationUtil.hexStringToByteArray(this.previous));
		blake.update(this.address.getParent().addressToPublicKey(this.sendAddress));
		blake.update(DataManipulationUtil.hexStringToByteArray(this.amountHex));
		byte[] signature = ED25519.signature(blake.digest(), this.address.getPrivateKey(),
				this.address.getPublicKey());
		return DataManipulationUtil.bytesToHex(signature);
	}

	@Override
	public String getAsJSON() {
		return 
			"{" + 
				"\"action\": \"process\"," + 
				"\"block\": \"" + 
				"{" + 
					"\\\"type\\\": \\\"send\\\"," + 
					"\\\"destination\\\": \\\""+ this.sendAddress + "\\\"," + 
					"\\\"balance\\\": \\\"" + this.amountHex + "\\\"," + 
					"\\\"previous\\\": \\\"" + this.previous + "\\\"," + 
					"\\\"work\\\": \\\"" + this.getWork() + "\\\"," + 
					"\\\"signature\\\": \\\"" + this.signature + "\\\"" + 
				"}\"" + 
			"}";
	}
}
