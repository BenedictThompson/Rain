package net.azurewebsites.thehen101.raiblockswallet.rain.util.file;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;

public enum SettingsLoader {
	INSTANCE;
	
	private final String newline = System.getProperty("line.separator");
	private final String 
		defaultRepresentatives = 
			"[" + newline +
			"  \"xrb_1hza3f7wiiqa7ig3jczyxj5yo86yegcmqk3criaz838j91sxcckpfhbhhra1\"," + newline +
			"  \"xrb_1awsn43we17c1oshdru4azeqjz9wii41dy8npubm4rg11so7dx3jtqgoeahy\"" + newline +
			"]" + newline,
			
		defaultServers = 
			"[" + newline +
			"  {" + newline +
			"    \"hostnameOrIP\": \"192.168.1.4\"," + newline +
			"    \"port\": 37076" + newline +
			"  }," + newline +
			"  {" + newline +
			"    \"hostnameOrIP\": \"192.168.1.5\"," + newline +
			"    \"port\": 37076" + newline +
			"  }" + newline +
			"]" + newline;
	
	private final byte[] salt = new byte[] { 
			(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
			(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
	
	private final byte[] iv = new byte[] { 
			(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
			(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
			(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
			(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
	
	private final File rainDirectory = new File(System.getProperty("user.home") + File.separator + "Rain"); { 
		rainDirectory.mkdirs();
	}
	
	private final File accountFile = new File(rainDirectory + File.separator + "accounts.json"),
			representativesFile = new File(rainDirectory + File.separator + "defaultRepresentatives.json"),
			serversFile = new File(rainDirectory + File.separator + "servers.json");
	
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	private String password;
	
	public void setPassword(String pass) {
		this.fixKeyLength();
		this.password = pass;
	}
	
	public LoadedAccount[] getAccounts() {
		if (!this.accountFile.exists()) {
			try {
				this.accountFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			byte[] seed = this.getSecureSeed();
			int max = 0;
			LoadedAccount[] la = new LoadedAccount[] { new LoadedAccount(seed, max) };
			String js = gson.toJson(la);
			try {
				FileUtil.bytesToFile(this.accountFile, this.encryptBytes(js.getBytes(), this.password));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Could not save accounts. FATAL! Exiting...");
				System.exit(-1);
			}
		}
		byte[] enc = FileUtil.fileToBytes(this.accountFile);
		try {
			return this.gson.fromJson(
					new String(this.decryptBytes(enc, this.password)),
					LoadedAccount[].class);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to decrypt accounts... exiting...");
			System.exit(-1);
		}
		return null;
	}
	
	public void saveAccounts(ArrayList<Account> accounts) {
		LoadedAccount[] toSave = new LoadedAccount[accounts.size()];
		for (int i = 0; i < toSave.length; i++) {
			Account a = accounts.get(i);
			toSave[i] = new LoadedAccount(a.getSeed(), a.getAddressesCount());
		}
		String json = gson.toJson(toSave);
		if (!this.accountFile.exists()) {
			try {
				this.accountFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			FileUtil.bytesToFile(this.accountFile, 
					this.encryptBytes(json.getBytes(), this.password));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not save accounts. FATAL! Exiting...");
			System.exit(-1);
		}
	}
	
	public String[] getDefaultRepresentatives() {
		if (!this.representativesFile.exists()) {
			try {
				this.representativesFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			FileUtil.stringToFile(this.representativesFile, defaultRepresentatives);
		}
		String json = FileUtil.fileToString(this.representativesFile);
		return this.gson.fromJson(json, String[].class);
	}
	
	public LoadedServer[] getDefaultServers() {
		if (!this.serversFile.exists()) {
			try {
				this.serversFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			FileUtil.stringToFile(this.serversFile, defaultServers);
		}
		String json = FileUtil.fileToString(this.serversFile);
		return this.gson.fromJson(json, LoadedServer[].class);
	}
	
	public byte[] getSecureSeed() {
		try {
			SecureRandom sr = SecureRandom.getInstanceStrong();
			byte[] seed = new byte[32];
			sr.nextBytes(seed);
			return seed;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		Random r = new Random();
		byte[] seed = new byte[32];
		r.nextBytes(seed);
		return seed;
	}

	private Cipher makeCipher(String pass, Boolean decryptMode) throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(pass.toCharArray(), salt, 65536, 256);
		SecretKey tmp = factory.generateSecret(spec);
		SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
	    IvParameterSpec ivspec = new IvParameterSpec(iv);
		if (decryptMode)
			cipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
		else
			cipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
		return cipher;
	}

	public byte[] encryptBytes(byte[] plaintext, String pass) throws Exception {
		Cipher cipher = makeCipher(pass, true);
		return cipher.doFinal(plaintext);
	}

	private byte[] decryptBytes(byte[] encrypted, String pass) throws Exception {
		Cipher cipher = makeCipher(pass, false);
		return cipher.doFinal(encrypted);
	}
	
	//thanks stackoverflow!! code only solution, we don't have to mess with files
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void fixKeyLength() {
	    String errorString = "Failed manually overriding key-length permissions.";
	    int newMaxKeyLength;
	    try {
	        if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
	            Class<?> c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
	            Constructor<?> con = c.getDeclaredConstructor();
	            con.setAccessible(true);
	            Object allPermissionCollection = con.newInstance();
	            Field f = c.getDeclaredField("all_allowed");
	            f.setAccessible(true);
	            f.setBoolean(allPermissionCollection, true);

	            c = Class.forName("javax.crypto.CryptoPermissions");
	            con = c.getDeclaredConstructor();
	            con.setAccessible(true);
	            Object allPermissions = con.newInstance();
	            f = c.getDeclaredField("perms");
	            f.setAccessible(true);
	            ((Map) f.get(allPermissions)).put("*", allPermissionCollection);

	            c = Class.forName("javax.crypto.JceSecurityManager");
	            f = c.getDeclaredField("defaultPolicy");
	            f.setAccessible(true);
	            Field mf = Field.class.getDeclaredField("modifiers");
	            mf.setAccessible(true);
	            mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
	            f.set(null, allPermissions);

	            newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
	        }
	    } catch (Exception e) {
	        throw new RuntimeException(errorString, e);
	    }
	    if (newMaxKeyLength < 256)
	        throw new RuntimeException(errorString); // hack failed
	}
}
