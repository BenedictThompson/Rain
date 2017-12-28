package net.azurewebsites.thehen101.raiblockswallet.rain;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.RequestWithHeader;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerNewBlock;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerServerResponse;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.POWFinder;

public class Main {
	public static void main(String[] args) {
		try {
			//test code
			Account a = new Account(
					DataManipulationUtil
					.hexStringToByteArray(
							"0000000000000000000000000000000000000000000000000000000000000000"));
			System.out.println(a.getAddressForIndex(0).getAddress());
			
			//generate pow for an open block for this address indefinitely
			for (int i = 0; i < 8; i++) {
				POWFinder pow = new POWFinder(a.getAddressForIndex(0));
				pow.start();
			}
			//System.exit(0);
			Thread.sleep(100000000);
			
			ListenerNewBlock newBlockListener = new ListenerNewBlock() {
				@Override
				public void onNewBlock(RequestWithHeader newBlockNotification) {
					System.out.println("New block: " + new String(newBlockNotification.getRequestBytes()));
				}
			};
			
			ServerConnection c = new ServerConnection("192.168.1.4", 37076, newBlockListener);
			c.start();
			
			ListenerServerResponse listener = new ListenerServerResponse() {
				@Override
				public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest) {
					System.out.println("We sent: " + new String(initialRequest.getRequestBytes()));
					System.out.println("We got: " + new String(receivedRequest.getRequestBytes()));
				}
			};
			
			c.addListener(listener);
			c.addToSendQueue(new RequestWithHeader(false, ""
					+ "{  " + 
					"  \"action\": \"accounts_frontiers\"," + 
					"  \"accounts\": [\"xrb_19ygfgw97htkabkc9ma7btnqun7ijjpc9trpna5ukb1ujtji76poyj8bc9au\"]" + 
					"}"));
			
			Thread.sleep(10000);
			
			c.addToSendQueue(new RequestWithHeader(false, "{\"action\": \"account_balance\"," + 
					"  \"account\": \"xrb_1owda95f9hc841qbb6dcm4egng3ohedw5xf1apjzykstx3zky7nsym5t6k4d\"" + 
					"}"));
			c.addToSendQueue(new RequestWithHeader(false, "{\"action\": \"account_balance\"," + 
					"  \"account\": \"xrb_1owda95f9hc841qbb6dcm4egng3ohedw5xf1apjzykstx3zky7nsym5t6k4d\"" + 
					"}"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
