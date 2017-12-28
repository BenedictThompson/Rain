package net.azurewebsites.thehen101.raiblockswallet.rain.transaction;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.Blake2b;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.ED25519;

public class TransactionOpen extends Transaction {
	private final Address address;
	private final String signature, source;

	public TransactionOpen(Type type, String work, String source, Address address) {
		super(type, work);
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
		
		byte[] privateKey = this.address.getPrivateKey();
		byte[] publicKey = this.address.getPrivateKey();
		byte[] newPrivateKey = new byte[privateKey.length + publicKey.length];

		System.arraycopy(privateKey, 0, newPrivateKey, 0, privateKey.length);
		System.arraycopy(publicKey, 0, newPrivateKey, privateKey.length, publicKey.length);

		byte[] newPublicKey = ED25519.publickey(newPrivateKey);
		byte[] signature = ED25519.signature(blake.digest(), newPrivateKey, newPublicKey);
		return DataManipulationUtil.bytesToHex(signature);
	}

	@Override
	public String getAsJSON() {
		// TODO Auto-generated method stub
		return null;
	}
}
