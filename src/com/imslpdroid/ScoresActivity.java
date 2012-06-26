package com.imslpdroid;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.imslpdroid.data.DataStorage;
import com.imslpdroid.data.Score;
import com.imslpdroid.gui.ScoreAdapter;

public class ScoresActivity extends ListActivity {

	private static final int DIALOG_PROGRESS = 2;

	private String piece = null;
	private AtomicBoolean stopFlag = new AtomicBoolean(false);
	private ProgressDialog progressDialog = null;
	private GetListOfScoresTask task = null;
	private static String infodialog_display = null;
	private static Map<String, List<Score>> scoreCache = new HashMap<String, List<Score>>();

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			progressDialog = new ProgressDialog(ScoresActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setCancelable(true);
			progressDialog.setMessage("");
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					stopFlag.set(true);
					if (task != null
							&& task.getStatus() == AsyncTask.Status.RUNNING)
						task.cancel(true);
				}
			});
			return progressDialog;
		case DIALOG_SCOREINFO:
			if (infodialog_display != null) {
				Spanned marked_up = Html.fromHtml(infodialog_display);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(marked_up).setCancelable(false)
						.setPositiveButton("ok",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dismissDialog(DIALOG_SCOREINFO);
									}
								});
				AlertDialog alert = builder.create();
				return alert;
			}
		default:
			return super.onCreateDialog(id);
		}
	}

	private class GetListOfScoresTask extends
			AsyncTask<String, String, List<Score>> {

		private boolean addScores(String author, String piece, Element content,
				Elements weFiles, List<Score> res, boolean separator,
				int separatorLevel) {
			boolean hasScore = false;
			if (separator) {
				Score newScore = new Score("", author, "", "", "", "", false,
						separatorLevel);
				res.add(newScore);
			} else {

				for (Element weFile : weFiles) {
					String scoreId = weFile.getElementsByTag("a").get(0).attr(
							"title").toString();
					boolean blocked = !weFile.getElementsByClass(
							"we_file_dlarrow_blocked").isEmpty();
					String publisherInfo = content.getElementsByClass(
							"we_edition_info").get(0).text();
					String scannedBy = weFile
							.getElementsByClass("we_file_info").get(0).text();
					String pagesandco = weFile.getElementsByClass(
							"we_file_info2").get(0).text();
					String title = weFile.getElementsByClass(
							"we_file_dlarrwrap").parents().get(0).text();
					if (scannedBy.contains("PDF file")) {
						Score newScore = new Score(scoreId, author, piece,
								publisherInfo, title, pagesandco, blocked, -1);
						res.add(newScore);
						hasScore = true;
					}
				}
			}
			return hasScore;
		}

		private List<Score> getScoresForPiece() throws IOException {
			String url = "http://imslp.org/wiki/"
					+ URLEncoder.encode(piece).replace("+", "_");
			List<Score> res = new LinkedList<Score>();
			String[] split = piece.split("\\(");
			String author = split[split.length - 1];
			author = author.substring(0, author.length() - 1).trim();
			String pieceName = split[0].trim();
			while (url != null && !stopFlag.get()) {
				Document doc = Jsoup.connect(url).get();
				Elements tabs = doc.getElementsByClass("tabs");
				Elements allElements = null;
				Elements contents = null;
				if (tabs.size() > 0) {
					Element tab = null;
					int i = 1, level = 0;
					boolean scoreFound = false;
					boolean atLeastOneTab = false;
					boolean stop = false;
					Iterator<Element> itTab = tabs.iterator();
					Element tmpTab = null;
					
					do {
						if (itTab.hasNext() && !scoreFound) {
							tmpTab = itTab.next();
							do {
								tab = tmpTab.getElementById("tab" + i);
								if (tab != null) {
									atLeastOneTab = true;
									allElements = tab.children();
									for (Element el : allElements) {
										if (!el.getElementsByClass(
												"mw-headline").isEmpty()) {
											level = Integer.parseInt(el
													.nodeName().substring(1));
											addScores(el.text(), "", null,
													null, res, true, level);

										} else if (el.hasClass("we")) {
											contents = el
													.getElementsByClass("we");
											for (Element content : contents) {
												Elements weFilesFirst = content
														.getElementsByClass("we_file_first");
												boolean hasScore = addScores(
														author, pieceName,
														content, weFilesFirst,
														res, false, -1);

												Elements weFiles = content
														.getElementsByClass("we_file");
												addScores(author, pieceName,
														content, weFiles, res,
														false, -1);
												if (hasScore) {
													scoreFound = true;
												}
											}
										} else if (scoreFound && el.toString().contains("href")) { // redirect
											addScores(el.text(), "", null,
													null, res, true, level+1);
										}
									}
								}
								i++;
							} while (tab != null || !atLeastOneTab);
							if (!scoreFound) {
								atLeastOneTab = false;
								i = 1;
								res.clear();
							}
						} else
							stop = true;
					} while (!stop);
					url = null;

				} else {
					contents = doc.getElementsByClass("we");
					if (contents != null) {
						for (Element content : contents) {
							Elements weFilesFirst = content
									.getElementsByClass("we_file_first");
							addScores(author, pieceName, content, weFilesFirst,
									res, false, -1);
							Elements weFiles = content
									.getElementsByClass("we_file");
							addScores(author, pieceName, content, weFiles, res,
									false, -1);
						}
						url = null;
						Elements allAnchors = doc.getElementsByTag("a");
						for (Element anchor : allAnchors)
							if (anchor.hasText()
									&& anchor.text().contains("next 200")) {
								url = "http://imslp.org/" + anchor.attr("href");
								break;
							}
					}
				}
			}
			return res;
		}

		@Override
		protected List<Score> doInBackground(String... params) {
			try {
				if (!scoreCache.containsKey(piece)) {
					publishProgress(getString(R.string.downloadingfromimslp));
					List<Score> list = getScoresForPiece();
					scoreCache.put(piece, list);
					return list;
				} else {
					Log.i("scoresactivity", "cached scores");
					return scoreCache.get(piece);
				}
			} catch (IOException e) {
				publishProgress("___TOAST___"
						+ getString(R.string.errordownloadingscorelist)
								.substring(11));
				Log.e("scoresactivity", "doInBackgound --- " + e.toString());
				return new LinkedList<Score>();
			}
		}

		@Override
		protected void onCancelled() {
			stopFlag.set(true);
			if (progressDialog != null && progressDialog.isShowing())
				dismissDialog(DIALOG_PROGRESS);
			finish();
		}

		@Override
		protected void onPreExecute() {
			task = this;
		}

		@Override
		protected void onPostExecute(List<Score> result) {
			if (progressDialog != null && progressDialog.isShowing())
				dismissDialog(DIALOG_PROGRESS);
			setListAdapter(new ScoreAdapter(getApplicationContext(),
					R.layout.row, result));
		}

		@Override
		protected void onProgressUpdate(String... values) {
			if (values[0].startsWith("___TOAST___")) {
				Toast.makeText(getApplicationContext(), values[0],
						Toast.LENGTH_LONG);
			} else {
				if (progressDialog == null)
					showDialog(DIALOG_PROGRESS);
				if (progressDialog.isShowing()) {
					progressDialog.setMessage(values[0]);
				}
			}
		}

	}

	private static final int DIALOG_SCOREINFO = 1;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final Score clickedScore = ((ScoreAdapter) l.getAdapter())
				.getItem(position);
		if (!clickedScore.isSeparator()) {
			if (clickedScore.isBlocked()) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.filerequestediscopyright),
						Toast.LENGTH_LONG).show();
			} else if (clickedScore.isDownloaded()) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(DataStorage
						.getDownloadedScoreFile(clickedScore)),
						"application/pdf");
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				try {
					startActivity(intent);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(getApplicationContext(),
							getString(R.string.nopdfapp), Toast.LENGTH_LONG);
				}
			} else {
				Intent intent = new Intent().setClass(this,
						ScoreDownloadActivity.class);
				intent.putExtra("score", clickedScore);
				startActivity(intent);
			}
		}
	}

	private boolean onListItemLongClick(AdapterView<?> l, View v, int position,
			long id) {
		final Score clickedScore = ((ScoreAdapter) l.getAdapter())
				.getItem(position);
		if (!clickedScore.isSeparator()) {
			List<String> itemlist = new LinkedList<String>();
			if (clickedScore.isDownloaded()) {
				itemlist.add(getString(R.string.lic_delete));
				itemlist.add(getString(R.string.lic_redownload));
			}
			if (clickedScore.getPublisherInfo() != null) {
				itemlist.add(getString(R.string.lic_info));
			}
			if (itemlist.size() > 0) {
				final CharSequence[] items = itemlist
						.toArray(new CharSequence[0]);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.lic_chooseaction));
				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						CharSequence choice = items[item];
						if (choice.equals(getString(R.string.lic_delete))) {
							Log.i("scoresactivity", "deleting file");
							DataStorage.deleteDownloadedFile(
									getApplicationContext(), clickedScore);
							setListAdapter(new ScoreAdapter(
									getApplicationContext(), R.layout.row,
									scoreCache.get(piece)));
						} else if (choice
								.equals(getString(R.string.lic_redownload))) {
							Intent intent = new Intent().setClass(
									getApplicationContext(),
									ScoreDownloadActivity.class);
							intent.putExtra("score", clickedScore);
							startActivity(intent);
						} else if (choice.equals(getString(R.string.lic_info))) {
							infodialog_display = String.format(
									"<h3>%s</h3><p><i>%s</i></p><p>%s</p>",
									clickedScore.getTitle(), clickedScore
											.getPagesAndCo(), clickedScore
											.getPublisherInfo());
							showDialog(DIALOG_SCOREINFO);
						}
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		}
		return true;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		switch (id) {
		case DIALOG_SCOREINFO:
			Spanned marked_up = Html.fromHtml(infodialog_display);
			AlertDialog d = (AlertDialog) dialog;
			d.setMessage(marked_up);
			break;
		default:
			break;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		piece = getIntent().getStringExtra("piece");
		super.onCreate(savedInstanceState);
		setTitle(piece);
		new GetListOfScoresTask().execute();
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				return onListItemLongClick(arg0, arg1, arg2, arg3);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (scoreCache.get(piece) != null)
			setListAdapter(new ScoreAdapter(getApplicationContext(),
					R.layout.row, scoreCache.get(piece)));
	}
}
