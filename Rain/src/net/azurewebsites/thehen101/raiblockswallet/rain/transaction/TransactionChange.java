package net.azurewebsites.thehen101.raiblockswallet.rain.transaction;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.Blake2b;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.ED25519;

public class TransactionChange extends Transaction {
	private final Address address;
	private final String signature, previous;
	
	/**
	 * @param address Should have the representative already updated
	 */
	public TransactionChange(String work, Address address, String previousHash) {
		super(Transaction.Type.CHANGE, work);
		this.address = address;
		this.previous = previousHash;
		this.signature = calculateSignature();
	}

	@Override
	String calculateSignature() {
		final Blake2b blake = Blake2b.Digest.newInstance(32);
		blake.update(DataManipulationUtil.hexStringToByteArray(this.previous));
		blake.update(this.address.getParent().addressToPublicKey(this.address.getRepresentative()));
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
					"\\\"type\\\": \\\"change\\\"," + 
					"\\\"previous\\\": \\\""+ this.previous + "\\\"," + 
					"\\\"representative\\\": \\\"" + this.address.getRepresentative() + "\\\"," + 
					"\\\"work\\\": \\\"" + this.getWork() + "\\\"," + 
					"\\\"signature\\\": \\\"" + this.signature + "\\\"" + 
				"}\"" + 
			"}";
	}
}
