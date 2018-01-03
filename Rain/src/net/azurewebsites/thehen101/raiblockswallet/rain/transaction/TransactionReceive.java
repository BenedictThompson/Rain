package net.azurewebsites.thehen101.raiblockswallet.rain.transaction;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.Blake2b;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.ED25519;

public class TransactionReceive extends Transaction {
	private final Address address;
	private final String previousHash, unconfirmedHash, signature;

	public TransactionReceive(String work, Address address,
			String previousHash, String unconfirmedHash) {
		super(Transaction.Type.RECEIVE, work);
		this.address = address;
		this.previousHash = previousHash;
		this.unconfirmedHash = unconfirmedHash;
		this.signature = calculateSignature();
	}

	@Override
	String calculateSignature() {
		final Blake2b blake = Blake2b.Digest.newInstance(32);
		blake.update(DataManipulationUtil.hexStringToByteArray(this.previousHash));
		blake.update(DataManipulationUtil.hexStringToByteArray(this.unconfirmedHash));
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
					"\\\"type\\\": \\\"receive\\\"," + 
					"\\\"source\\\": \\\""+ this.unconfirmedHash + "\\\"," + 
					"\\\"previous\\\": \\\"" + this.previousHash + "\\\"," + 
					"\\\"work\\\": \\\"" + this.getWork() + "\\\"," + 
					"\\\"signature\\\": \\\"" + this.signature + "\\\"" + 
				"}\"" + 
			"}";
	}
}
