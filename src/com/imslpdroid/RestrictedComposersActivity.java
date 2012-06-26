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

/**
 * Used by nationality and age. Arguments (db table name and pk) must
 * be passed in from outside.
 */
public class RestrictedComposersActivity extends StorableRestrictableListView {

	private String baseUrl = null;
	private String group;

	@Override
	public List<String> downloadList(Handler handler, AtomicBoolean stopFlag) throws IOException {
		String url = baseUrl;
		List<String> res = new LinkedList<String>();
		while (url != null && !stopFlag.get()) {
			Document doc = Jsoup.connect(url).get();
			// titles: all the <a> below <div id=mw-pages">
			Element content = doc.getElementById("mw-subcategories");
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
				//				int total = Integer.parseInt(matcher.group(1).replace(",", ""));
			} catch (Exception e) {
				Log.e("getComposerList()", e.toString());
			}
		}
		stopFlag.set(false);
		return res;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Bundle bun = getIntent().getExtras();
		group = bun.getString("group");
		baseUrl = "http://imslp.org/wiki/Category:" + URLEncoder.encode(group).replace("+", "_");
		super.onCreate(savedInstanceState);
		setTitle(group);

		// click on list item
		ListView lv = (ListView) findViewById(R.id.rlv_listview);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String composer = (String) ((TextView) view).getText();
				Intent newIntent = new Intent();
				Bundle bun = new Bundle();
				bun.putString("composer", composer);
				bun.putString("prevPage", "RestrictedComposers");
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

}
