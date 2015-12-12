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

		intent = new Intent().setClass(this, AboutAppActivity.class);
		spec = tabHost.newTabSpec("help").setIndicator(getString(R.string.aboutapp), res.getDrawable(R.drawable.ic_tabs_aboutapp)).setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);

		if (!IntentUtils.isPdfReaderAvailable(getApplicationContext()))
			IntentUtils.noPdfReaderDialog(this);

	}

}