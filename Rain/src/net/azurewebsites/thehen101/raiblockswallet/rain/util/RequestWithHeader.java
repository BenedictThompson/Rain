package net.azurewebsites.thehen101.raiblockswallet.rain.util;

import java.nio.ByteBuffer;

public class RequestWithHeader {
	private final boolean isNewBlockNotification;
	private final byte[] requestBytes;
	
	public RequestWithHeader(boolean newBlockNotification, byte[] request) {
		this.isNewBlockNotification = newBlockNotification;
		this.requestBytes = request;
	}
	
	public RequestWithHeader(boolean newBlockNotification, String request) {
		this(newBlockNotification, request.getBytes());
	}
	
	public byte[] get() {
		byte[] b = new byte[1 + 4 + this.requestBytes.length];
		b[0] = (byte) (this.isNewBlockNotification ? 1 : 0);
		System.arraycopy(ByteBuffer.allocate(4).putInt(this.requestBytes.length).array(),
				0, b, 1, 4);
		System.arraycopy(this.requestBytes, 0, b, 5, this.requestBytes.length);
		return b;
	}
}
