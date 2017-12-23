package net.azurewebsites.thehen101.raiblockswallet.rain;

import java.security.KeyPair;
import java.security.SecureRandom;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
