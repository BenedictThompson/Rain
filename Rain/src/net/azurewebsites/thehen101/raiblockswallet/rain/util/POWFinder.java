package net.azurewebsites.thehen101.raiblockswallet.rain.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.Blake2b;

public class POWFinder extends Thread {
	private final Rain rain;
	private final HashMap<Address, String> powMap = new HashMap<Address, String>();
	private final Random random;
	private final int threadCount;
	private boolean alive;
	
	public POWFinder(Rain rain, int threadsToUse) {
		this.rain = rain;
		this.random = new Random();
		this.threadCount = threadsToUse;
		this.alive = true;
	}
	
	@Override
	public void run() {
		while (this.alive) {
			this.syncArrayAndMap();
			
			for (Entry<Address, String> entry : this.powMap.entrySet()) {
				Address key = entry.getKey();
				String hash = entry.getValue();
				if (hash != null) {
					if (hash.equals("") && key.getIsOpened()) {
						System.out.println("Getting POW for account: " + key.getAddress());
						String prevBlock = this.rain.getPreviousHash(key);
						if (prevBlock.equals("")) {
							key.setIsOpened(false);
							System.out.println(key.getAddress() + " needs to be opened");
						} else {
							byte[] powBytes = this.getPOW(DataManipulationUtil
									.hexStringToByteArray(prevBlock));
							String pow = DataManipulationUtil.bytesToHex(powBytes);
							System.out.println("Found POW: " + pow);
							this.powMap.put(key, pow);
						}
					}
				}
			}
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void syncArrayAndMap() {
		ArrayList<Address> adds = new ArrayList<Address>();
		for (Account account : this.rain.getAccounts()) {
			int max = account.getAddressesCount();
			for (int i = 0; i < max; i++) {
				Address add = account.getAddressForIndex(i);
				adds.add(add);
				if (this.powMap.get(add) == null) {
					this.powMap.put(add, "");
				}
			}
		}
		this.powMap.keySet().retainAll(adds);
	}
	
	public String getPowBlocking(Address address) {
		String pow = this.getWorkForAddress(address);
		while (pow.equals("")) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			pow = this.getWorkForAddress(address);
		}
		return pow;
	}
	
	public String getWorkForAddress(Address address) {
		String hash = this.powMap.get(address);
		this.powMap.put(address, "");
		return hash;
	}
	
	public void setAlive(boolean shouldBeAlive) {
		this.alive = shouldBeAlive;
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
		final byte[] pow = new byte[8], zero = new byte[8];
		Arrays.fill(zero, (byte) 0x00);
		Arrays.fill(pow, (byte) 0x00);
		Thread[] threads = new Thread[this.threadCount];
		for (int i = 0; i < this.threadCount; i++) {
			Thread powFinder = new Thread() {
				@Override
				public void run() {
					final Blake2b blake = Blake2b.Digest.newInstance(8);
					while (isEqual(pow, zero)) {
						byte[] bytes = new byte[8];
						random.nextBytes(bytes);
						for (byte b = -128; b < 127; b++) {
							bytes[7] = b;
							blake.reset();
							blake.update(bytes);
							blake.update(hash);
							byte[] digest = DataManipulationUtil.swapEndian(blake.digest());
							if (overThreshold(digest))
								System.arraycopy(DataManipulationUtil.swapEndian(bytes), 0, pow, 0, 8);
						}
					}
				}
			};
			threads[i] = powFinder;
			powFinder.start();
		}
		while (isEqual(pow, zero)) {
			try {
				Thread.sleep(5);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// for (Thread t : threads) //deprecated
		// t.stop();
		
		return pow;
	}
	
	private boolean isEqual(byte[] b0, byte[] b1) {
		for (int i = 0; i < b0.length; i++) {
			if (b0[i] != b1[i])
				return false;
		}
		return true;
	}
}
