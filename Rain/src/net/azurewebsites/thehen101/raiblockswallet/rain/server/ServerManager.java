package net.azurewebsites.thehen101.raiblockswallet.rain.server;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;

import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerServerResponse;

public final class ServerManager {
	private final ArrayList<ServerConnectionWithInfo> connections;
	private int bestServerIndex = 0;
	
	public ServerManager(ArrayList<ServerConnection> connections) {
		ArrayList<ServerConnectionWithInfo> list = new ArrayList<ServerConnectionWithInfo>();
		for (ServerConnection connection : connections) {
			ServerConnectionWithInfo scwi = this.getInfoForConnection(connection, 30000);
			if (scwi != null)
				list.add(scwi);
		}
		System.out.println("ServerManager has " + list.size() + " reachable RainServer connections.");
		if (list.size() == 0) {
			JOptionPane.showMessageDialog(null, "Could not connect to any RainServers."
					+ " Please click OK to close Rain.", "Rain: Fatal error", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
		int bestIndex = 0;
		long bestResponseTime = -1;
		for (int i = 0; i < list.size(); i++) {
			ServerConnectionWithInfo scwi = list.get(i);
			if (bestResponseTime == -1 || scwi.getResponseTime() < bestResponseTime) {
				bestResponseTime = scwi.getResponseTime();
				bestIndex = i;
			}
		}
		System.out.println("Best server chosen with a reponse time of " 
				+ bestResponseTime + "ms: " + list.get(bestIndex).getConnection().getIP());
		this.bestServerIndex = bestIndex;
		
		for (int i = 0; i < list.size(); i++) {
			if (i != this.bestServerIndex) {
				ServerConnectionWithInfo scwi = list.get(i);
				scwi.getConnection().setShouldConnect(false);
				System.out.println("Closed unneeded server connection: " + scwi.getConnection().getIP());
			}
		}
		
		this.connections = list;
	}
	
	private ServerConnectionWithInfo getInfoForConnection(ServerConnection connection, int timeout) {
		if (connection.getState().equals(State.NEW))
			connection.start();
		ServerConnectionWithInfo[] scwi = new ServerConnectionWithInfo[1];
		scwi[0] = null;
		long startTime = System.currentTimeMillis();
		Thread thread = new Thread() {
			@Override
			public void run() {
				int[] connected = new int[1];
				connected[0] = -1;

				RequestWithHeader request = new RequestWithHeader(false, "{\"command\":\"connectedcount\"}");
				ListenerServerResponse listener = new ListenerServerResponse() {
					@Override
					public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest) {
						if (!Arrays.equals(request.getRequestBytes(), initialRequest.getRequestBytes()))
							return;

						String json = new String(receivedRequest.getRequestBytes());
						String a = json.substring(0, json.lastIndexOf("\""));
						int b = a.lastIndexOf("\"") + 1;
						int conn = Integer.parseInt(a.substring(b));
						System.out.println("Connected count: " + conn);
						connected[0] = conn;
					}
				};

				connection.addListener(listener);
				long time = System.nanoTime() / 1000000;
				connection.addToSendQueue(request);
				while (connected[0] == -1) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				long timeTaken = (System.nanoTime() / 1000000) - time;
				System.out.println(
						connection.getIP() + " response time: " + timeTaken + "ms, connected count: " + connected[0]);
				connection.removeListener(listener);
				scwi[0] = new ServerConnectionWithInfo(connection, timeTaken, connected[0]);
			}
		};
		thread.start();
		
		while (scwi[0] == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if ((startTime + timeout) < System.currentTimeMillis()) {
				System.out.println("### WARNING: Could not connect to RainServer at: " 
						+ connection.getIP() + ":" + connection.getPort());
				return null;
			}
		}
		
		return scwi[0];
	}
	
	public int getBestServer() {
		return this.bestServerIndex;
	}
	
	public void addToConnectedServerQueue(RequestWithHeader request) {
		this.connections.get(this.bestServerIndex).getConnection().addToSendQueue(request);
	}
	
	public void addListenerToAll(ListenerServerResponse listener) {
		for (ServerConnectionWithInfo scwi : this.connections)
			scwi.getConnection().addListener(listener);
	}
	
	public void removeListenerFromAll(ListenerServerResponse listener) {
		for (ServerConnectionWithInfo scwi : this.connections)
			scwi.getConnection().removeListener(listener);
	}
	
	public ArrayList<ServerConnectionWithInfo> getConnectionsWithInfo() {
		return this.connections;
	}
	
	public class ServerConnectionWithInfo {
		private final ServerConnection connection;
		private final long responseTime, connectedCount;
		
		public ServerConnectionWithInfo(ServerConnection connection, long responseTime, long connectedCount) {
			this.connection = connection;
			this.responseTime = responseTime;
			this.connectedCount = connectedCount;
		}
		
		public ServerConnection getConnection() {
			return this.connection;
		}

		public long getResponseTime() {
			return this.responseTime;
		}
		
		public long getConnectedCount() {
			return this.connectedCount;
		}
	}
}
