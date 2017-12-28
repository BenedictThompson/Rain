package net.azurewebsites.thehen101.raiblockswallet.rain.transaction;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.Blake2b;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.ED25519;

public class TransactionOpen extends Transaction {
	private final Address address;
	private final String signature, source;

	public TransactionOpen(String work, String source, Address address) {
		super(Transaction.Type.OPEN, work);
		this.address = address;
		this.source = source;
		this.signature = calculateSignature();
	}

	@Override
	String calculateSignature() {
		final Blake2b blake = Blake2b.Digest.newInstance(32);
		blake.update(DataManipulationUtil.hexStringToByteArray(this.source));
		blake.update(this.address.getParent().addressToPublicKey(this.address.getRepresentative()));
		blake.update(this.address.getParent().addressToPublicKey(this.address.getAddress()));
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
					"\\\"type\\\": \\\"open\\\"," + 
					"\\\"account\\\": \\\""+ this.address.getAddress() + "\\\"," + 
					"\\\"representative\\\": \\\"" + this.address.getRepresentative() + "\\\"," + 
					"\\\"source\\\": \\\"" + this.source + "\\\"," + 
					"\\\"work\\\": \\\"" + this.getWork() + "\\\"," + 
					"\\\"signature\\\": \\\"" + this.signature + "\\\"" + 
				"}\"" + 
			"}";
	}
}
