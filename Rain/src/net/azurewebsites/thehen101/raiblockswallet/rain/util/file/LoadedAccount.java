package net.azurewebsites.thehen101.raiblockswallet.rain.util.file;

import java.util.ArrayList;

public final class LoadedAccount {
	private final byte[] seed;
	private final ArrayList<Boolean> generatedIndex;

	public LoadedAccount(byte[] seed, ArrayList<Boolean> generatedIndex) {
		this.seed = seed;
		this.generatedIndex = generatedIndex;
	}

	public byte[] getSeed() {
		return this.seed;
	}

	public ArrayList<Boolean> getGeneratedIndex() {
		return this.generatedIndex;
	}
}
