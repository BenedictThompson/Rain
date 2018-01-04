package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.BalanceUpdater;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.EventOurBlockReceived;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.base.EventManager;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.RequestWithHeader;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerManager;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerNewBlock;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.listener.ListenerServerResponse;
import net.azurewebsites.thehen101.raiblockswallet.rain.transaction.ThreadTransactionPocketer;
import net.azurewebsites.thehen101.raiblockswallet.rain.transaction.Transaction;
import net.azurewebsites.thehen101.raiblockswallet.rain.transaction.TransactionChange;
import net.azurewebsites.thehen101.raiblockswallet.rain.transaction.TransactionSend;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.ThreadPriceUpdater;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.SettingsLoader;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.POWFinder;

public final class Rain {
	public static final String VERSION = "v1.0";
	private final ArrayList<byte[]> recentBlockHashes;
	private final ArrayList<Account> accounts;
	private final ServerManager serverManager;
	private final ListenerNewBlock newBlockListener;
	private final POWFinder powfinder;
	private final ThreadTransactionPocketer transactionPocketer;
	private final EventManager eventManager;
	private final String[] defaultReps;
	private final BalanceUpdater balanceUpdater;
	private final ThreadPriceUpdater threadPriceUpdater;
	
	private MessageDigest md;
	
	public Rain(ArrayList<ServerConnection> serverConnections, ArrayList<Account> accounts,
			String[] defaultReps) {
		this.eventManager = new EventManager();
		this.balanceUpdater = new BalanceUpdater(this);
		this.defaultReps = defaultReps;
		this.serverManager = new ServerManager(serverConnections);
		this.accounts = accounts;
		this.recentBlockHashes = new ArrayList<byte[]>();
		try {
			this.md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		this.powfinder = SettingsLoader.INSTANCE.getCachedPOW(this);
		this.powfinder.start();
		SettingsLoader.INSTANCE.cachePOW(this.powfinder);
		this.newBlockListener = new ListenerNewBlock() {
			@Override
			public void onNewBlock(RequestWithHeader newBlockNotification) {
				notifyNewBlock(new String(newBlockNotification.getRequestBytes()));
			}
		};
		this.transactionPocketer = new ThreadTransactionPocketer(this);
		this.transactionPocketer.start();
		
		this.threadPriceUpdater = new ThreadPriceUpdater(this, 30000);
		this.threadPriceUpdater.start();
		
		//set open status
		for (int i = 0; i < getAccounts().size(); i++) {
			Account a = getAccounts().get(i);
			for (int ii = 0; ii < a.getMaxAddressIndex(); ii++)
				if (a.isAddressAtIndex(ii)) {
					this.getPreviousHash(a.getAddressAtIndex(ii));
					try {
						//so we don't get rate limited
						Thread.sleep(51);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
		}
		
		System.out.println("Rain instance initialised");
	}
	
	public ThreadPriceUpdater getPriceUpdater() {
		return this.threadPriceUpdater;
	}
	
	public EventManager getEventManager() {
		return this.eventManager;
	}
	
	public void addAccount(Account account) {
		if (!this.accounts.contains(account)) {
			this.accounts.add(account);
			SettingsLoader.INSTANCE.saveAccounts(accounts);
		}
	}
	
	public String getDefaultRepresentative() {
		return this.defaultReps[new Random().nextInt(this.defaultReps.length)];
	}
	
	public BigInteger[] getAddressBalance(final Address address) {
		BigInteger[] balance = new BigInteger[2];
		Arrays.fill(balance, null);
		String body = 
				"{" + 
					"\"action\": \"account_balance\"," + 
					"\"account\": \"" + address.getAddress() + "\"" + 
				"}";
		RequestWithHeader request = new RequestWithHeader(false, body);
		ListenerServerResponse listener = new ListenerServerResponse() {
			@Override
			public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest) {
				if (!Arrays.equals(request.getRequestBytes(), initialRequest.getRequestBytes()))
					return;
				
				String json = new String(receivedRequest.getRequestBytes()).trim();
				
				JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
				JsonPrimitive bal = jsonObject.getAsJsonPrimitive("balance");
				JsonPrimitive pend = jsonObject.getAsJsonPrimitive("pending");
				
				balance[0] = new BigInteger(bal.getAsString());
				balance[1] = new BigInteger(pend.getAsString());
			}
		};
		this.serverManager.addListenerToAll(listener);
		this.serverManager.addToConnectedServerQueue(request);
		while (balance[0] == null) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.serverManager.removeListenerFromAll(listener);
		return balance;
	}
	
	public void sendXRBRaw(final Address sendFrom, String sendTo, BigInteger rawToSend) {
		BigInteger remaining = sendFrom.getRawTotalBalance().subtract(rawToSend);
		System.out.println("Sending: " + sendFrom.getAddress() + " -> " + sendTo);
		System.out.println("Sending raw: " + rawToSend.toString());
		System.out.println("Total raw: " + sendFrom.getRawTotalBalance());
		System.out.println("Remaining raw: " + remaining.toString());
		TransactionSend send = new TransactionSend(this.powfinder.getPowBlocking(sendFrom),
				this.getPreviousHash(sendFrom), sendFrom, sendTo, remaining);
		this.getServerManager().addToConnectedServerQueue(new RequestWithHeader(false,
				send.getAsJSON()));
		System.out.println(send.getAsJSON());
	}
	
	public void changeRepresentative(final Address address, String newRep) {
		address.setRepresentative(newRep);
		TransactionChange change = new TransactionChange(this.powfinder.getPowBlocking(address),
				address, this.getPreviousHash(address));
		this.serverManager.addToConnectedServerQueue(new RequestWithHeader(false, 
				change.getAsJSON()));
	}
	
	public String getPreviousHash(final Address address) {
		String[] previousHash = new String[1];
		previousHash[0] = null;
		String body = 
				"{" + 
					"\"action\": \"frontiers\"," + 
					"\"account\": \"" + address.getAddress() + "\"," + 
					"\"count\": \"1\"" + 
				"}";
		RequestWithHeader request = new RequestWithHeader(false, body);
		ListenerServerResponse listener = new ListenerServerResponse() {
			@Override
			public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest) {
				if (Arrays.equals(request.getRequestBytes(), initialRequest.getRequestBytes())) {
					//we got the request
					String returned = new String(receivedRequest.getRequestBytes());
					String a = returned.substring(returned.indexOf("xrb_"));
					String add = a.substring(0, a.indexOf("\""));
					if (!add.equals(address.getAddress())) {
						address.setIsOpened(false);
						System.out.println(address.getAddress() + " needs to be opened.");
						previousHash[0] = "";
					} else {
						String d = returned.substring(0, returned.lastIndexOf("\""));
						int index = d.lastIndexOf("\"") + 1;
						String hash = d.substring(index);
						previousHash[0] = hash;
					}
				}
			}
		};
		this.serverManager.addListenerToAll(listener);
		this.serverManager.addToConnectedServerQueue(request);
		while (previousHash[0] == null) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.serverManager.removeListenerFromAll(listener);
		return previousHash[0];
	}
	
	public String[] getUnpocketedForAddress(final Address address) {
		String[] unpocketed = new String[10];
		Arrays.fill(unpocketed, null);
		String body = 
				"{" + 
					"\"action\": \"pending\"," + 
					"\"account\": \"" + address.getAddress() + "\"," + 
					"\"count\": \"10\"" + 
				"}";
		RequestWithHeader request = new RequestWithHeader(false, body);
		ListenerServerResponse listener = new ListenerServerResponse() {
			@Override
			public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest) {
				if (!Arrays.equals(request.getRequestBytes(), initialRequest.getRequestBytes()))
					return;
				
				String json = new String(receivedRequest.getRequestBytes()).trim();

				if (json.length() != 20) {
					JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
					JsonArray jsonArray = jsonObject.getAsJsonArray("blocks");

					String[] arrName = new Gson().fromJson(jsonArray, String[].class);

					List<String> lstName = new ArrayList<>();
					lstName = Arrays.asList(arrName);

					for (int i = 0; i < lstName.size(); i++)
						unpocketed[i] = lstName.get(i);
				}
				if (unpocketed[0] == null)
					unpocketed[0] = "";
			}
		};
		this.serverManager.addListenerToAll(listener);
		this.serverManager.addToConnectedServerQueue(request);
		while (unpocketed[0] == null) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.serverManager.removeListenerFromAll(listener);
		return unpocketed;
	}
	
