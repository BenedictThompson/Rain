package net.azurewebsites.thehen101.raiblockswallet.rain.util.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.azurewebsites.thehen101.raiblockswallet.rain.Rain;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Account;
import net.azurewebsites.thehen101.raiblockswallet.rain.account.Address;
import net.azurewebsites.thehen101.raiblockswallet.rain.util.hash.POWFinder;

public enum SettingsLoader {
	INSTANCE;
	
	private final String newline = System.getProperty("line.separator");
	private final String 
		defaultRepresentatives = 
			"[" + newline +
			"  \"xrb_15dk1r8wgw57gpebspnadfayiuwp9wy49qnrzp7xf7jof9u6rfgeju4x85du\"," + newline +
			"  \"xrb_19pnoe8nttqffs78mpr98kowzf6s3ug5czmmkj5e4ogbydcoxkjcbctp8hwn\"" + newline +
			"]" + newline,
			
		defaultServers = 
			"[" + newline +
			"  {" + newline +
			"    \"hostnameOrIP\": \"virginia.thehen101.co.uk\"," + newline +
			"    \"port\": 37076" + newline +
			"  }," + newline +
			"  {" + newline +
			"    \"hostnameOrIP\": \"london.thehen101.co.uk\"," + newline +
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
	
	private final File rainDirectory = new File(System.getProperty("user.home") + File.separator + "Rain");
	private final File logDirectory = new File(rainDirectory + File.separator + "logs"); { 
		rainDirectory.mkdirs();
		logDirectory.mkdirs();
		logFile = new File(logDirectory + File.separator + "rainLogAtEpoch" + Instant.now().getEpochSecond() + ".log");
		this.setPrintStream();
	}
	
	private final File 
			accountFile = new File(rainDirectory + File.separator + "accounts.json"),
			representativesFile = new File(rainDirectory + File.separator + "defaultRepresentatives.json"),
			serversFile = new File(rainDirectory + File.separator + "servers.json"),
			powFile = new File(rainDirectory + File.separator + "cachedPOW.json"),
			logFile;
	
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
			ArrayList<Boolean> genned = new ArrayList<Boolean>();
			genned.add(0, true);
			LoadedAccount[] la = new LoadedAccount[] { new LoadedAccount(seed, genned) };
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
			JOptionPane.showMessageDialog(null, "Could not decrypt accounts. Wrong password?",
					"Rain: Fatal error", JOptionPane.ERROR_MESSAGE);
			System.out.println("Failed to decrypt accounts... exiting...");
			System.exit(-1);
		}
		return null;
	}
	
	public void saveAccounts(ArrayList<Account> accounts) {
		LoadedAccount[] toSave = new LoadedAccount[accounts.size()];
		for (int i = 0; i < toSave.length; i++) {
			Account a = accounts.get(i);
			toSave[i] = new LoadedAccount(a.getSeed(), a.getShouldGenerateAddressIndex());
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
	
	public void cachePOW(POWFinder powFinder) {
		HashMap<String, String> a = new HashMap<String, String>();
		HashMap<String, Boolean> b = new HashMap<String, Boolean>();
		ArrayList<String> c = new ArrayList<String>();
		
	    Iterator<Entry<Address, String>> it0 = powFinder.getPOWMap().entrySet().iterator();
	    while (it0.hasNext()) {
	        Map.Entry<Address, String> pair = (Map.Entry<Address, String>) it0.next();
	        a.put(pair.getKey().getAddress(), pair.getValue());
	    }
	    
	    Iterator<Entry<Address, Boolean>> it1 = powFinder.getOpenPOWMap().entrySet().iterator();
	    while (it1.hasNext()) {
	        Map.Entry<Address, Boolean> pair = (Map.Entry<Address, Boolean>) it1.next();
	        b.put(pair.getKey().getAddress(), pair.getValue());
	    }
	    
	    for (int i = 0; i < powFinder.getGeneratePowAddresses().size(); i++) {
	    	Address oculus = powFinder.getGeneratePowAddresses().get(i);
	    	c.add(i, oculus.getAddress());
	    }
	    
		LoadedPOWFinder lpf = new LoadedPOWFinder(powFinder.getThreadCount(), a, b, c);
		String json = gson.toJson(lpf);
		if (!this.powFile.exists()) {
			try {
				this.powFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			FileUtil.bytesToFile(this.powFile, json.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not save POW. FATAL! Exiting...");
			System.exit(-1);
		}
	}
	
	public POWFinder getCachedPOW(Rain rain) {
		boolean powFileExists = this.powFile.exists();
		if (powFileExists) {
			LoadedPOWFinder lpf = new Gson().fromJson(
					FileUtil.fileToString(this.powFile), LoadedPOWFinder.class);
			
			HashMap<Address, String> a = new HashMap<Address, String>();
			HashMap<Address, Boolean> b = new HashMap<Address, Boolean>();
			ArrayList<Address> c = new ArrayList<Address>();
			
		    Iterator<Entry<String, String>> it0 = lpf.getPowMap().entrySet().iterator();
		    while (it0.hasNext()) {
		        Map.Entry<String, String> pair = (Map.Entry<String, String>) it0.next();
		        a.put(addressForAddressString(rain, pair.getKey()), pair.getValue());
		    }
		    
		    Iterator<Entry<String, Boolean>> it1 = lpf.getOpenPowMap().entrySet().iterator();
		    while (it1.hasNext()) {
		        Map.Entry<String, Boolean> pair = (Map.Entry<String, Boolean>) it1.next();
		        b.put(addressForAddressString(rain, pair.getKey()), pair.getValue());
		    }
		    
		    for (int i = 0; i < lpf.getGeneratePowAddresses().size(); i++) {
		    	String h = lpf.getGeneratePowAddresses().get(i);
		    	c.add(i, addressForAddressString(rain, h));
		    }
			
			return new POWFinder(rain, lpf.getThreadsToUse(), a, b, c, false);
		} else {
			int powThreads = Runtime.getRuntime().availableProcessors() - 1;
			if (powThreads == 0)
				powThreads = 1;
			return new POWFinder(rain, powThreads);
		}
	}
	
	private final Address addressForAddressString(Rain rain, String add) {
		for (int i = 0; i < rain.getAccounts().size(); i++) {
			Account a = rain.getAccounts().get(i);
			for (int ii = 0; ii < a.getMaxAddressIndex(); ii++) {
				boolean valid = a.isAddressAtIndex(ii);
				if (valid) {
					Address c = a.getAddressAtIndex(ii);
					if (c.getAddress().equals(add))
						return c;
				}
			}
		}
		return null;
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
	
	public File getRainDirectory() {
		return this.rainDirectory;
	}
	
	private void setPrintStream() {
		try {
			if (this.logFile.exists())
				this.logFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(this.logFile);
			TeeOutputStream myOut = new TeeOutputStream(System.out, fos);
			PrintStream ps = new PrintStream(myOut, true);
			System.setOut(ps);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
