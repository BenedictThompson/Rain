package net.azurewebsites.thehen101.raiblockswallet.rain.util;

import java.util.Random;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.Blake2b;

public class POWFinder extends Thread {
	private final Address address;
	private final Random random;
	private String openWork;
	private String sendWork;
	private String receiveWork;
	private String changeWork;
	private boolean alive;
	
	public POWFinder(Address address) {
		this.random = new Random();
		this.address = address;
		this.alive = true;
	}
	
	//UNFINISHED
	@Override
	public void run() {
		while (this.alive) {
			if (this.openWork == null) {
				System.out.println("Starting open POW generation...");
				byte[] pow = this.getPOW(this.address.getPublicKey());
				System.out.println(DataManipulationUtil.bytesToHex(pow));
				System.out.println("Finished open POW generation.");
			}
		}
	}
	
	private boolean overThreshold(byte[] bytes) {
	    long result = 0; //faster than ByteBuffer apparently:
	    for (int i = 0; i < 8; i++) {
	        result <<= 8;
	        result |= (bytes[i] & 0xFF);
	    }
		return Long.compareUnsigned(result, 0xFFFFFFC000000000L) > 0; //wew java 8!
	}
	
	private byte[] getPOW(byte[] hash) {
		final Blake2b blake = Blake2b.Digest.newInstance(8);
		while (true) {
			byte[] bytes = new byte[8];
			this.random.nextBytes(bytes);
			for (byte b = -128; b < 127; b++) {
				bytes[7] = b;
				blake.reset();
				blake.update(bytes);
				blake.update(hash);
				byte[] digest = DataManipulationUtil.swapEndian(blake.digest());
				if (overThreshold(digest))
					return DataManipulationUtil.swapEndian(bytes);
			}
		}
	}
	
	public void setAlive(boolean shouldBeAlive) {
		this.alive = shouldBeAlive;
	}
	
	public class POW {
		private final String pow;
		
		public POW(String pow) {
			this.pow = pow;
		}
		
		public String getPOW() {
			return this.pow;
		}
	}
}
