package net.azurewebsites.thehen101.raiblockswallet.rain.server;

public interface ServerResponseListener {
	public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest);
}
