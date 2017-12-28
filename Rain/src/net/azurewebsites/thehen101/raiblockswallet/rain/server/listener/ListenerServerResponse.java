package net.azurewebsites.thehen101.raiblockswallet.rain.server.listener;

import net.azurewebsites.thehen101.raiblockswallet.rain.server.RequestWithHeader;

public interface ListenerServerResponse extends Listener {
	public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest);
}
