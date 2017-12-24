package net.azurewebsites.thehen101.raiblockswallet.rain.server;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public final class ServerConnection extends Thread {
	public static final int HEADER_LENGTH = 5;
	
	private final ArrayList<RequestWithHeader> sendQueue = new ArrayList<RequestWithHeader>();
	private final ArrayList<ServerResponseListener> listeners = new ArrayList<ServerResponseListener>();
	
	private final String IP;
	private final int port;
	private boolean shouldConnect;
	private Socket currentSocket;
	
	private boolean isReadingRequest;
	private long messageStartTime;
	private int bytesRead;
	private byte[] requestHeader;
	private byte[] request;
	private int requestLength;
	
	public ServerConnection(String serverIP, int serverPort) {
		this.IP = serverIP;
		this.port = serverPort;
		this.shouldConnect = true;
		this.requestHeader = new byte[HEADER_LENGTH];
	}
	
	@Override
	public void run() {
		while (this.shouldConnect) {
			try {
				if (this.currentSocket == null) {
					this.currentSocket = new Socket(IP, port);
				}
				
				if (this.sendQueue.size() > 0) {
					RequestWithHeader rwh = this.sendQueue.get(0);
					this.sendQueue.remove(rwh);
					OutputStream out = this.currentSocket.getOutputStream();
					out.write(rwh.get());
					out.flush();
					InputStreamReader in = new InputStreamReader(this.currentSocket.getInputStream());
					boolean messageCompleted = false;
					
					//the following code is mostly copied from the rain server as its what we need
					while (!messageCompleted) {
						int read = in.read();
						if (read > 256 || read == -1)
							throw new Exception("read() has invalid value: " + read);
						byte b = (byte) read;
						
						//read header
						if (this.bytesRead < HEADER_LENGTH) {
							if (this.messageStartTime == -1)
								this.messageStartTime = System.currentTimeMillis();
							if (!this.isReadingRequest) 
								this.isReadingRequest = true;
							this.requestHeader[this.bytesRead] = b;
							this.bytesRead++;
							if (this.bytesRead != HEADER_LENGTH)
								continue;
						}
						
						//when we've read the last byte of the header
						if (this.bytesRead == HEADER_LENGTH) {
							//make sure the header is valid
							int length = this.getHeaderLength(this.requestHeader);
							if (length == -1)
								throw new Exception("invalid request received");
							this.requestLength = length + 1;
							this.request = new byte[this.requestLength];
							this.bytesRead++;
							continue;
						}
						
						//read the request
						if (this.bytesRead < this.requestLength + HEADER_LENGTH) {
							this.request[(this.bytesRead - HEADER_LENGTH) - 1] = b;
							this.bytesRead++;
						}
						
						//when we've just read the last byte of the request
						if (this.bytesRead == this.requestLength + HEADER_LENGTH) {
							this.messageStartTime = -1;
							this.isReadingRequest = false;
							//finished reading request, this is our last byte so notify the listener
							boolean isNewBlock = this.requestHeader[0] == 0x00 ? false : true;
							this.sendToListeners(rwh, new RequestWithHeader(isNewBlock, this.request));
							
							//reset variables
							this.bytesRead = 0;
							this.requestLength = 0;
							this.request = null;
							messageCompleted = true;
						}
					}
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
	 * @param header a five byte header of bytes received from the server
	 * @return length of the header
	 */
	private int getHeaderLength(byte[] header) {
		byte[] l = new byte[4];
		//turn a byte array into an integer
		System.arraycopy(header, 1, l, 0, 4);
		int length = ByteBuffer.wrap(l).getInt();
		//return request length
		return length;
	}
	
	private void sendToListeners(RequestWithHeader weSent, RequestWithHeader serverGaveUs) {
		for (ServerResponseListener listener : this.listeners) {
			listener.onResponse(weSent, serverGaveUs);
		}
	}
	
	public void addListener(ServerResponseListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeListener(ServerResponseListener listener) {
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

	//TODO: replace this with heartbeats
	public boolean getIsConnected() {
		return this.currentSocket.isConnected();
	}
}
