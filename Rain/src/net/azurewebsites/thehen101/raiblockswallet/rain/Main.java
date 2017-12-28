package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.math.BigInteger;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.RequestWithHeader;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerNewBlock;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerServerResponse;
import net.azurewebsites.thehen101.raiblockswallet.rain.transaction.TransactionChange;
import net.azurewebsites.thehen101.raiblockswallet.rain.transaction.TransactionOpen;
import net.azurewebsites.thehen101.raiblockswallet.rain.transaction.TransactionReceive;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DenominationConverter;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.POWFinder;

public class Main {
	public static void main(String[] args) {
		try {
			//test code
			Account a = new Account(
					DataManipulationUtil
					.hexStringToByteArray(
							"SNIP"));
			System.out.println(DataManipulationUtil.bytesToHex(a.getAddressForIndex(0).getPublicKey()));
			System.out.println(DataManipulationUtil.bytesToHex(a.addressToPublicKey(a.getAddressForIndex(0).getAddress())));
			System.out.println(a.getAddressForIndex(0).getAddress());
			System.out.println(DenominationConverter.convertToRaw(new BigInteger("1"), DenominationConverter.MRAI));
			//generate pow for an open block for this address indefinitely
			
			POWFinder pow = new POWFinder(8);
			pow.start();
			//System.out.println(pow.canGiveWork());
			//System.out.println(pow.canGiveWork() + " " + pow.getWork());
			//while (!pow.canGiveWork())
			//	Thread.sleep(100);
			//System.out.println(pow.canGiveWork() + " " + pow.getWork());
			pow.giveWork(DataManipulationUtil.hexStringToByteArray("5738E61E5D43BFD2F82271846E27482FA23BD616CECCDBF1A08B3B0B843D10FD"));
			while (!pow.canGiveWork())
				Thread.sleep(100);
			//System.out.println(pow.canGiveWork() + " " + pow.getWork());
			//Thread.sleep(100000000);
			
			a.getAddressForIndex(0).setRepresentative("xrb_1anrzcuwe64rwxzcco8dkhpyxpi8kd7zsjc1oeimpc3ppca4mrjtwnqposrs");
			
			TransactionChange change = new TransactionChange(pow.getWork(), a.getAddressForIndex(0),
					"5738E61E5D43BFD2F82271846E27482FA23BD616CECCDBF1A08B3B0B843D10FD");
			
			//TransactionReceive receive = new TransactionReceive(pow.getWork(), a.getAddressForIndex(0),
			//		"5A983D9D8EC8A8C32AB1620E57F0617ADE2891084CFAF8015845303AD3254F62",
			//		"2A49369DD0F1609FB3C91AB2E63883F6FA7E4FADAAD06F4C41839F2FD11F9346");
			
			//TransactionSend send = new TransactionSend(pow.getWork(),
			//		"SNIP", a.getAddressForIndex(0),
			//		"xrb_1owda95f9hc841qbb6dcm4egng3ohedw5xf1apjzykstx3zky7nsym5t6k4d",
			//		DenominationConverter.convertToRaw(new BigInteger("1"), DenominationConverter.MRAI));
			
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
			c.addToSendQueue(new RequestWithHeader(false, change.getAsJSON()));
			Thread.sleep(100000000);
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
			TransactionOpen open = new TransactionOpen(pow.getWork(), "SNIP",
					a.getAddressForIndex(0));
			c.addToSendQueue(new RequestWithHeader(false, open.getAsJSON()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
