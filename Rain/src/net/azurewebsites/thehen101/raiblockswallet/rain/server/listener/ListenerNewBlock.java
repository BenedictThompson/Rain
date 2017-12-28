package net.azurewebsites.thehen101.raiblockswallet.rain.server.listener;

import net.azurewebsites.thehen101.raiblockswallet.rain.server.RequestWithHeader;

public interface ListenerNewBlock extends Listener {
	public void onNewBlock(RequestWithHeader newBlockNotification);
}
