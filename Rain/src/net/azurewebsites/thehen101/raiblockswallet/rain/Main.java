package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.util.ArrayList;
import java.util.Random;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.LoadedAccount;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.LoadedServer;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.SettingsLoader;

public class Main {
	public static void main(String[] args) {
		try {
			//NOTE: the first thing to happen must be the password ****MUST**** set in SettingsLoader
			SettingsLoader.INSTANCE.setPassword("ChangeME"); //TODO: do this in a gui so the user can enter it
			
			LoadedServer[] servers = SettingsLoader.INSTANCE.getDefaultServers();
			LoadedAccount[] accs = SettingsLoader.INSTANCE.getAccounts();
			String[] representatives = SettingsLoader.INSTANCE.getDefaultRepresentatives();
			
			ArrayList<ServerConnection> serverConnections = new ArrayList<ServerConnection>();
			ArrayList<Account> accounts = new ArrayList<Account>();
			
			for (LoadedServer ls : servers)
				serverConnections.add(new ServerConnection(ls.getHostnameOrIP(), ls.getPort(), null));
			
			for (LoadedAccount la : accs) 
				accounts.add(new Account(la.getSeed(), 
						representatives[new Random().nextInt(representatives.length)], la.getMaxIndex()));
			
			int powThreads = Runtime.getRuntime().availableProcessors() - 1;
			if (powThreads == 0)
				powThreads = 1;
			
			Rain rain = new Rain(serverConnections, accounts, powThreads, representatives);
			
			for (ServerConnection connection : serverConnections)
				if (connection.getNewBlockListener() == null)
					connection.setNewBlockListener(rain.getBlockListener());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
