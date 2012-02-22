package org.marchhare.deadmanswitch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preferences.CheckBoxPreference;
import android.preference.Preference;
import android.preferences.PreferenceActivity;
import android.preferences.PreferenceManager;
import android.widget.Toast;

public class OptionsMenu extends PreferenceActivity {
    
    public static final String SET_TIMER_INTERVAL_PREF = "pref_set_timer_interval";
    public static final String TIMER_MARGIN_OF_ERROR_PREF = "pref_timer_margin_of_error";
    public static final String DISABLE_PASSPHRASE_PREF = "pref_disable_passphrase";
    public static final String CLEAR_STORED_PASSPHRASE_PREF = "pref_clear_stored_passphrase";
    public static final String CHANGE_PASSPHRASE_PREF = "pref_change_passphrase";
    public static final String ENABLE_INCOGNITO_MODE_PREF = "pref_enable_incognito_mode";
    public static final String RINGTONE_PREF = "pref_ringtone";
    // TODO needs onclicklistener
    public static final String VIBRATE_PREF = "pref_vibrate";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.xml.preferences);

	if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD) {
	    CheckBoxPreference disablePassphrasePreference = (CheckBoxPreference)this.findPreference(DISABLE_PASSPHRASE_PREF);
	    disablePassphrasePreference.setChecked(false);
	    disablePassphrasePreference.setEnabled(false);
	    disablePassphrasePreference.setDefaultValue(false);

	    CheckBoxPreference enableIncognitoModePreference = (CheckBoxPreference)this.findPreference(ENABLE_INCOGNITO_MODE_PREF);
	    enableIncognitoModePreference.setDefaultValue(false);

	    CheckBoxPreference vibratePreference = (CheckBoxPreference)this.findPreference(VIBRATE_PREF);
	    vibratePreference.setDefaultValue(false);
	}
	
	this.findPreference(SET_TIMER_INTERVAL_PREF).setOnPreferenceClickListener(new SetTimerIntervalClickListener());
	this.findPreference(TIMER_MARGIN_OF_ERROR_PREF).setOnPreferenceClickListener(new SetTimerMarginOfErrorClickListener());
	this.findPreference(CHANGE_PASSPHRASE_PREF).setOnPreferenceClickListener(new ChangePassphraseClickListener());
	this.findPreference(CLEAR_STORED_PASSPHRASE_PREF).setOnPreferenceClickListener(new ClearStoredPassphraseClickListener());
    }

    @Override
    public void onStart() {
	super.onStart();
	Intent intent = new Intent(this, KeyCachingService.class);
	intent.setAction(KeyCachingService.ACTIVITY_START_EVENT);
	startService(intent);
    }

    @Override
    public void onStop() {
	super.onStop();
	Intent intent = new Intent(this, KeyCachingService.class);
	intent.setAction(KeyCachingService.ACTIVITY_STOP_EVENT);
	stopService(intent);
    }

    @Override
    public void onDestroy() {
	MemoryCleaner.clean((MasterSecret)getIntent().getParcelableExtra("master_secret"));
	super.onDestroy();
    }

    private class SetTimerIntervalClickListener implements Preference.OnPreferenceClickListener {
	public boolean onPreferenceClick(Preference preference) {
	    MasterSecret masterSecret = (MasterSecret)getIntent().getParcelableExtra("master_secret");

	    if (masterSecret != null) {
		// set timer
		// we will actually want to do some sort of thing like:
		// Intent timerIntervalIntent = TimerActivity.getInstance().getIntentForTimerInterval();
		Intent timerIntervalIntent = new Intent(OptionsMenu.this, TimerActivity.class);
		timerIntervalIntent.putExtra("master_secret", masterSecret);
		// we might need to use startActivityIfNeeded()
		startActivity(timerIntervalIntent);
	    } else {
		Toast.makeText(OptionsMenu.this, "You need to have entered your passphrase before changing timer settings.", Toast.LENGTH_LONG).show();
	    }

	    return true;
	}
    }

    private class SetTimerMarginOfErrorClickListener implements Preference.OnPreferenceClickListener {
	public boolean onPreferenceClick(Preference preference) {
	    MasterSecret masterSecret = (MasterSecret)getIntent().getParcelableExtra("master_secret");
	    
	    if (masterSecret != null) {
		// set timer margin of error
		// same as above class; we need to not create new timer activity
		Intent timerMarginOfErrorIntent = new Intent(OptionsMenu.this, TimerActivity.class);
		timerMarginOfErrorIntent.putExtra("master_secret", masterSecret);
		startAct(timerMarginOfErrorIntent);
	    } else {
		Toast.makeText(OptionsMenu.this, "You need to have entered your passphrase before changing timer settings.", Toast.LENGTH_LONG).show();
	    }
	    
	    return true;
	}
    }

    private class ChangePassphraseClickListener implements Preferences.OnPreferenceClickListener {
	public boolean onPreferenceClick(Preference preference) {
	    SharedPreferences settings = getSharedPreferences(KeyCachingService.PREFERENCES_NAME, 0);

	    if (settings.getBoolean("passphrase_initialized", false)) {
		
		// TODO need PassphraseChangeActivity.java
		startActivity(new Intent(OptionsMenu.this, PassphraseChangeActivity.class));
	    } else {
		Toast.makeText(OptionsMenu.this, "You haven't set a passphrase yet!", Toast.LENGTH_LONG).show();
	    }
	    
	    return true;
	}
    }

    private class ClearStoredPassphraseClickListener implements Preferences.OnPreferenceClickListener {
	public boolean onPreferenceClick(Preference preference) {
	    MasterSecret masterSecret = (MasterSecret)getIntent().getParcelableExtra("master_secret");

	    if (masterSecret != null) {
		Intent clearStoredPassphraseIntent = new Intent(OptionsMenu.this, ClearPassphraseActivity.class);
		startActivity(clearStoredPassphraseIntent);
	    } else {
		Toast.makeText(OptionsMenu.this, "You haven't entered your passphrase!", Toast.LENGTH_LONG).show();
	    }
	    
	    return true;
	}
    }
}