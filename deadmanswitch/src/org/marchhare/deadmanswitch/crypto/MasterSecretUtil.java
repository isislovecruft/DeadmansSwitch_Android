package org.marchhare.deadmanswitch.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.ECPrivateKeyParameters;
import org.marchhare.deadmanswitch.util.Base64;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class MasterSecretUtil {
    
    public static final String PREFERENCES_NAME    = "Deadman Switch Preferences";
    public static final String MACKEY_LOCAL_PUBLIC = "mac_master_secret";
    
    public static MasterSecret changeMasterSecretPassphrase(Context context, 
							    String originalPassphrase, 
							    String newPassphrase) 
	throws InvalidPassphraseException {
	try {
	    MasterSecret masterSecret = getMasterSecret(context, originalPassphrase);
	    byte[] masterSecretByteArray = masterSecret.getMacKey().getEncoded();
	    encryptWithPassphraseAndSave(context, masterSecretByteArray, newPassphrase);
		
	    return masterSecret;
	} catch (GeneralSecurtyException gse) {
	    throw new AssertionError(gse);
	}
    }
    
    public static MasterSecret getMasterSecret(Context context, String passphrase) 
	throws InvalidPassphraseException {
	try {
	    byte[] passphrasedMasterSecret = retrieve(context, "master_secret");
	    byte[] unpassphrasedSecret     = decryptWithPassphrase(context, 
								   passphrasedMasterSecret, 
								   passphrase);
	    byte[] macSecret               = getMacSecret(unpassphrasedSecret);
	    
	    return new MasterSecret(new SecretKeySpec(macSecret, "HmacSHA1"));
	} catch (GeneralSecurityException e) {
	    Log.w("MasterSecretUtil", e);
	    return null;
	} catch (IOException e) {
	    Log.w("MasterSecretUtil", e);
	    return null;
	}
    }
    
    public static MasterSecret generateMasterSecret(Context context, String passphrase) {
	try {
	    byte[] masterSecret = generateMacSecret();
	    
	    encryptWithPassphraseAndSave(context, masterSecret, passphrase);
	    
	    return new MasterSecret(new SecretKeySpec(macSecret, "HmacSHA1"));
	} catch (GeneralSecurityException e) {
	    Log.w("MasterSecretUtil", e);
	    return null;
	}
    }
    
    public static boolean hasMacKey(Context context) {
	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
	return settings.contains(MACKEY_LOCAL_PUBLIC);
    }
    
    public static void encryptWithPassphraseAndSave(Context context, byte[] masterSecret, 
						    String passphrase) 
	throws GeneralSecurityException {
	byte[] encryptedMasterSecret    = encryptWithPassphrase(context, masterSecret, 
								passphrase);
	byte[] passphrasedMasterSecret  = macWithPassphrase(context, encryptedMasterSecret, 
							    passphrase);
	
	save(context, "master_secret", passphrasedMasterSecret);
	save(context, "passphrase_initialized", true);
    }
    
    private static byte[] getMacSecret(byte[] unpassphrasedSecret) {
	byte[] macSecret = new byte[20];
	System.arraycopy(unpassphrasedSecret, 0, macSecret, 0, macSecret.length);
	return macSecret;
    }
    
    private static void save(Context context, String key, byte[] value) {
	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
	Editor editor              = settings.edit();
	
	editor.putString(key, Base64.encodeBytes(value));
	editor.commit();
    }
    
    private static void save(Context context, String key, boolean value) {
	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
	Editor editor              = settings.edit();
	
	editor.putBoolean(key, value);
	editor.commit();
    }
    
    private static byte[] retrieve(Context context, String key) throws IOException {
	SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
	String encodedValue        = settings.getString(key, "");
	
	if (encodedValue == "") return null;
	else                    return Base64.decode(encodedValue);
    }
    
    private static byte[] generateMacSecret() {
	try {
	    KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
	    return generator.generateKey().getEncoded();
	} catch (NoSuchAlgorithmException e) {
	    Log.w("MasterSecretUtil", e);
	    return null;
	}
    }
    
    private static byte[] generateSalt() throws NoSuchAlgorithmException {
	SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
	byte[] salt         = new byte[8];
	random.nextBytes(salt);
	
	return salt;
    }
    
    private static SecretKey getKeyFromPassphrase(String passphrase, byte[] salt) 
	throws GeneralSecurityException {
	PBEKeySpec keyspec   = new PBEKeySpec(passphrase.toCharArray(), salt, 100);
	SecretKeyFactory skf = SecretKeyFactory.getInstance("PBEWITHSHA1AND128BITAES-CBC-BC");
	return skf.generateSecret(keyspec);
    }
    
    private static Cipher getCipherFromPassphrase(String passphrase, byte[] salt, 
						  int opMode) 
	throws GeneralSecurityException {
	SecretKey key          = getKeyFromPassphrase(passphrase, salt);
	Cipher cipher          = Cipher.getInstance(key.getAlgorithm());
	cipher.init(opMode, key, new PBEParameterSpec(salt, 100));
	
	return cipher;
    }
    
    private static byte[] encryptWithPassphrase(Context context, byte[] data, 
						String passphrase) 
	throws NoSuchAlgorithmException, GeneralSecurityException {
	byte[] encryptionSalt = generateSalt();
	Cipher cipher         = getCipherFromPassphrase(passphrase, encryptionSalt, 
							Cipher.ENCRYPT_MODE);
	byte[] cipherText     = cipher.doFinal(data);
	
	save(context, "encryption_salt", encryptionSalt);
	return cipherText;
    }
    
    private static byte[] decryptWithPassphrase(Context context, byte[] data, 
						String passphrase) 
	throws GeneralSecurityException, IOException {
	byte[] encryptionSalt = retrieve(context, "encryption_salt");
	Cipher cipher         = getCipherFromPassphrase(passphrase, encryptionSalt, 
							Cipher.DECRYPT_MODE);
	return cipher.doFinal(data);
    }
    
    private static Mac getMacForPassphrase(String passphrase, byte[] salt) 
	throws GeneralSecurityException {
	SecretKey key         = getKeyFromPassphrase(passphrase, salt);
	byte[] pbkdf2         = key.getEncoded();
	SecretKeySpec hmacKey = new SecretKeySpec(pbkdf2, "HmacSHA1");
	Mac hmac              = Mac.getInstance("HmacSHA1");
	hmac.init(hmacKey);
	
	return hmac;
    }
    
    private static byte[] verifyMac(Context context, byte[] passphrasedAndMacdData, 
				    String passphrase) 
	throws InvalidPassphraseException, GeneralSecurityException, IOException {
	byte[] macSalt   = retrieve(context, "mac_salt");
	Mac hmac         = getMacForPassphrase(passphrase, macSalt);
	
	byte[] encryptedData = new byte[passphrasedAndMacdData.length - hmac.getMacLength()];
	System.arraycopy(passphrasedAndMacdData, 0, encryptedData, 0, encryptedData.length);
	
	byte[] givenMac      = new byte[hmac.getMacLength()];
	System.arraycopy(passphrasedAndMacdData, 
			 passphrasedAndMacdData.length-hmac.getMacLength(), 
			 givenMac, 0, givenMac.length);
	
	byte[] localMac      = hmac.doFinal(encryptedData);
	
	if (Arrays.equals(givenMac, localMac)) {
	    return encryptedData;
	} else {
	    throw new InvalidPassphraseException("MAC Error");
	}
    }
    
    private static byte[] macWithPassphrase(Context context, byte[] data, 
					    String passphrase)
	throws GeneralSecurityException {
	byte[] macSalt = generateSalt();
	Mac hmac       = getMacForPassphrase(passphrase, macSalt);
	byte[] mac     = hmac.doFinal(data);
	byte[] result  = new byte[data.length + mac.length];
	
	System.arraycopy(data, 0, result, 0, data.length);
	System.arraycopy(mac, 0, result, data.length, mac.length);
	
	save(context, "mac_salt", macSalt);
	return result;
    }
}