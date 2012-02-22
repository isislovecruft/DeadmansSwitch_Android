package org.marchhare.deadmanswitch;

import org.marchhare.deadmanswitch.crypto.MasterSecret;
import org.marchhare.deadmanswitch.service.KeyCachingService;
import org.marchhare.deadmanswitch.util.MemoryCleaner;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public abstract class PassphraseActivity extends Activity {
    
    private KeyCachineService keyCachingService;
    private MasterSecret masterSecret;

    protected void setMasterSecret(MasterSecret masterSecret) {
	this.masterSecret = masterSecret;
	Intent bindIntent = new Intent(this, KeyCachineService.class);
	bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    protected abstract void cleanup();

    private ServiceConnection serviceConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
		keyCachingService.setMasterSecret(masterSecret);

		PassphraseActivity.this.unbindService(PassphraseActivity.this.serviceConnection);
		
		MemoryCleaner.clean(masterSecret);
		cleanup();
		PassphraseActivity.this.finish();
	    }
	    
	    public void onServiceDisconnected(ComponentName name) {
		keyCaching Service = null;
	    }
	};
}
