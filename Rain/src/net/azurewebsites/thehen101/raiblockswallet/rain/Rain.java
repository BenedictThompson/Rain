package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.util.ArrayList;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;

public class Rain {
	private final ArrayList<ServerConnection> serverConnections = new ArrayList<ServerConnection>();
	
	private final Account account;
	
	public Rain(Account account) {
		this.account = account;
	}
	
	public ArrayList<ServerConnection> getServerConnections() {
		return this.serverConnections;
	}
	
	public Account getAccount() {
		return this.account;
	}
}
