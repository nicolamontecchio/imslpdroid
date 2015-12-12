package com.imslpdroid;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

import com.imslpdroid.gui.IntentUtils;

/**
 * Activity for the main screen, contains the various tabs
 */
public class ImslpdroidActivity extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Resources res = getResources();
		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;

		// downloaded is the first tab because it is much faster than composersactivity to load
		intent = new Intent().setClass(this, DownloadedActivity.class);
		spec = tabHost.newTabSpec("downloaded").setIndicator(getString(R.string.downloaded), res.getDrawable(R.drawable.ic_tabs_recent)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, ComposersActivity.class);
		spec = tabHost.newTabSpec("composers").setIndicator(getString(R.string.composers), res.getDrawable(R.drawable.ic_tabs_composers)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, TimePeriodActivity.class);
		spec = tabHost.newTabSpec("timePeriod").setIndicator(getString(R.string.period), res.getDrawable(R.drawable.ic_tabs_timeperiod)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, NationalityActivity.class);
		spec = tabHost.newTabSpec("nationality").setIndicator(getString(R.string.nationality), res.getDrawable(R.drawable.ic_tabs_nationality))
				.setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, WorkTypesActivity.class);
		spec = tabHost.newTabSpec("worktypes").setIndicator(getString(R.string.worktype), res.getDrawable(R.drawable.ic_tabs_worktype)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, InstrumentationActivity.class);
		spec = tabHost.newTabSpec("instrumentations").setIndicator(getString(R.string.instrumentation), res.getDrawable(R.drawable.ic_tabs_instrumentation))
				.setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, AboutAppActivity.class);
		spec = tabHost.newTabSpec("help").setIndicator(getString(R.string.aboutapp), res.getDrawable(R.drawable.ic_tabs_aboutapp)).setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(6); // show "about" tab on opening

		if (!IntentUtils.isPdfReaderAvailable(getApplicationContext()))
			IntentUtils.noPdfReaderDialog(this);

	}

}