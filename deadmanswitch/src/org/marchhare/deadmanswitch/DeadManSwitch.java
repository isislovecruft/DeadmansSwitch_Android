package org.marchhare.deadmanswitch;

import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.service.KeyCachingService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.Byte;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.lang.StringBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.Toolkit;

public class DeadManSwitch extends Activity {

    private static final int MENU_SETUP_KEY      = 1;
    private static final int MENU_OPTIONS_KEY    = 2;
    private static final int MENU_PASSPHRASE_KEY = 3;
    private static final int MENU_TASKS_KEY      = 4;
    private static final int MENU_INCOGNITO_KEY  = 5;
    private static final int MENU_PANIC_KEY      = 6;
    private static final int MENU_EXIT_KEY       = 7;
    private static final int MENU_NONCOGNITO_KEY = 8;

    private BroadcastReceiver receiver;
    private MasterSecret masterSecret;
    private KillActivityReceiver killActivityReceiver;
    private boolean incognitoMode             = false;
    private boolean havePromptedForPassphrase = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

	/** Passphrase prompt on main screen. */
	TextView enterPasswd = (TextView)findViewById(R.id.enter_passwd);
	
	/** Passphrase box. */
	final EditText alivePasswd = (EditText)findViewById(R.id.alive_passwd);

	/** Button to enter passphrase. */
	final Button aliveButton = (Button)findViewById(R.id.alive_button);
	aliveButton.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    /** Check that they are pressing the intended button.*/
		    if (v == aliveButton) {
			/** Grab passphrase from user input.*/
			String plaintext = alivePasswd.getText().toString();
			//NEED WAY TO CATCH BLANK PASSWORD
			
			/** Convert to byte array, needed for hashing.*/
			byte[] plaintextbytes = plaintext.getBytes();
			String sha256hash;
			try {
			    /** Use SHA-256 */
			    MessageDigest md = MessageDigest.getInstance("SHA-256");
			    md.reset();
			    md.update(plaintextbytes);
			    byte hash[] = md.digest();

			    StringBuffer hexstring = new StringBuffer();
			    for (int i=0;i<hash.length;i++) {
				String hex = Integer.toHexString(0xFF & hash[i]);
				if (hex.length()==1) {
				    hexstring.append('0');
				}
				hexstring.append(hex);
				sha256hash = hexstring.toString();
				if (plaintext.equals(sha256hash)) {
					Toast.makeText(DeadManSwitch.this, "Authentication successful", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(DeadManSwitch.this, "Authentication failure", Toast.LENGTH_LONG).show();
					//WAY TO SET LIMIT ON TRIES
					//AFTER THREE TRIES DEADMAN'S SWITCH ACTIVATES ANYWAY?
				}
			    }
			} catch (NoSuchAlgorithmException nsae) {
			};
		    }; //closes "if (v == aliveButton)"
		};
	    });
    };

    @Override
    public void onPause() {
	super.onPause();

	if (receiver != null) {
	    Log.w("deadmanswitch", "Unregistering receiver...");
	    unregesterReceiver(receiver);
	    receiver = null;
	}
    }

    @Override
    public void onResume() {
	super.onResume();

	Log.w("deadmanswitch", "Restart called...");
    }

    @Override
    public void onStart() {
	super.onStart();
	registerPassphraseActivityStarted();
    }

    @Override
    public void onStop() {
	super.onStop();
	havePromptedForPassphrase = false;
	registerPassphraseActivityStopped();
    }

    @Override
    public void onDestroy() {
	Log.w("deadmanswitch", "onDestroy...");
	//unregisterReceiver(killActivityReceiver);
	//MemoryCleaner.clean(masterSecret);
	super.onDestroy();
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
	super.onPrepareOptionsMenu(menu);

	menu.clear();
	
	if (!incognitoMode) prepareNoncognitoMainMenu(menu);
	else                prepareIncognitoMainMenu(menu);
	
	return true;
    }
    
    // TODO add actual icons
    private void prepareNoncognitoMainMenu(Menu menu) {
	menu.add(0, MENU_SETUP_KEY, Menu.NONE, "Setup").setIcon(android.R.drawable.icon_menu_dummy);

	if (masterSecret != null) menu.add(0, MENU_OPTIONS_KEY, Menu.NONE, "Options").setIcon(android.R.drawable.icon_menu_dummy);
	else menu.add(0, MENU_PASSPHRASE_KEY, Menu.NONE, "Enter passphrase").setIcon(R.drawable.icon_menu_dummy);
	
	menu.add(0, MENU_TASKS_KEY, Menu.NONE, "Tasks").setIcon(android.R.drawable.icon_menu_dummy);
	menu.add(0, MENU_INCOGITO_KEY, Menu.NONE, "Incognito Mode").setIcon(android.R.drawable.icon_menu_dummy);
	menu.add(0, MENU_PANIC_KEY, Menu.NONE, "PANIC").setIcon(android.R.drawable.icon_menu_dummy);
	menu.add(0, MENU_EXIT_KEY, Menu.NONE, "Exit").setIcon(android.R.drawable.icon_menu_dummy);
    }

    private void prepareIncognitoMainMenu(Menu menu) {
	menu.add(0, MENU_SETUP_KEY, Menu.NONE, "Setup").setIcon(android.R.drawable.icon_menu_dummy);

	if (masterSecret != null) menu.add(0, MENU_OPTIONS_KEY, Menu.NONE, "Options").setIcon(android.R.drawable.icon_menu_dummy);
	else menu.add(0, MENU_PASSPHRASE_KEY, Menu.NONE, "Enter passphrase").setIcon(R.drawable.icon_menu_dummy);
	
	menu.add(0, MENU_TASKS_KEY, Menu.NONE, "Tasks").setIcon(android.R.drawable.icon_menu_dummy);
	menu.add(0, MENU_NONINCOGITO_KEY, Menu.NONE, "Normal Mode").setIcon(android.R.drawable.icon_menu_dummy);
	menu.add(0, MENU_PANIC_KEY, Menu.NONE, "PANIC").setIcon(android.R.drawable.icon_menu_dummy);
	menu.add(0, MENU_EXIT_KEY, Menu.NONE, "Exit").setIcon(android.R.drawable.icon_menu_dummy);
    }
    
    @Override
    public boolean onMainMenuItemSelected(MenuItem item) {
	super.onMainMenuItemSelected(item);

	switch (item.getItemID()) {
	    
	    // TODO if statement to wrap and check for correct password
	case MENU_SETUP_KEY:
	    Intent setupWizard = new Intent(this, SetupWizard.class);
	    // TODO SetupWizard.class
	    setupWizard.putExtra("master_secret", masterSecret);
	    // TODO what does intent.putExtra do?
	    startActivity(setupWizard);
	    // TODO where is the startActivity method located?
	    return true;

	case MENU_OPTIONS_KEY:
	    Intent optionsMenu = new Intent(this, OptionsMenu.class);
	    // TODO OptionsMenu.class
	    optionsMenu.putExtra("master_secret", masterSecret);
	    
	    startActivity(optionsMenu);
	    return true;

	case MENU_PASSPHRASE_KEY:
	    promptForPassphrase();
	    return true;

	case MENU_TASKS_KEY:
	    Intent tasksMenu = new Intent(this, TasksMenu.class);
	    // TODO TasksMenu.class
	    tasksMenu.putExtra("master_secret", masterSecret);
	    startActivity(tasksMenu);
	    return true;
	    
	case MENU_INCOGNITO_KEY:
	    // TODO switch to incognito mode
	    // TODO how do we accomplish this?
	    //initiateIncognitoMode();
	    return true;

	case MENU_PANIC_KEY:
	    // TODO setOffTimer();
	    // TODO should this lead to another menu?
	    // needs some type of failsafe, so not pressed accidentally
	    return true;

	case MENU_EXIT_KEY:
	    // TODO how to safely exit the app and shutdown all threads?
	    return true;

	case MENU_NONCOGNITO_KEY:
	    // TODO switch to noncognito mode
	    //initiateNoncognitoMode();
	    return true;
	}

	return false;
    }

    private void registerPassphraseActivityStarted() {
	Intent intent = new Intent(this, KeyCachingService.class);
	intent.setAction(KeyCachingService.ACTIVITY_START_EVENT);
	startService(intent);
    }

    private void registerPassphraseActivityStopped() {
	Intent intent = new Intent(this, KeyCachingService.class);
	intent.setAction(KeyCachingService.ACTIVITY_STOP_EVENT);
	startService(intent);
    }
    
    private void promptForPassphrase() {
	havePromptedForPassphrase = true;
	if (hasSelectedPassphrase()) startActivity(new Intent(this, PassphrasePromptActivity.class));
	else startActivity(new Intent(this, PassphraseCreateActivity.class));
    }
    
    private boolean hasSelectedPassphrase() {
	SharedPreferences settings = getSharedPreferences(KeyCachingService.PREFERENCES_NAME, 0);
	return settings.getBoolean("passphrase_initialized", false);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
		KeyCachingService keyCachingService = ((KeyCachingService.KeyCachingBinder)service).getService();
		MasterSecret masterSecret = keyCachingService.getMasterSecret();

		initializeWithMasterSecret(masterSecret);

		if (masterSecret == null && !havePromptedForPassphrase)
		    promptForPassphrase();
		
		Intent cachingIntent = new Intent(DeadManSwitch.this, KeyCachingService.class);
		
		startService(cachingIntent);

		try {
		    DeadManSwitch.this.unbindService(this);
		} catch (IllegalArgumentExeception iae) {
		    Log.w("DeadManSwitch", iae);
		}
	    }

	    public void onServiceDisconnected(ComponentName name) {}
	};
    
    private class KillActivityReceiver extends BroadcastReceiver {
	@Override
	    public void onReceive(Context arg0, Intent arg1) {
	    finish();
	}
    };
}
    /** Method for returning button listeners.
    public View.OnClickListener CreateOnClickListener(final Context context, final Class<?> DeadManSwitch) {
	View.OnClickListener btnlistener = new View.OnClickListener() {
		public void onClick(View v) {
		    Intent passwdbox = new Intent(context, DeadManSwitch);
 		    context.startActivity(passwdbox);			    	
		    };
		};
	    };
	return btnlistener;
	}; */

