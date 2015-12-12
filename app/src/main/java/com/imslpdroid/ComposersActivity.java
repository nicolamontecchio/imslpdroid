package com.imslpdroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class ComposersActivity extends StorableRestrictableListView {

	private static final String baseUrl = "http://imslp.org/wiki/Category:Composers";

	@Override
	public List<String> downloadList(Handler h, AtomicBoolean stopFlag) throws IOException {
		h.sendMessage(StorableRestrictableListView.getNotificationMessage(getString(R.string.downloading_list_of_composers), null));
		String url = baseUrl;
		List<String> res = new LinkedList<String>();
		while (url != null && !stopFlag.get()) {
			Document doc = Jsoup.connect(url).timeout(5*1000).get();
			// titles: all the <a> below <div id=mw-pages">
			Element content = doc.getElementById("mw-subcategories");
			if (content != null) {
				Elements links = content.getElementsByTag("a");
				for (Element link : links)
					res.add(link.attr("title").substring(9));
				// examine all anchors for "next 200"
				url = null;
				Elements allAnchors = doc.getElementsByTag("a");
				for (Element anchor : allAnchors)
					if (anchor.hasText() && anchor.text().contains("next 200")) {
						url = "http://imslp.org/" + anchor.attr("href");
						break;
					}
				try {
					Pattern p = Pattern.compile("out of ([0-9\\,]*) total", Pattern.MULTILINE);
					String totalString = doc.getElementById("mw-subcategories").getElementsByTag("p").get(0).text();
					Matcher matcher = p.matcher(totalString);
					matcher.find();
					int total = Integer.parseInt(matcher.group(1).replace(",", ""));
					h.sendMessage(StorableRestrictableListView.getNotificationMessage(
							String.format("%s - %5.2f %%", getString(R.string.downloading_list_of_composers), Math.min(100., res.size() * 100. / total)), null));
				} catch (Exception e) {
					Log.e("getComposerList()", e.toString());
				}
			}
		}
		return res;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// click on list item
		ListView lv = (ListView) findViewById(R.id.rlv_listview);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String composer = (String) ((TextView) view).getText();
				Intent newIntent = new Intent();
				Bundle bun = new Bundle();
				bun.putString("composer", composer);
				bun.putString("prevPage", "ComposersActivity");
				newIntent.setClass(parent.getContext(), PiecesActivity.class);
				newIntent.putExtras(bun);
				startActivity(newIntent);
			}
		});
	}

	@Override
	public String getBaseUrl() {
		return baseUrl;
	}

	@Override
	public List<String> getPreCachedItems() {
		List<String> list = new LinkedList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("composers.txt")));
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0) {
					list.add(line);
					line = br.readLine();
				}
			}
		} catch (IOException e) {
			Log.e("composersactivity", "getPreCachedItems ...");
		}
		return list;
	}	
}
