package net.azurewebsites.thehen101.raiblockswallet.rain.account;

import static net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil.bytesToHex;
import static net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil.hexToBinary;
import static net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil.swapEndian;
//import static for readability, we don't want DataManipulationUtil everywhere!

import java.nio.ByteBuffer;
import java.util.HashMap;

import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.Blake2b;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.ED25519;


public class Account {
	public static final char[] ACCOUNT_MAP = "13456789abcdefghijkmnopqrstuwxyz".toCharArray();
	public static final HashMap<String, Character> ACCOUNT_CHAR_TABLE = 
			new HashMap<String, Character>();
	
	private final HashMap<Integer, Address> accountAddresses = 
			new HashMap<Integer, Address>(); //an index will return an address
	
	private final byte[] seed;
	
	static {
		//populate the accountCharTable
		for (int i = 0; i < ACCOUNT_MAP.length; i++) {
			String bin = Integer.toBinaryString(i);
			while (bin.length() < 5)
				bin = "0" + bin; //pad with 0
			ACCOUNT_CHAR_TABLE.put(bin, ACCOUNT_MAP[i]);
		}
	}
	
	public Account(byte[] seed) {
		this.seed = seed;
	}
	
	public Address getAddressForIndex(int index) {
		final Blake2b blake2b = Blake2b.Digest.newInstance(32);
		blake2b.update(this.seed);
		blake2b.update(ByteBuffer.allocate(4).putInt(index).array());
		byte[] privateKey = blake2b.digest();
		byte[] publicKey = ED25519.publickey(privateKey);
		
		return new Address(index, publicKey, privateKey, this.publicKeyToXRBAddress(publicKey));
	}
	
	/**
	 * Derives and returns an XRB address from a passed public key.
	 * 
	 * @param publicK Public key to be used in addres derivation.
	 * @return An XRB address.
	 */
	private String publicKeyToXRBAddress(byte[] publicKey) {
		String keyBinary = hexToBinary(bytesToHex(publicKey)); //we get the address by picking
		//five bit (not byte!) chunks of the public key (in binary)
		
		final Blake2b blake2b = Blake2b.Digest.newInstance(5);
		blake2b.update(publicKey); //the blake2b digest will be used for the checksum
		byte[] digest = swapEndian(blake2b.digest()); //the original wallet flips it
		String bin = hexToBinary(bytesToHex(digest)); //we get the checksum by, similarly
		//to getting the address, picking 5 bit chunks of the five byte digest
		
		//calculate the checksum:
		String checksum = ""; //string that we will populate with the checksum chars
		while (bin.length() < digest.length * 8)
			bin = "0" + bin; //leading zeroes are sometimes omitted (idk why)
		for (int i = 0; i < ((digest.length * 8) / 5); i++) {
			String fiveBit = bin.substring(i * 5, (i * 5) + 5);
			checksum += ACCOUNT_CHAR_TABLE.get(fiveBit);//go through the [40] bits in
			//our digest and turn each five into a char using the accountCharTable
		}
		
		//calculate the address
		String account = ""; //string to populate with address chars
		while (keyBinary.length() < 260) //binary for address should always be 260 bits
			keyBinary = "0" + keyBinary; //so pad it if it isn't
		for (int i = 0; i < keyBinary.length(); i += 5) {
			String fiveBit = keyBinary.substring(i, i + 5);
			account += ACCOUNT_CHAR_TABLE.get(fiveBit); //go through the 260 bits that
			//represent our public key five bits at a time and convert each five bits
			//into a char that is retrieved from the accountCharTable
		}
		
		//return the address prefixed with xrb_ and suffixed with the checksum
		return "xrb_" + account + checksum;
	}
}
