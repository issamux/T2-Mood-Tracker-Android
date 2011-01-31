package com.t2.vas.activity;

import android.content.Intent;
import android.os.Bundle;

import com.t2.vas.ReminderService;
import com.t2.vas.SharedPref;

public class StartupActivity extends ABSActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Start the reminder service if it isn't already.
		ReminderService.startRunning(this);
		
		Intent i;
		if(SharedPref.Security.isEnabled(sharedPref)) {
			i = new Intent(this, MainActivity.class);
		} else {
			i = new Intent(this, SplashScreenActivity.class);
		}
		
		this.startActivity(i);
		this.finish();
	}

}
