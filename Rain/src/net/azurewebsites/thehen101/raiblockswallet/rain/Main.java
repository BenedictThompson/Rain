package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.io.File;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.FileUtil;

public class Main {
	public static void main(String[] args) {
		try {
			String defaultRepresentatives = FileUtil.fileToString(new File("defaultRepresentatives.json"));
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String[] reps = gson.fromJson(defaultRepresentatives, String[].class);
			
			System.out.println(reps.length);
			System.out.println(reps[0]);
			System.out.println(reps[1]);
			
			
			System.exit(0);
			ArrayList<ServerConnection> connections = new ArrayList<ServerConnection>();
			connections.add(new ServerConnection("192.168.1.4", 37076, null));
			
			ArrayList<Account> accounts = new ArrayList<Account>();
			accounts.add(new Account(
					DataManipulationUtil
					.hexStringToByteArray(
							"Snip"), reps[0]));
			Rain rain = new Rain(connections, accounts, 8, reps);
			
			for (ServerConnection connection : connections) {
				if (connection.getNewBlockListener() == null) {
					connection.setNewBlockListener(rain.getBlockListener());
				}
			}
			
			Thread.sleep(500);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