/** The following implements the timer function and 
    I'm not quite sure if it needs to go into the 
    public class of previous function.
    Code example taken from http://www.java2s.com/Code/
    Java/Development-Class/UsejavautilTimertoschedulea
    tasktoexecuteonce5secondshavepassed.htm */

/**protected class SetOffTimer {
    Toolkit toolkit;
    
    Timer timer;
    
    public SetOffTimer(int seconds) {
	toolkit = Toolkit.getDefaultToolkit();
	timer = new Timer();
	
	timer.schedule(new TrippedSwitch(), seconds * 1000);
    }

    public class TrippedSwitch extends TimerTask {
	public void run() {
	    TextView tv = new TextView(this);
	    // Can I use System.out.println() here instead of 
		//android.textView()? 
	    tv.setText("Deactivation Failure: Dead Man's Switch has been triggered to execute scheduled tasks");
	    setContentView(tv);
	    tookit.beep();
	    System.exit(0);
	}
    }
    
    public void main(String args[]) {
	TextView tv = new TextView(this);
	
	tv.setText("Dead Man's Switch Activating...");
	new SetOffTimer(10);
	tv.setText("Dead Man's Switch Activated: You must continue to deactivate the Dead Man's Switch at the specified interval to defer execution of scheduled tasks");
    }
    }		*/