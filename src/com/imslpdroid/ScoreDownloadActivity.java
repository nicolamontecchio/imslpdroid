package com.imslpdroid;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.ProgressBar;

import com.imslpdroid.data.DataStorage;
import com.imslpdroid.data.Score;
import com.imslpdroid.gui.IntentUtils;

// TODO figure out how to save instance state so that download does not get interrupted ...

public class ScoreDownloadActivity extends Activity {

	private static final String disclaimterHtmlText = "<center><p><i>IMSLP makes no guarantee that the files provided for download "
			+ "on IMSLP are public domain in your country and assumes no legal responsibility or liability of any kind for their copyright status. "
			+ "Please obey the copyright laws of your country and consult the copyright statute itself or a qualified IP attorney to verify whether "
			+ "a certain file is in the public domain in your country or if downloading a copy constitutes fair use.</i></p><span style=\"color:crimson\">"
			+ "<b>BY CLICKING ANY LINK ON THIS SITE INCLUDING THE LINK BELOW, YOU ACKNOWLEDGE THAT YOU UNDERSTAND AND AGREE TO THE ABOVE DISCLAIMER.</b>"
			+ "</span><br><b>The disclaimer also applies to the creator of this application.</b></center>";

	private Score score = null;
	private ScoreDownloadTask task = null;
	private AtomicBoolean stopFlag = new AtomicBoolean(false);
	private boolean downloadErrorOccurred = false;
	private static final int DL_CHUNK_SIZE = 8192 * 16;
	private static final int DIALOG_DISCLAIMER = 1;
	private static final int DIALOG_STORAGE_ERROR = 2;
	private static final int DIALOG_DOWNLOAD_ERROR = 3;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DISCLAIMER:
			Spanned marked_up = Html.fromHtml(disclaimterHtmlText);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(marked_up).setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					task = new ScoreDownloadTask();
					task.execute();
				}
			}).setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					finish();
				}
			});
			return builder.create();
		case DIALOG_STORAGE_ERROR:
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setMessage(getString(R.string.device_storage_error)).setCancelable(false).setPositiveButton("ok", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							finish();
						}
					});
			return builder2.create();
		case DIALOG_DOWNLOAD_ERROR:
			AlertDialog.Builder builder3 = new AlertDialog.Builder(this);
			builder3.setMessage(getString(R.string.download_error)).setCancelable(false).setPositiveButton("ok", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							finish();
						}
					});
			return builder3.create();
		default:
			return super.onCreateDialog(id);
		}
	}

	private class ScoreDownloadTask extends AsyncTask<Void, Integer, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet("http://imslp.org/wiki/Special:IMSLPDisclaimerAccept/" + score.getScoreId());
				HttpResponse response = httpclient.execute(httpget);
				HttpEntity entity = response.getEntity();
				Header contentType = entity.getContentType();
				for (HeaderElement element : contentType.getElements()) {
					String responseType = element.getName();
					if (responseType.equals("application/pdf")) {
						int size = -1;
						for (Header h : response.getAllHeaders())
							if (h.getName().equals("Content-Length"))
								size = Integer.parseInt(h.getValue());
						InputStream contentStream = response.getEntity().getContent();

						// 2. download data
						FileOutputStream fos = new FileOutputStream(DataStorage.getDownloadedScoreFile(score));
						int totRead = 0;
						while (!stopFlag.get()) {
							byte[] data = new byte[DL_CHUNK_SIZE];
							int dataread = contentStream.read(data);
							if (dataread == -1)
								break;
							fos.write(data, 0, dataread);
							totRead += dataread;
							publishProgress((100 * totRead) / size);
						}
						fos.close();
						if (stopFlag.get() && DataStorage.getDownloadedScoreFile(score).exists())
							DataStorage.getDownloadedScoreFile(score).delete(); // if it was interrupted
						DataStorage.addDownloadedFileInDB(getApplicationContext(), score);
						DataStorage.syncronizeDownloadedFileTable(getApplicationContext());
					}
				}
			} catch (IOException e) {
				downloadErrorOccurred = true;
			}
			return null;
		}

		@Override
		protected void onCancelled() {
			stopFlag.set(true);
			finish();
		}

		@Override
		protected void onPostExecute(Void result) {
			if (!downloadErrorOccurred) {
				if (IntentUtils.isPdfReaderAvailable(getApplicationContext())) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(DataStorage.getDownloadedScoreFile(score)), "application/pdf");
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				}
				finish();
			} else {
				showDialog(DIALOG_DOWNLOAD_ERROR);
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			ProgressBar pb = (ProgressBar) findViewById(R.id.dl_progress);
			pb.setMax(100);
			pb.setProgress(Math.min(100, values[0]));
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		score = (Score) getIntent().getExtras().get("score");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scoredownload);
		if (DataStorage.getDownloadedScoreFile(score).exists())
			DataStorage.deleteDownloadedFile(getApplicationContext(), score);
		if (!DataStorage.getExternalDownloadPath().exists())
			if (!DataStorage.getExternalDownloadPath().mkdirs()) {
				showDialog(DIALOG_STORAGE_ERROR);
				return;
			}
		if (!DataStorage.getExternalDownloadPath().canWrite()) {
			showDialog(DIALOG_STORAGE_ERROR);
			return;
		}
		showDialog(DIALOG_DISCLAIMER);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(task != null)
			task.cancel(true);
	}

}
