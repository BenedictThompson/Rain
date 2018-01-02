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
import com.google.gson.JsonPrimitive;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.ThreadBalanceUpdater;
import net.azurewebsites.thehen101.raiblockswallet.rain.event.EventBalanceUpdate;
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
import net.azurewebsites.thehen101.raiblockswallet.rain.util.file.SettingsLoader;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.POWFinder;

public final class Rain {
	private final ArrayList<byte[]> recentBlockHashes;
	private final ArrayList<Account> accounts;
	private final ServerManager serverManager;
	private final ListenerNewBlock newBlockListener;
	private final POWFinder powfinder;
	private final ThreadTransactionPocketer transactionPocketer;
	private final ThreadBalanceUpdater balanceUpdater;
	private final EventManager eventManager;
	private final String[] defaultReps;
	
	private MessageDigest md;
	
	public Rain(ArrayList<ServerConnection> serverConnections, 
			int powThreadCount, String[] defaultReps) {
		this.eventManager = new EventManager();
		this.defaultReps = defaultReps;
		this.serverManager = new ServerManager(serverConnections);
		this.accounts = new ArrayList<Account>();
		this.balanceUpdater = new ThreadBalanceUpdater(this);
		this.balanceUpdater.start();
		while (!this.balanceUpdater.hasInitiallyCheckedAccounts()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		this.recentBlockHashes = new ArrayList<byte[]>();
		try {
			this.md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		for (Account account : this.accounts) 
			this.initAccount(account);
		this.powfinder = new POWFinder(this, powThreadCount);
		this.powfinder.start();
		this.newBlockListener = new ListenerNewBlock() {
			@Override
			public void onNewBlock(RequestWithHeader newBlockNotification) {
				notifyNewBlock(new String(newBlockNotification.getRequestBytes()));
			}
		};
		this.transactionPocketer = new ThreadTransactionPocketer(this);
		this.transactionPocketer.start();
		System.out.println("Rain instance initialised");
	}
	
	public EventManager getEventManager() {
		return this.eventManager;
	}
	
	private void initAccount(Account account) {
		if (account.getAddressesCount() == 0) {
			account.getAddressForIndex(0);
		}
	}
	
	public void addAccount(Account account) {
		account.getAddressForIndex(0);
		this.accounts.add(account);
		SettingsLoader.INSTANCE.saveAccounts(accounts);
	}
	
	public String getDefaultRepresentative() {
		return this.defaultReps[new Random().nextInt(this.defaultReps.length)];
	}
	
	public String getPrice() {
		String[] price = new String[1];
		price[0] = null;
		String body = "{ \"command\": \"price\" }";
		RequestWithHeader request = new RequestWithHeader(false, body);
		ListenerServerResponse listener = new ListenerServerResponse() {
			@Override
			public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest) {
				if (!Arrays.equals(request.getRequestBytes(), initialRequest.getRequestBytes()))
					return;
				
				String json = new String(receivedRequest.getRequestBytes()).trim();
				
				JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
				JsonPrimitive p = jsonObject.getAsJsonPrimitive("price");
				
				price[0] = p.getAsString();
			}
		};
		this.serverManager.addListenerToAll(listener);
		this.serverManager.addToConnectedServerQueue(request);
		while (price[0] == null) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.serverManager.removeListenerFromAll(listener);
		return price[0];
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
		this.balanceUpdater.updateBalance(sendFrom);
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
		
		int z = newBlock.indexOf("\\\"type\\\"") + 8;
		String x = newBlock.substring(z);
		int y = x.indexOf("\"") + 1;
		String w = x.substring(y);
		int v = w.indexOf("\\");
		String type = w.substring(0, v);		
		int a = newBlock.indexOf("\\\"destination\\\"") + 15;
		String b = newBlock.substring(a);
		int c = b.indexOf("\"") + 1;
		String d = b.substring(c);
		int e = d.indexOf("\\");
		String destination = d.substring(0, e);
		int aa = newBlock.indexOf("\"hash\"") + 6;
		String ba = newBlock.substring(aa);
		int ca = ba.indexOf("\"") + 1;
		String da = ba.substring(ca);
		int ea = da.indexOf("\"");
		String hash = da.substring(0, ea);
		int a8 = newBlock.indexOf("\"account\"") + 9;
		String b8 = newBlock.substring(a8);
		int c8 = b8.indexOf("\"") + 1;
		String d8 = b.substring(c8);
		int e8 = d8.indexOf("\\");
		String acco = d8.substring(0, e8);
		
		Address add = this.doesAddressStringBelongToUs(destination);
		Address add1 = this.doesAddressStringBelongToUs(acco);
		
		
		if (add != null || add1 != null) {
			System.out.println("Block related to us!");
			Transaction.Type ttype = Transaction.Type.valueOf(type.toUpperCase());
			
			switch (ttype) {
			case OPEN:
			case RECEIVE:
			case SEND:
				Address ax = add != null ? add : add1;
				
				int aa0 = newBlock.indexOf("\"amount\"") + 8;
				String ba0 = newBlock.substring(aa0);
				int ca0 = ba0.indexOf("\"") + 1;
				String da0 = ba0.substring(ca0);
				int ea0 = da0.indexOf("\"");
				String amount = da0.substring(0, ea0);
				
				//update balance
				BigInteger[] balpend = getAddressBalance(ax);
				BigInteger bal = balpend[0];
				BigInteger pend = balpend[1];
				ax.setBalance(bal);
				ax.setPending(pend);
				ax.setTotalBalance(bal.add(pend));
				
				this.eventManager.callEvent(new EventBalanceUpdate(ax));
				
				this.eventManager.callEvent(new EventOurBlockReceived(ax, ttype,
						new BigInteger(amount), destination));
				
				System.out.println("Both events called!");
				break;
			default:
				break;
			}
		
			if (add == null)
				return;
			
			if (ttype != Transaction.Type.SEND)
				return;
		
			this.recentBlockHashes.add(digest);
			if (this.recentBlockHashes.size() > 10000)
				this.recentBlockHashes.remove(0);
		
			add.getUnpocketedTransactions().add(hash);
		}
	}
	
	private Address doesAddressStringBelongToUs(String xrbAddress) {
		for (Account account : this.accounts) {
			int max = account.getAddressesCount();
			for (int i = 0; i < max; i++) {
				Address address = account.getAddressForIndex(i);
				if (address.getAddress().equals(xrbAddress))
					return address;
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
	
	public ThreadBalanceUpdater getBalanceUpdater() {
		return this.balanceUpdater;
	}
}
