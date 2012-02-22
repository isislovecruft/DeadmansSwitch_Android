package org.marchhare.deadmanswitch.preferences;

import org.marchhare.deadmanswitch.OptionsMenu;
import org.marchhare.deadmanswitch.R;

import android.content.Context;
import android.content.DialogInterface;
import android.preferences.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public class TimerMarginOfErrorPreference extends DialogPreference {
    
    private Spinner scaleSpinner;
    private SeekBar seekBar;
    private TextView intervalText;
    
    public TimerMarginOfErrorPreference(Context context, AttributeSet attrs) {
	super(context, attrs);
	// TODO make layout
	this.setDialogLayoutResource(R.layout.timer_interval_dialog);
	this.setPositiveButtonText("Ok");
	this.setNegativeButtonText("Cancel");
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
	if (which == DialogInterface.BUTTON_POSITIVE) {
	    int interval;
	    
	    if (scaleSpinner.getSelectedItemPosition() == 0) {
		interval = Math.max(seekBar.getProgress(), 1);
	    } else {
		// ^minutes _hours
		interval = Math.max(seekBar.getProgress(), 1) * 60;
	    }

	    this.getSharedPreferences().edit().putInt(OptionsMenu.TIMER_MARGIN_OF_ERROR__PREF, interval).commit();
	}
	
	super.onClick(dialog, which);
    }

    private void initializeDefaults() {
	int timeout = this.getSharedPreferences().getInt(OptionsMenu.TIMER_MARGIN_OF_ERROR_PREF, 15);

	if (timeout > 60) {
	    scaleSpinner.setSelection(1);
	    seekBar.setMax(6);
	    seekBar.setProgress(timeout / 60);
	    intervalText.setText((timeout / 60) + "");
	} else {
	    scaleSpinner.setSelection(0);
	    seekBar.setMax(60);
	    seekBar.setProgress(timeout);
	    intervalText.setText(timeout + "");
	}
    }

    private void initializeListeners() {
	this.seekBar.setOnSeekBarChangeListener( new SeekbarChangedListener());
	this.scaleSpinner.setOnItemSelectedListener(new ScaleSelectedListener());
    }

    private class ScaleSelectedListener implements AdapterView.OnItemSelectedListener {
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long selected) {
	    if (selected == 0) {
		seekBar.setMax(60);
	    } else {
		seekBar.setMax(6);
	    }
	}
	
	public void onNothingSelected(AdapterView<?> arg0) {
	}
    }
	
    public class SeekbarChangedListener implements SeekBar.OnSeekBarChangeListener {
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	    if (progress < 1)
		progress = 1;
	    
	    intervalText.setText(progress + "");
	}
	
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	
	public void onStopTrackingTouch(SeekBar seekBar) {
	}
    }
}