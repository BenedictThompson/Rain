package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;

import net.azurewebsites.thehen101.raiblockswallet.rain.util.Blake2b;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.spec.EdDSAGenParameterSpec;

public class Main {
	
	public static void main(String[] args) {
		try {
			SecureRandom random = SecureRandom.getInstanceStrong();
			KeyPairGenerator gen = new KeyPairGenerator();
			gen.initialize(new EdDSAGenParameterSpec("ED25519"), random);
			KeyPair keys = gen.generateKeyPair();
			PrivateKey pk = keys.getPrivate();
			System.out.println("algorithm: " + pk.getAlgorithm());
			System.out.println("private key: " + bytesToHex(keys.getPrivate().getEncoded()));
			System.out.println("public key: " + bytesToHex(keys.getPublic().getEncoded()));
			System.out.println("xrb address: " + publicKeyToXRBAddress(bytesToHex(keys.getPublic().getEncoded())));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static final String ACCOUNT_MAP = "13456789abcdefghijkmnopqrstuwxyz";
	public static final String[] ACCOUNT_LOOKUP = new String[32];
	
	static {
		for (int i = 0; i < ACCOUNT_MAP.length(); i++) {
			String bin = Integer.toBinaryString(i);
			while (bin.length() < 5) {
				bin = "0" + bin;
			}
			ACCOUNT_LOOKUP[i] = bin;
		}
	}
	
	/**
	 * a bad port of the python lite wallet and how it gets the address from
	 * the pub key
	 * 
	 * @param publicK public key
	 * @return xrb address
	 */
	public static String publicKeyToXRBAddress(String publicK) {
		String publicKey = publicK;
		if (publicKey.length() != 64) { // java likes to prefix 24 extra bytes on
										// the start of a public key, and they are
										// always the same, could someone please
										// explain to me why this happens i have
										// no idea - but this code removes them
										// if they are there
			publicKey = publicK.substring(24);
		}
		String keyBinary = hexToBinary(publicKey);
		byte[] bytes = hexStringToByteArray(publicKey);
		final Blake2b blake2b = Blake2b.Digest.newInstance(5);  
		blake2b.update(bytes);
		byte[] digest = swapEndian(blake2b.digest());
		String bin = hexToBinary(bytesToHex(digest));
		String checksum = "";
		while (bin.length() < digest.length * 8)
			bin = "0" + bin; //leading zeroes are sometimes omitted
		for (int i = 0; i < ((digest.length * 8) / 5); i++) {
			String fiveBit = bin.substring(i * 5, (i * 5) + 5);
			for (int o = 0; o < ACCOUNT_LOOKUP.length; o++) {
				String oo = ACCOUNT_LOOKUP[o];
				if (oo.equals(fiveBit)) {
					checksum += ACCOUNT_MAP.charAt(o);
				}
			}
		}
		String account = "";
		while (keyBinary.length() < 260)
			keyBinary = "0" + keyBinary;		
		for (int i = 0; i < keyBinary.length(); i += 5) {
			String fiveBit = keyBinary.substring(i, i + 5);
			for (int o = 0; o < ACCOUNT_LOOKUP.length; o++) {
				String oo = ACCOUNT_LOOKUP[o];
				if (oo.equals(fiveBit)) {
					account += ACCOUNT_MAP.charAt(o);
				}
			}
		}
		return "xrb_" + account + checksum;
	}
	
	public static byte[] swapEndian(byte[] b) {
		byte[] bb = new byte[b.length];
		for (int i = b.length; i > 0; i--) {
			bb[b.length - i] = b[i - 1];
		}
		return bb;
	}
	
	public static String hexToBinary(String hex) {
		String value = new BigInteger(hex, 16).toString(2);
		String formatPad = "%" + (hex.length() * 4) + "s";
		return (String.format(formatPad, value).replace(" ", ""));
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
