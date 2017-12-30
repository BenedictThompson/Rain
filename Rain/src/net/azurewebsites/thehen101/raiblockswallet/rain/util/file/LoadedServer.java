package net.azurewebsites.thehen101.raiblockswallet.rain.util.file;

public final class LoadedServer {
	private final String hostnameOrIP;
	private final int port;

	public LoadedServer(String hostnameOrIP, int port) {
		this.hostnameOrIP = hostnameOrIP;
		this.port = port;
	}
	
	public String getHostnameOrIP() {
		return hostnameOrIP;
	}

	public int getPort() {
		return port;
	}
}
