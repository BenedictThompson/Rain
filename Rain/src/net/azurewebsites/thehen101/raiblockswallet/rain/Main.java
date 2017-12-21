package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.security.SecureRandom;

public class Main {
	
	public static void main(String[] args) {
		try {
			long time = System.currentTimeMillis();
			SecureRandom random = SecureRandom.getInstanceStrong();
			byte[] values = new byte[32];
			random.nextBytes(values);
			System.out.println(System.currentTimeMillis() - time);
			System.out.println(bytesToHex(values));
		} catch (Exception e) {
			e.printStackTrace();
		}
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
