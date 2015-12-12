package com.imslpdroid;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.imslpdroid.gui.StorableRestrictableListView;

public class PiecesActivity extends StorableRestrictableListView {

	private String composerName;
	private static String prevPage = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		composerName = getIntent().getStringExtra("composer");
		prevPage = getIntent().getStringExtra("prevPage");
		super.onCreate(savedInstanceState);
		setTitle(composerName);
		if(!prevPage.equals("InstrumentationActivity"))
			setComposerNameToStrip("(" + composerName + ")");

		// click on list item
		ListView lv = (ListView) findViewById(R.id.rlv_listview);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				String tmpComposerName = "";
				if (!prevPage.equals("InstrumentationActivity"))
					tmpComposerName = "(" + composerName + ")";
				String piece = (String) ((TextView) view).getText();
				if (piece.lastIndexOf("*") != piece.trim().length() - 1) {
					// is not a transcription or arrangement
					piece += tmpComposerName;
				} else
					piece = piece.substring(0, piece.length() - 1);

				Intent newIntent = new Intent();
				newIntent.putExtra("piece", piece);
				newIntent.setClass(parent.getContext(), ScoresActivity.class);
				startActivity(newIntent);
			}
		});
	}

	@Override
	public String getBaseUrl() {
		return "http://imslp.org/index.php?title=Category:" + URLEncoder.encode(composerName).replace("+", "_")
		+ (prevPage != null && prevPage.equals("InstrumentationActivity") ? "&transclude=Template:Catintro" : "");
	}

	@Override
	public List<String> downloadList(Handler handler, AtomicBoolean stopFlag) throws IOException {
		List<String> list = new LinkedList<String>();
		boolean firstPage = true;
		String url = getBaseUrl();
		while (url != null && !stopFlag.get()) {
			Document doc = Jsoup.connect(url).get();
			int numMWPages = doc.html().split("\\<div id=\"mw-pages\"\\>").length; // number of tables
			if (firstPage && numMWPages > 2) { // more than one "mw-pages"
				firstPage = false;
				String[] html = doc.html().split("\\<div id=\"mw-pages\"\\>");
				String htmlStart = html[0];
				String[] splitEnd = html[html.length - 1].split("\\</div\\>");
				String htmlEnd = splitEnd[splitEnd.length - 1];
				html[html.length - 1] = splitEnd[0] + "\\</div\\>";
				for (int i = 1; i < numMWPages; i++) {
					String htmlPart = htmlStart + "\\<div id=\"mw-pages\"\\>" + html[i] + htmlEnd;
					Document newDoc = Jsoup.parse(htmlPart);
					Element content = newDoc.getElementById("mw-pages"); // titles
					Elements links = content.getElementsByTag("a");
					addPiece(list, links);
				}
			} else {
				Element content = doc.getElementById("mw-pages"); // titles
				if (content != null) {
					Elements links = content.getElementsByTag("a");
					addPiece(list, links);
				}
			}
			url = null;
			Elements allAnchors = doc.getElementsByTag("a"); // anchors for next 200
			for (Element anchor : allAnchors) {
				if (anchor.hasText() && anchor.text().contains("next 200")) {
					url = "http://imslp.org/" + anchor.attr("href");
					break;
				}
			}
			try {
				Pattern p = Pattern.compile("out of ([0-9\\,]*) total", Pattern.MULTILINE);
				String totalString = doc.getElementById("mw-pages").getElementsByTag("p").get(0).text();
				Matcher matcher = p.matcher(totalString);
				matcher.find();
				int total = Integer.parseInt(matcher.group(1).replace(",", ""));
				handler.sendMessage(getNotificationMessage(String.format("%s - %5.2f%%", "downloading pieces", Math.min(100,list.size() * 100. / total)), null));
			} catch (Exception e) {
				Log.e("getComposerList()", e.toString());
			}
		}
		return list;
	}
	
	void addPiece(List<String> list, Elements links){
		for (Element link : links) {
			String title = link.attr("title");
			if (title.contains("(" + composerName + ")") || prevPage.equals("InstrumentationActivity"))
				list.add(title);
			else
				list.add(title + "*");// transcription marker
		}
	}
}


