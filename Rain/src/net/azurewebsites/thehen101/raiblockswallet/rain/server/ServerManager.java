package net.azurewebsites.thehen101.raiblockswallet.rain.server;

import java.util.ArrayList;
import java.util.Random;

import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerServerResponse;

public final class ServerManager {
	private final ArrayList<ServerConnection> connections;
	private final Random random; //ensure load balancing
	
	public ServerManager(ArrayList<ServerConnection> connections) {
		this.connections = connections;
		this.random = new Random();
	}
	
	public void addToConnectedServerQueue(RequestWithHeader request) {
		ServerConnection connection = null;
		boolean foundGoodServer = false;
		while (!foundGoodServer) {
			int next = this.random.nextInt(this.connections.size());
			connection = this.connections.get(next);
			if (connection.getIsConnected())
				foundGoodServer = true;
		}
		connection.addToSendQueue(request);
	}
	
	public void addListenerToAll(ListenerServerResponse listener) {
		for (ServerConnection connection : this.connections)
			connection.addListener(listener);
	}
	
	public void removeListenerFromAll(ListenerServerResponse listener) {
		for (ServerConnection connection : this.connections)
			connection.removeListener(listener);
	}
	
	public ArrayList<ServerConnection> getConnections() {
		return this.connections;
	}
}
