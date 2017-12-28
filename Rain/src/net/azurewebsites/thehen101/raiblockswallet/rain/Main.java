package net.azurewebsites.thehen101.raiblockswallet.rain;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.RequestWithHeader;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerNewBlock;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerServerResponse;
import net.azurewebsites.thehen101.raiblockswallet.rain.transaction.TransactionOpen;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.POWFinder;

public class Main {
	public static void main(String[] args) {
		try {
			//test code
			Account a = new Account(
					DataManipulationUtil
					.hexStringToByteArray(
							"SNIPSNIP"));
			System.out.println(DataManipulationUtil.bytesToHex(a.getAddressForIndex(0).getPublicKey()));
			System.out.println(DataManipulationUtil.bytesToHex(a.addressToPublicKey(a.getAddressForIndex(0).getAddress())));
			System.out.println(a.getAddressForIndex(0).getAddress());
			
			//generate pow for an open block for this address indefinitely
			
			POWFinder pow = new POWFinder(a.getAddressForIndex(0), 8);
			pow.start();
				
			//System.exit(0);
			//Thread.sleep(100000000);
			
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
					"  \"accounts\": [\"xrb_3jzukt4ekdyrym9s3htcokoe7xdfa1tqckxr19gnqghqf6t7ud1pkwk7x9jq\"]" + 
					"}"));
			
			Thread.sleep(1000);
			
			c.addToSendQueue(new RequestWithHeader(false, "{\"action\": \"account_balance\"," + 
					"  \"account\": \"xrb_3jzukt4ekdyrym9s3htcokoe7xdfa1tqckxr19gnqghqf6t7ud1pkwk7x9jq\"" + 
					"}"));
			
			c.addToSendQueue(new RequestWithHeader(false, "{  " + 
					"  \"action\": \"pending\"," + 
					"  \"account\": \"xrb_3jzukt4ekdyrym9s3htcokoe7xdfa1tqckxr19gnqghqf6t7ud1pkwk7x9jq\", " + 
					"  \"count\": \"100\"  " + 
					"}"));
			
			Thread.sleep(15000);
			System.out.println("Sending open block...");
			Thread.sleep(4000);
			TransactionOpen open = new TransactionOpen(pow.openWork, "2026CDF1B97A8DE397666FE98064959976CB1E19E3D6B619ADF93ECCD80D004A",
					a.getAddressForIndex(0));
			c.addToSendQueue(new RequestWithHeader(false, open.getAsJSON()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
