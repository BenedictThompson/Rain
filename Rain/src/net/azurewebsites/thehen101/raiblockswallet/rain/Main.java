package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.util.ArrayList;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;

public class Main {
	public static void main(String[] args) {
		try {
			ArrayList<ServerConnection> connections = new ArrayList<ServerConnection>();
			connections.add(new ServerConnection("192.168.1.4", 37076, null));
			
			ArrayList<Account> accounts = new ArrayList<Account>();
			accounts.add(new Account(
					DataManipulationUtil
					.hexStringToByteArray(
							"SNIP")));
			Rain rain = new Rain(connections, accounts, 8);
			
			for (ServerConnection connection : connections) {
				if (connection.getNewBlockListener() == null) {
					connection.setNewBlockListener(rain.getBlockListener());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
