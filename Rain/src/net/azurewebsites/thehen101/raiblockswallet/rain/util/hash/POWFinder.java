package net.azurewebsites.thehen101.raiblockswallet.rain.util.hash;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.EventOurBlockReceived;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Event;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.Listener;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.SettingsLoader;

public class POWFinder extends Thread implements Listener {
	private final Rain rain;
	private final ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
	private final HashMap<Address, String> powMap;
	private final HashMap<Address, Boolean> openPowMap;
	private final ArrayList<Address> generatePowAddresses;
	private final Random random;
	private final int threadCount;
	private boolean alive;
	private boolean nullInit;
	
	public POWFinder(Rain rain, int threadsToUse) {
		this(rain, threadsToUse, null, null, null, true);
	}
	
	public POWFinder(Rain rain, int threadsToUse, HashMap<Address, String> powMap,
			HashMap<Address, Boolean> openPowMap, ArrayList<Address> generatePowAddresses, 
			boolean nullInit) {
		this.rain = rain;
		this.random = new Random();
		this.threadCount = threadsToUse;
		this.alive = true;
		if (!nullInit) {
			this.powMap = powMap;
			this.openPowMap = openPowMap;
			this.generatePowAddresses = generatePowAddresses;
		} else {
			this.powMap = new HashMap<Address, String>();
			this.openPowMap = new HashMap<Address, Boolean>();
			this.generatePowAddresses = new ArrayList<Address>();
		}
		this.rain.getEventManager().addListener(this);
		this.nullInit = nullInit;
	}
	
	@Override
	public void run() {
		//add all addresses initially
		if (this.nullInit) {
			this.nullInit = false;
			for (int i = 0; i < this.rain.getAccounts().size(); i++) {
				Account a = this.rain.getAccounts().get(i);
				for (int ii = 0; ii < a.getMaxAddressIndex(); ii++) {
				boolean addressAtIndex = a.isAddressAtIndex(ii);
					if (addressAtIndex) {
						Address address = a.getAddressAtIndex(ii);
						this.generatePowAddresses.add(address);
					}
				}
			}
		}
		while (this.alive) {
			this.syncArrayAndMap();
			for (int i = 0; i < this.generatePowAddresses.size(); i++) {
				this.syncArrayAndMap();
				Address rustypancake = this.generatePowAddresses.get(i);
				String hash = this.powMap.get(rustypancake);
				if (hash != null) {
					if (hash.equals("")) {
						this.notifyListeners(true);
						System.out.println("Getting POW for account: " + rustypancake.getAddress());
						String prevBlock = this.rain.getPreviousHash(rustypancake);
						boolean calculateOpenAlready = this.openPowMap.get(rustypancake) == null ? false
								: this.openPowMap.get(rustypancake);
						if (prevBlock.equals("") && !calculateOpenAlready) {
							System.out.println(rustypancake.getAddress() 
									+ " is not open, getting open POW");
							rustypancake.setIsOpened(false);
							byte[] powBytes = this.getPOW(rustypancake.getPublicKey());
							String pow = DataManipulationUtil.bytesToHex(powBytes);
							System.out.println("Found open POW: " + pow);
							this.powMap.put(rustypancake, pow);
							this.openPowMap.put(rustypancake, true);
						} else {
							byte[] powBytes = this.getPOW(DataManipulationUtil
									.hexStringToByteArray(prevBlock));
							String pow = DataManipulationUtil.bytesToHex(powBytes);
							System.out.println("Found POW: " + pow);
							this.powMap.put(rustypancake, pow);
						}
						this.generatePowAddresses.remove(rustypancake);
						SettingsLoader.INSTANCE.cachePOW(this);
						this.notifyListeners(false);
					}
				}
				this.syncArrayAndMap();
			}
			this.notifyListeners(false);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isPOWForAddress(Address address) {
		String hash = this.powMap.get(address);
		if (hash != null)
			if (!hash.equals(""))
				return true;
		return false;
	}
	
	public void syncArrayAndMap() {
		int a0 = this.powMap.size();
		int b = this.openPowMap.size();
		int c = this.generatePowAddresses.size();
		ArrayList<Address> adds = new ArrayList<Address>();
		for (int i = 0; i < this.rain.getAccounts().size(); i++) {
			Account a = this.rain.getAccounts().get(i);
			for (int ii = 0; ii < a.getMaxAddressIndex(); ii++) {
				boolean addressAtIndex = a.isAddressAtIndex(ii);
				if (addressAtIndex) {
					Address address = a.getAddressAtIndex(ii);
					adds.add(address);
					if (this.powMap.get(address) == null) {
						this.powMap.put(address, "");
						if (!this.generatePowAddresses.contains(address))
							this.generatePowAddresses.add(address);
					}
				}
			}
		}
		this.powMap.keySet().retainAll(adds);
		this.openPowMap.keySet().retainAll(adds);
		this.generatePowAddresses.retainAll(adds);
		if (a0 != this.powMap.size() 
				|| b != this.openPowMap.size() 
				|| c != this.generatePowAddresses.size()) {
			SettingsLoader.INSTANCE.cachePOW(this);
		}
	}
	
	public String getPowBlocking(Address address) {
		String pow = this.getWorkForAddress(address);
		while (pow.equals("")) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			pow = this.getWorkForAddress(address);
		}
		return pow;
	}
	
	public String getWorkForAddress(Address address) {
		this.syncArrayAndMap();
		String hash = this.powMap.get(address);
		this.powMap.put(address, "");
		return hash;
	}
	
	public void setAlive(boolean shouldBeAlive) {
		this.alive = shouldBeAlive;
	}
	
	public void addActionListener(ActionListener al) {
		this.listeners.add(al);
	}
	
	private void notifyListeners(boolean genPow) {
		for (int i = 0; i < this.listeners.size(); i++) {
			this.listeners.get(i).actionPerformed(new ActionEvent(this, genPow ? 1 : 0, "pow"));
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
								System.arraycopy(DataManipulationUtil.swapEndian(bytes), 0,
										pow, 0, 8);
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
		
		return pow;
	}
	
	private boolean isEqual(byte[] b0, byte[] b1) {
		for (int i = 0; i < b0.length; i++) {
			if (b0[i] != b1[i])
				return false;
		}
		return true;
	}
	
	private void invalidateAccountWork(Address address) {
		System.out.println("New block received, address work invalidated: " + address.getAddress());
		this.powMap.put(address, "");
	}

	public HashMap<Address, String> getPOWMap() {
		return this.powMap;
	}

	public HashMap<Address, Boolean> getOpenPOWMap() {
		return this.openPowMap;
	}

	public ArrayList<Address> getGeneratePowAddresses() {
		return this.generatePowAddresses;
	}
	
	public int getThreadCount() {
		return this.threadCount;
	}

	@Override
	public void onEvent(Event event) {
		if (event instanceof EventOurBlockReceived) {
			EventOurBlockReceived eobr = (EventOurBlockReceived) event;
			Address address = eobr.getAdd();
			this.invalidateAccountWork(address);
			this.generatePowAddresses.add(address);
			System.out.println("Address POW invalidated and readded to POWGen: " + address.getAddress());
		}
	}
}
