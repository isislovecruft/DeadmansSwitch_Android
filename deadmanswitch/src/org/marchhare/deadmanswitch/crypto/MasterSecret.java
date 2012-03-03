package org.marchhare.deadmanswitch.crypto;

import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class MasterSecret implements Parcelable {
    
    private final SecretKeySpec macKey;

    public static final Parcelable.Creator<MasterSecret> CREATOR = new Parcelable.Creator<MasterSecret>() {
	public MasterSecret createFromParcel(Parcel in) {
	    return new MasterSecret(in);
	}

	public MasterSecret[] newArray(int size) {
	    return new MasterSecret[size];
	}
    };
    
    public MasterSecret(SecretKeySpec macKey) {
	this.macKey        = macKey;
    }

    private MasterSecret(Parcel in) {
	byte[] macKeyBytes = new byte[in.readInt()];
	in.readByteArray(macKeyBytes);
	
	this.macKey        = new SecretKeySpec(macKeyBytes, "HmacSHA1");
	
	// SecretKeySpec does and internal copy in its constructor.
	Arrays.fill(macKeyBytes, (byte)0x00);
    }

    public SecretKeySpec getMacKey() {
	return this.macKey;
    }
    
    public void writeToParcel(Parcel out, int flags) {
	out.writeInt(macKey.getEncoded().length);
	out.writeByteArray(macKey.getEncoded());
    }
    
    public int describeContents() {
	return 0;
    }
    
}