package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.security.KeyPair;
import java.security.SecureRandom;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.RequestWithHeader;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerConnection;
import net.azurewebsites.thehen101.raiblockswallet.rain.server.ServerResponseListener;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.DataManipulationUtil;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.spec.EdDSAGenParameterSpec;

public class Main {
	
	public static void main(String[] args) {
		try {
			SecureRandom random = SecureRandom.getInstanceStrong();
			KeyPairGenerator gen = new KeyPairGenerator();
			gen.initialize(new EdDSAGenParameterSpec("ED25519"), random);
			KeyPair keys = gen.generateKeyPair();
			Account xrb = new Account(keys);
			EdDSAPublicKey pk = (EdDSAPublicKey) keys.getPublic();
			System.out.println("public key: " + DataManipulationUtil.bytesToHex(pk.getAbyte()));
			System.out.println("xrbaddress: " + xrb.getAddress());
			
			ServerConnection c = new ServerConnection("192.168.1.4", 37076);
			c.start();
			
			ServerResponseListener listener = new ServerResponseListener() {
				@Override
				public void onResponse(RequestWithHeader initialRequest, RequestWithHeader receivedRequest) {
					System.out.println("We sent: " + new String(initialRequest.getRequestBytes()));
					System.out.println("We got: " + new String(receivedRequest.getRequestBytes()));
				}
			};
			
			c.addListener(listener);
			c.addToSendQueue(new RequestWithHeader(false, "{\"action\": \"available_supply\"}"));
			
			Thread.sleep(10000);
			
			c.addToSendQueue(new RequestWithHeader(false, "{\"action\": \"account_balance\"," + 
					"  \"account\": \"xrb_1owda95f9hc841qbb6dcm4egng3ohedw5xf1apjzykstx3zky7nsym5t6k4d\"" + 
					"}"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
