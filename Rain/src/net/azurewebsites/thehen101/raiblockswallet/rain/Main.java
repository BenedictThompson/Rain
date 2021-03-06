package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.util.ArrayList;
import java.util.Random;

import com.alee.laf.WebLookAndFeel;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.gui.RainFramePassword;
import net.azurewebsites.thehen101.raiblockswallet.rain.gui.RainFrameSplash;
import net.azurewebsites.thehen101.raiblockswallet.rain.gui.RainFrameWallet;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.LoadedAccount;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.LoadedServer;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.SettingsLoader;

public class Main {
	public static void main(String[] args) {
		try {
			WebLookAndFeel.install();
			
			RainFrameSplash rfs = new RainFrameSplash();
			rfs.show();
			
			RainFramePassword rfp = new RainFramePassword();
			rfp.show();
			
			while (!rfp.passwordSet())
				Thread.sleep(10);
			
			//initialise client
			LoadedServer[] servers = SettingsLoader.INSTANCE.getDefaultServers();
			LoadedAccount[] accs = SettingsLoader.INSTANCE.getAccounts();
			String[] representatives = SettingsLoader.INSTANCE.getDefaultRepresentatives();
			
			ArrayList<ServerConnection> serverConnections = new ArrayList<ServerConnection>();
			ArrayList<Account> accounts = new ArrayList<Account>();
			
			for (LoadedServer ls : servers)
				serverConnections.add(new ServerConnection(ls.getHostnameOrIP(), ls.getPort(), null));
			
			
			for (LoadedAccount la : accs)
				accounts.add(new Account(la.getSeed(), 
						representatives[new Random().nextInt(representatives.length)], la.getGeneratedIndex()));
			
			Rain rain = new Rain(serverConnections, accounts, representatives);
			
			for (ServerConnection connection : serverConnections)
				if (connection.getNewBlockListener() == null)
					connection.setNewBlockListener(rain.getBlockListener());
			
			//initialisation completed
			
			RainFrameWallet rfw = new RainFrameWallet(rain);
			rfw.show();
			
			rfs.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
