package net.azurewebsites.thehen101.raiblockswallet.rain.account;

import static net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil.binaryToHex;
import static net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil.bytesToHex;
import static net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil.hexStringToByteArray;
import static net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil.hexToBinary;
import static net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil.swapEndian;
//import static for readability, we don't want DataManipulationUtil everywhere!

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.Blake2b;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.ED25519;

public final class Account {
	public static final String REPRESENTATIVE = "xrb_3arg3asgtigae3xckabaaewkx3bzsh7nwz7jkmjos79ihyaxwphhm6qgjps4";
	public static final char[] ACCOUNT_MAP = "13456789abcdefghijkmnopqrstuwxyz".toCharArray();
	public static final HashMap<String, Character> ACCOUNT_CHAR_TABLE = 
			new HashMap<String, Character>();
	public static final HashMap<Character, String> ACCOUNT_BIN_TABLE = 
			new HashMap<Character, String>();
	
	private final HashMap<Integer, Address> accountAddresses = 
			new HashMap<Integer, Address>(); //an index will return an address
	
	private final byte[] seed;
	
	static {
		//populate the ACCOUNT_CHAR_TABLE and ACCOUNT_BIN_TABLE
		for (int i = 0; i < ACCOUNT_MAP.length; i++) {
			String bin = Integer.toBinaryString(i);
			while (bin.length() < 5)
				bin = "0" + bin; //pad with 0
			ACCOUNT_CHAR_TABLE.put(bin, ACCOUNT_MAP[i]);
			ACCOUNT_BIN_TABLE.put(ACCOUNT_MAP[i], bin);
		}
	}
	
	public Account(byte[] seed) {
		this.seed = seed;
	}
	
	/**
	 * Gets an address for a given index. The majority of the times this method
	 * will be called, index will be zero as that's what is used for the first
	 * address. If a second address is made, the index *should* be one, so a
	 * different Address is returned (but both are generated from the same
	 * account seed).
	 * 
	 * @param index The index to get the address from
	 * @return The Address corresponding to the given index, or null if the
	 * passed index was invalid
	 */
	public Address getAddressForIndex(int index) {
		//iirc the reference spec uses an unsigned int
		if (index < 0)
			return null;
		
		//if we have already generated an Address for this index, return it.
		Address a = this.accountAddresses.get(index);
		if (a != null)
			return a;
		
		//if not, generate an address for the given index and return it.
		final Blake2b blake2b = Blake2b.Digest.newInstance(32); //will return 32 bytes digest
		blake2b.update(this.seed); //add seed
		blake2b.update(ByteBuffer.allocate(4).putInt(index).array()); //and add index
		byte[] privateKey = blake2b.digest(); //digest 36 bytes into 32
		byte[] publicKey = ED25519.publickey(privateKey); //return the public key
		
		
		Address newAddress = new Address(
				this, index, publicKey, privateKey, this.publicKeyToXRBAddress(publicKey), REPRESENTATIVE);
		//add the generated address to the addres table (hashmap).
		this.accountAddresses.put(newAddress.getIndex(), newAddress);
		return newAddress; //and finally return the new address
	}
	
	public byte[] addressToPublicKey(String address) {
		if (address.length() != 64)
			return null;
		if (!address.substring(0, 4).equals("xrb_"))
			return null;
		
		String pub = address.substring(4, address.length() - 8);
		String checksum = address.substring(address.length() - 8);
		
		String pubBin = "";
		for (int i = 0; i < pub.length(); i++) {
			pubBin += ACCOUNT_BIN_TABLE.get(pub.charAt(i));
		}
		pubBin = pubBin.substring(4);
		
		String checkBin = "";
		for (int i = 0; i < checksum.length(); i++) {
			checkBin += ACCOUNT_BIN_TABLE.get(checksum.charAt(i));
		}
		byte[] checkHex = swapEndian(hexStringToByteArray(binaryToHex(checkBin)));
		byte[] publicKey = hexStringToByteArray(binaryToHex(pubBin));
		
		
		final Blake2b blake = Blake2b.Digest.newInstance(5);
		blake.update(publicKey);
		byte[] digest = blake.digest();
		if (Arrays.equals(digest, checkHex)) 
			return publicKey;
		
		return null;
	}
	
	/**
	 * Derives and returns an XRB address from a passed public key.
	 * 
	 * @param publicK Public key to be used in address derivation.
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
