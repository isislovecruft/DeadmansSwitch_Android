package org.marchhare.deadmanswitch.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.marchhare.deadmanswitch.OptionsMenu;
import org.marchhare.deadmanswitch.R;
import org.marchhare.deadmanswitch.DeadManSwitch;
import org.marchhare.deadmanswitch.crypto.MasterSecret;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class KeyCachingService extends Service {

  public static final int    NOTIFICATION_ID    = 1337;
  public static final int    SERVICE_RUNNING_ID = 6666;
	
  public  static final String KEY_PERMISSION           = "org.marchhare.deadmanswitch.ACCESS_SECRETS";
  public  static final String NEW_KEY_EVENT            = "org.marchhare.deadmanswitch.service.action.NEW_KEY_EVENT";
  public  static final String PASSPHRASE_EXPIRED_EVENT = "org.marchhare.deadmanswitch.service.action.PASSPHRASE_EXPIRED_EVENT";
  public  static final String CLEAR_KEY_ACTION         = "org.marchhare.deadmanswitch.service.action.CLEAR_KEY";
  public  static final String ACTIVITY_START_EVENT     = "org.marchhare.deadmanswitch.service.action.ACTIVITY_START_EVENT";
  public  static final String ACTIVITY_STOP_EVENT      = "org.marchhare.deadmanswitch.service.action.ACTIVITY_STOP_EVENT";
  public  static final String PREFERENCES_NAME         = "Deadman Switch Preferences";

  private static final Class[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
  private static final Class[] mStopForegroundSignature = new Class[] {boolean.class};
    
  private PendingIntent pending;
  private NotificationManager notificationManager;
  private Method mStartForeground;
  private Method mStopForeground;
    
  private Object[] mStartForegroundArgs = new Object[2];
  private Object[] mStopForegroundArgs  = new Object[1];
  private int activitiesRunning         = 0;
  private final IBinder binder          = new KeyCachingBinder();
	
  private MasterSecret masterSecret;
	
  public KeyCachingService() {}

  public synchronized MasterSecret getMasterSecret() {
    return masterSecret;
  }
	
  public synchronized void setMasterSecret(MasterSecret masterSecret) {
    this.masterSecret = masterSecret;
		
    foregroundService();
    broadcastNewSecret();
    startTimeoutIfAppropriate();
  }
	
  @Override
  public void onStart(Intent intent, int startId) {
    if (intent.getAction() != null && intent.getAction().equals(CLEAR_KEY_ACTION))
      handleClearKey();
    else if (intent.getAction() != null && intent.getAction().equals(ACTIVITY_START_EVENT))
      handleActivityStarted();
    else if (intent.getAction() != null && intent.getAction().equals(ACTIVITY_STOP_EVENT))
      handleActivityStopped();
    else if (intent.getAction() != null && intent.getAction().equals(PASSPHRASE_EXPIRED_EVENT))
      handlePassphraseExpired();
  }
	
  @Override
  public void onCreate() {
    pending             = PendingIntent.getService(this, 0, new Intent(PASSPHRASE_EXPIRED_EVENT, null, this, KeyCachingService.class), 0);
    notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

    try {
      mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
      mStopForeground  = getClass().getMethod("stopForeground", mStopForegroundSignature);
    } catch (NoSuchMethodException e) {
      // Running on an older platform.
      mStartForeground = mStopForeground = null;
    }        
  }
	
  @Override
  public void onDestroy() {
    Log.e("KeyCachingService", "KeyCachingService Is Being Destroyed!");
  }
	
  private void handleActivityStarted() {
    Log.w("KeyCachingService", "Incrementing activity count...");
		
    AlarmManager alarmManager = (AlarmManager)this.getSystemService(ALARM_SERVICE);
    alarmManager.cancel(pending);				
    activitiesRunning++;
  }

  private void handleActivityStopped() {
    Log.w("KeyCachingService", "Decrementing activity count...");

    activitiesRunning--;
    startTimeoutIfAppropriate();
  }
	
  private void handleClearKey() {
    this.masterSecret = null;
    stopForegroundCompat(SERVICE_RUNNING_ID);		
  }
	
  private void handlePassphraseExpired() {
    handleClearKey();
    Intent intent = new Intent(PASSPHRASE_EXPIRED_EVENT);
    intent.setPackage(getApplicationContext().getPackageName());
    
    sendBroadcast(intent, KEY_PERMISSION);
  }
	
  private void startTimeoutIfAppropriate() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);	
    if ((activitiesRunning == 0) && (this.masterSecret != null)) {
      long timeoutHours = sharedPreferences.getInt(OptionsMenu.SET_TIMER_INTERVAL_PREF, 4);
      long timeoutMillis  = timeoutHours * 60 * 60 * 1000;

      Log.w("KeyCachingService", "Starting timeout: " + timeoutMillis);
			
      AlarmManager alarmManager = (AlarmManager)this.getSystemService(ALARM_SERVICE);
      alarmManager.cancel(pending);
      alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + timeoutMillis, pending);
    }		
  }
	
  private void foregroundService() {
    Notification notification  = new Notification(R.drawable.icon, "DeadmanSwitch Passphrase Cached", System.currentTimeMillis());
    Intent intent              = new Intent(this, DeadManSwitch.class);
    PendingIntent launchIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
    notification.setLatestEventInfo(getApplicationContext(), "DeadmanSwitch Cached", "DeadmanSwitch Passphrase Cached", launchIntent);
		
    stopForegroundCompat(SERVICE_RUNNING_ID);
    startForegroundCompat(SERVICE_RUNNING_ID, notification);
  }
	
  private void broadcastNewSecret() {
    Log.w("service", "Broadcasting new secret...");
    Intent intent = new Intent(NEW_KEY_EVENT);
    intent.putExtra("master_secret", masterSecret);
    intent.setPackage(getApplicationContext().getPackageName());

    sendBroadcast(intent, KEY_PERMISSION);
  }
	
  @Override
  public IBinder onBind(Intent arg0) {
    return binder;
  }
	
  public class KeyCachingBinder extends Binder {
    public KeyCachingService getService() {
      return KeyCachingService.this;
    }
  }

  /**
   * This is a wrapper around the new startForeground method, using the older
   * APIs if it is not available.
   */
  private void startForegroundCompat(int id, Notification notification) {
      Log.w("KeyCachingService", "Calling startForeground.");
      if (mStartForeground != null) {
	  mStartForegroundArgs[0] = Integer.valueOf(id);
	  mStartForegroundArgs[1] = notification;
	  
	  try {
	      mStartForeground.invoke(this, mStartForegroundArgs);
	  } catch (InvocationTargetException e) {
	      Log.w("KeyCachingService", "Unable to invoke startForeground", e);
	  } catch (IllegalAccessException e) {
	      Log.w("KeyCachingService", "Unable to invoke startForeground", e);
	  }
	  return;
      }        
      setForeground(true);
      notificationManager.notify(id, notification);
  }
    
  /**
   * This is a wrapper around the new stopForeground method, using the older
   * APIs if it is not available.
   */
  private void stopForegroundCompat(int id) {
      Log.w("KeyCachingService", "Calling stopForeground!");
      if (mStopForeground != null) {
	  mStopForegroundArgs[0] = Boolean.TRUE;
	  try {
	      mStopForeground.invoke(this, mStopForegroundArgs);
	  } catch (InvocationTargetException e) {
	      Log.w("KeyCachingService", "Unable to invoke stopForeground", e);
	  } catch (IllegalAccessException e) {
	      Log.w("KeyCachingService", "Unable to invoke stopForeground", e);
	  }
	  return;
      }
      
      notificationManager.cancel(id);
      setForeground(false);
  }
}
