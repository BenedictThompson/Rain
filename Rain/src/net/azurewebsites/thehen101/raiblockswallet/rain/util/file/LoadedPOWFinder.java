package net.azurewebsites.thehen101.raiblockswallet.rain.util.file;

import java.util.ArrayList;
import java.util.HashMap;

public class LoadedPOWFinder {
	private final int threadsToUse;
	private final HashMap<String, String> powMap;
	private final HashMap<String, Boolean> openPowMap;
	private final ArrayList<String> generatePowAddresses;

	public LoadedPOWFinder(int threadsToUse, HashMap<String, String> powMap,
			HashMap<String, Boolean> openPowMap, ArrayList<String> generatePowAddresses) {
		this.threadsToUse = threadsToUse;
		this.powMap = powMap;
		this.openPowMap = openPowMap;
		this.generatePowAddresses = generatePowAddresses;
	}
	
	public int getThreadsToUse() {
		return threadsToUse;
	}

	public HashMap<String, String> getPowMap() {
		return powMap;
	}

	public HashMap<String, Boolean> getOpenPowMap() {
		return openPowMap;
	}

	public ArrayList<String> getGeneratePowAddresses() {
		return generatePowAddresses;
	}
}
