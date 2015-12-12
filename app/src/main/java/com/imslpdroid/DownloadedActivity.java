package com.imslpdroid;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.imslpdroid.data.DataStorage;
import com.imslpdroid.gui.IntentUtils;
import com.imslpdroid.gui.RestrictableListView;

public class DownloadedActivity extends RestrictableListView {

	private static final int DIALOG_SCOREINFO = 1;
	private static final int DIALOG_NOFILE = 0;
	private static String infodialog_display = null;

	public void loadFileList() {
		DataStorage.syncronizeDownloadedFileTable(getApplicationContext());
		List<String> filesInfo = DataStorage.getDownloadedFilesName(getApplicationContext());
		//		if(filesInfo.isEmpty())
		//			showDialog(DIALOG_NOFILE);
		setListViewElements(filesInfo);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadFileList();

		// click on list item
		ListView lv = (ListView) findViewById(R.id.rlv_listview);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String info = (String) ((TextView) view).getText();
				String[] split = info.split("IMSLP");
				String score = split[split.length - 1].trim() + ".pdf";
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(new File(DataStorage.getExternalDownloadPath(), score)), "application/pdf");
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				if (IntentUtils.isPdfReaderAvailable(getApplicationContext()))
					startActivity(intent);
				else {
					Log.i("imslpdroidactivity", "no pdf viewer installed");
					AlertDialog.Builder builder = new AlertDialog.Builder(DownloadedActivity.this);
					builder.setMessage(R.string.nopdfviewer).setCancelable(false).setPositiveButton("ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}
			}
		});

		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> l, View view, int position, long id) {
				String info = (String) ((TextView) view).getText();
				String[] split = info.split("IMSLP");
				final String score = split[split.length - 1].trim() + ".pdf";
				List<String> itemlist = new LinkedList<String>();
				itemlist.add(getString(R.string.lic_info));
				itemlist.add(getString(R.string.lic_delete));

				final CharSequence[] items = itemlist.toArray(new CharSequence[0]);
				AlertDialog.Builder builder = new AlertDialog.Builder(DownloadedActivity.this);
				builder.setTitle(getString(R.string.lic_chooseaction));
				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						CharSequence choice = items[item];
						if (choice.equals(getString(R.string.lic_delete))) {
							Log.i("scoresactivity", "deleting file");
							DataStorage.deleteDownloadedFile(getApplicationContext(), score);
							loadFileList();
						} else if (choice.equals(getString(R.string.lic_info))) {
							String[] infoFile = DataStorage.getDownloadedFileinfo(getApplicationContext(), score.replace(".pdf", ""));
							boolean no_info = true;
							for (int i = 0; i< infoFile.length; i++){
								if (infoFile[i] == null)
									infoFile[i] = "";
								else
									no_info = false;
							}
							if (no_info)
								infoFile[0] = getString(R.string.no_info);
							infodialog_display = String.format("<h3>%s</h3><p><i>%s</i></p><p>%s</p>", infoFile[0], infoFile[1], infoFile[2]);
							showDialog(DIALOG_SCOREINFO);
						}
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
				return false;
			}
		});

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_SCOREINFO:
			Spanned marked_up = Html.fromHtml(infodialog_display);
			builder.setMessage(marked_up).setCancelable(false).setPositiveButton("ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dismissDialog(DIALOG_SCOREINFO);
				}
			});
			return builder.create();
		case DIALOG_NOFILE:
			builder.setMessage(getString(R.string.no_file)).setCancelable(false).setPositiveButton("ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dismissDialog(DIALOG_NOFILE);
				}
			});
			return builder.create();

		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		AlertDialog d = (AlertDialog) dialog;
		switch (id) {
		case DIALOG_SCOREINFO:
			Spanned marked_up = Html.fromHtml(infodialog_display);
			d.setMessage(marked_up);
			break;
		case DIALOG_NOFILE:
			d.setMessage(getString(R.string.no_file));
			break;
		default:
			break;
		}
	}

	protected void onResume() {
		super.onResume();
		loadFileList();
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_downloaded_activity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_downloaded_deleteall:
			DataStorage.deleteAllDownloadedFiles(getApplicationContext());
			Log.i("downloadedactivity", "deleting all downloaded files");
			loadFileList();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