	public void notifyNewBlock(String newBlock) {
		newBlock = newBlock.replaceAll("\\s+", "").trim();
		byte[] digest = null;
		try {
			digest = md.digest(newBlock.getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 0; i < this.recentBlockHashes.size(); i++) {
			byte[] array = this.recentBlockHashes.get(i);
			if (Arrays.equals(digest, array))
				return;
		}
		
		this.recentBlockHashes.add(digest);
		if (this.recentBlockHashes.size() > 1000)
			this.recentBlockHashes.remove(0);
		
		JsonParser parser = new JsonParser();
		JsonObject command = parser.parse(newBlock).getAsJsonObject();
		String unformattedBlock = command.get("block").getAsString();
		String formattedBlock = unformattedBlock.replaceAll("\\\"", "\"").replaceAll("\\n", "");
		JsonObject block = parser.parse(formattedBlock).getAsJsonObject();
		String type = block.get("type").getAsString();
		//System.out.println("New block type: " + type + ", " + formattedBlock);
		Address address = null;
		
		switch (type) {
		case "receive":
		case "open":
		case "send":
			address = this.doesAddressStringBelongToUs(command.get("account").getAsString());
			if (address == null && type.equals("send"))
				address = this.doesAddressStringBelongToUs(block.get("destination").getAsString());
			break;
		default:
			break;
		}
		
		if (address != null) {
			BigInteger amountRaw = command.get("amount").getAsBigInteger();
			Transaction.Type transactionType = Transaction.Type.valueOf(type.toUpperCase());
			this.eventManager.callEvent(new EventOurBlockReceived(address, transactionType, amountRaw));
			System.out.println("EventOurBlockReceived event called: " + transactionType + ", " + address.getAddress());
		}
	}
	
	private Address doesAddressStringBelongToUs(String xrbAddress) {
		for (int i = 0; i < this.accounts.size(); i++) {
			Account a = this.accounts.get(i);
			for (int ii = 0; ii < a.getMaxAddressIndex(); ii++) {
				boolean addressAtIndex = a.isAddressAtIndex(ii);
				if (addressAtIndex) {
					Address address = a.getAddressAtIndex(ii);
					if (address.getAddress().equals(xrbAddress))
						return address;
				}
			}
		}
		return null;
	}
	
	public POWFinder getPOWFinder() {
		return this.powfinder;
	}
	
	public ServerManager getServerManager() {
		return this.serverManager;
	}
	
	public ArrayList<Account> getAccounts() {
		return this.accounts;
	}
	
	public ListenerNewBlock getBlockListener() {
		return this.newBlockListener;
	}
	
	public BalanceUpdater getBalanceUpdater() {
		return this.balanceUpdater;
	}
}
