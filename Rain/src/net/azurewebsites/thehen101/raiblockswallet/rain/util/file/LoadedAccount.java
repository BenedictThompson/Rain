package net.azurewebsites.thehen101.raiblockswallet.rain.util.file;

public final class LoadedAccount {
	private final byte[] seed;
	private final int maxIndex;

	public LoadedAccount(byte[] seed, int maxIndex) {
		this.seed = seed;
		this.maxIndex = maxIndex;
	}

	public byte[] getSeed() {
		return seed;
	}

	public int getMaxIndex() {
		return maxIndex;
	}
}
