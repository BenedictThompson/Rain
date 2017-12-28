package net.azurewebsites.thehen101.raiblockswallet.rain.server;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerNewBlock;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerServerResponse;

public final class ServerConnection extends Thread {
	public static final int HEADER_LENGTH = 5;

	private final ArrayList<RequestWithHeader> sendQueue = new ArrayList<RequestWithHeader>();
	private final ArrayList<ListenerServerResponse> listeners = new ArrayList<ListenerServerResponse>();

	private final ListenerNewBlock newBlockListener;

	private final String IP;
	private final int port;
	private boolean shouldConnect;
	private Socket currentSocket;

	private boolean receivedResponse = true;
	private boolean isReadingRequest;
	private long messageStartTime;
	private int bytesRead;
	private byte[] requestHeader;
	private byte[] request;
	private int requestLength;

	public ServerConnection(String serverIP, int serverPort, ListenerNewBlock newBlockListener) {
		this.IP = serverIP;
		this.port = serverPort;
		this.newBlockListener = newBlockListener;
		this.shouldConnect = true;
		this.requestHeader = new byte[HEADER_LENGTH];
	}

	@Override
	public void run() {
		while (this.currentSocket == null) {
			try {
				this.currentSocket = new Socket(IP, port);
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(30000);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}

		Thread socketListener = new Thread() {
			@Override
			public void run() {
				try {
					InputStreamReader in = new InputStreamReader(currentSocket.getInputStream());
					while (shouldConnect) {
						// the following code is mostly copied from the rain server as its what we need
						int read = in.read();
						if (read > 256 || read == -1)
							throw new Exception("read() has invalid value: " + read);
						byte b = (byte) read;
						
						// read header
						if (bytesRead < HEADER_LENGTH) {
							if (messageStartTime == -1)
								messageStartTime = System.currentTimeMillis();
							if (!isReadingRequest)
								isReadingRequest = true;
							requestHeader[bytesRead] = b;
							bytesRead++;
							if (bytesRead != HEADER_LENGTH)
								continue;
						}

						// when we've read the last byte of the header
						if (bytesRead == HEADER_LENGTH) {
							// make sure the header is valid
							int length = getHeaderLength(requestHeader);
							if (length == -1)
								throw new Exception("invalid request received");
							requestLength = length + 1;
							request = new byte[requestLength];
							bytesRead++;
							continue;
						}

						// read the request
						if (bytesRead < requestLength + HEADER_LENGTH) {
							request[(bytesRead - HEADER_LENGTH) - 1] = b;
							bytesRead++;
						}
						
						// when we've just read the last byte of the request
						if (bytesRead == requestLength + HEADER_LENGTH) {
							messageStartTime = -1;
							isReadingRequest = false;
							// finished reading request, this is our last byte so notify the listener
							boolean isNewBlock = requestHeader[0] == 0x00 ? false : true;
							if (isNewBlock)
								newBlockListener.onNewBlock(new RequestWithHeader(isNewBlock, request));
							else
								receivedResponse(new RequestWithHeader(isNewBlock, request));

							// reset variables
							bytesRead = 0;
							requestLength = 0;
							request = null;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					shouldConnect = false;
				}
			}
		};
		socketListener.start();

		while (this.shouldConnect) {
			try {
				if (this.currentSocket == null)
					this.currentSocket = new Socket(IP, port);

				if (this.sendQueue.size() > 0 && receivedResponse) {
					receivedResponse = false;
					RequestWithHeader rwh = this.sendQueue.get(0);
					OutputStream out = this.currentSocket.getOutputStream();
					out.write(rwh.get());
					out.flush();
				}
				Thread.sleep(50);
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(120000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Validates the passed header and returns the length of the request.
	 * 
	 * @param header
	 *            a five byte header of bytes received from the server
	 * @return length of the header
	 */
	private int getHeaderLength(byte[] header) {
		byte[] l = new byte[4];
		// turn a byte array into an integer
		System.arraycopy(header, 1, l, 0, 4);
		int length = ByteBuffer.wrap(l).getInt();
		// return request length
		return length;
	}

	private void receivedResponse(RequestWithHeader serverGaveUs) {
		receivedResponse = true;
		RequestWithHeader rwh = this.sendQueue.get(0);
		this.sendQueue.remove(rwh);
		for (ListenerServerResponse listener : this.listeners) {
			listener.onResponse(rwh, serverGaveUs);
		}
	}

	public void addListener(ListenerServerResponse listener) {
		this.listeners.add(listener);
	}

	public void removeListener(ListenerServerResponse listener) {
		this.listeners.remove(listener);
	}

	public void addToSendQueue(RequestWithHeader request) {
		this.sendQueue.add(request);
	}

	public String getIP() {
		return IP;
	}

	public int getPort() {
		return port;
	}

	public boolean getShouldConnect() {
		return this.shouldConnect;
	}

	public void setShouldConnect(boolean shouldConnect) {
		this.shouldConnect = shouldConnect;
	}

	// TODO: replace this with heartbeats
	public boolean getIsConnected() {
		return this.currentSocket.isConnected();
	}
}
