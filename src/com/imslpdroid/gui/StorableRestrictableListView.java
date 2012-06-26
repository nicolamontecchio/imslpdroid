package com.imslpdroid.gui;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.imslpdroid.R;
import com.imslpdroid.data.DataStorage;

/**
 * A RestrictableListView that gets automatically stored in the database.
 */
public abstract class StorableRestrictableListView extends RestrictableListView {

	/**
	 * Get the base url of the page that is used to generate the list.
	 * This is used as key in the multi-map stored in the DB.
	 * In case of multiple pagination results, this is the FIRST one.
	 */
	public abstract String getBaseUrl();

	/**
	 * must be implemented by all activities -
	 * used to download list of items from imslp
	 */
	public abstract List<String> downloadList(Handler handler, AtomicBoolean stopFlag) throws IOException;

	private ProgressDialog progressDialog = null;
	private static final int DIALOG_PROGRESS = 0;
	private GetListTask currentTask = null;

	public static Message getNotificationMessage(String notificationDialogText, String notificationDialogTitle) {
		if (notificationDialogTitle == null)
			notificationDialogTitle = "";
		if (notificationDialogText == null)
			notificationDialogText = "";
		Message m = new Message();
		Bundle b = new Bundle();
		b.putString("msg", notificationDialogText);
		b.putString("title", notificationDialogTitle);
		m.setData(b);
		return m;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			progressDialog = new ProgressDialog(StorableRestrictableListView.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setCancelable(true);
			progressDialog.setMessage("");
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					currentTask.cancel(true);
				}
			});
			return progressDialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	/** Task that handles all dialog messages, downloads ... */
	private class GetListTask extends AsyncTask<String, String, List<String>> {

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (progressDialog != null && progressDialog.isShowing())
				dismissDialog(DIALOG_PROGRESS);
			stopFlag.set(true);
		}

		private class GLTHandler extends Handler {
			@Override
			public void handleMessage(Message msg) {
				publishProgress(new String[] { msg.getData().getString("msg") });
			}
		}

		// handle progress and notification message
		private Handler h = new GLTHandler();
		// used for forcing stop ...
		private AtomicBoolean stopFlag = new AtomicBoolean(false);

		@Override
		protected List<String> doInBackground(String... params) {
			boolean forcedRefresh = false;
			for (String s : params)
				if (s.equals("forcerefresh"))
					forcedRefresh = true;
			try {
				if (forcedRefresh) {
					publishProgress(getString(R.string.downloadingfromimslp));
					List<String> list = downloadList(h, stopFlag);
					if (!stopFlag.get())
						DataStorage.writeGenericListToDB(getApplicationContext(), getBaseUrl(), list);
					return list;
				} else {
					List<String> list = DataStorage.readGenericListFromDB(getApplicationContext(), getBaseUrl());
					if(list.isEmpty()) {
						list = getPreCachedItems();
						if(list.size() > 0) {
							publishProgress(getString(R.string.initializing_data));
							DataStorage.writeGenericListToDB(getApplicationContext(), getBaseUrl(), list);
						}
					}
					if (list.isEmpty()) {
						publishProgress(getString(R.string.downloadingfromimslp));
						list = downloadList(h, stopFlag);
						if(!stopFlag.get())
							DataStorage.writeGenericListToDB(getApplicationContext(), getBaseUrl(), list);
					}
					
					return list;
				}
			} catch (IOException e) {
				Log.e("StorableRestrictableListView", "EXCEPTION - " + e.toString());
				return null;
			}
		}

		@Override
		protected void onPreExecute() {
			if (progressDialog != null && progressDialog.isShowing())
				dismissDialog(DIALOG_PROGRESS);
			if (currentTask != null && currentTask.getStatus() == AsyncTask.Status.RUNNING)
				currentTask.cancel(true);
			currentTask = this;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			if (progressDialog == null)
				showDialog(DIALOG_PROGRESS);
			if (progressDialog.isShowing()) {
				progressDialog.setMessage(values[0]);
			}
		}

		@Override
		protected void onPostExecute(List<String> result) {
			if (result != null)
				setListViewElements(result);
			else
				Toast.makeText(StorableRestrictableListView.this, getString(R.string.list_download_error), Toast.LENGTH_LONG).show();
			if (progressDialog != null && progressDialog.isShowing())
				dismissDialog(DIALOG_PROGRESS);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new GetListTask().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_activity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh_list_menu_button:
			new GetListTask().execute("forcerefresh");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/** ComposersActivity is the only one for now to override this */
	public List<String> getPreCachedItems() {
		return new LinkedList<String>();
	}

}
