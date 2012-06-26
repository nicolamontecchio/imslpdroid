package com.imslpdroid;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.imslpdroid.gui.StorableRestrictableListView;

public class WorkTypesActivity extends StorableRestrictableListView {

	String baseUrl = "http://imslp.org/wiki/IMSLP:View_Genres";

	@Override
	public List<String> downloadList(Handler handler, AtomicBoolean stopFlag) throws IOException {
		String url = baseUrl;
		List<String> res = new LinkedList<String>();
		Document doc = Jsoup.connect(url).get();
		Element tables = doc.getElementsByTag("table").get(0);
		if (tables != null) {
			Elements contents = tables.getElementsByClass("plainlinks");
			for (Element content : contents)
				res.add(content.text());
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
				String instrumentation = (String) ((TextView) view).getText();
				Intent newIntent = new Intent();
				Bundle bun = new Bundle();
				bun.putString("composer", instrumentation);
				bun.putString("prevPage", "InstrumentationActivity");
				newIntent.setClass(parent.getContext(), PiecesActivity.class);
				newIntent.putExtras(bun);
				startActivity(newIntent);
			}
		});
	}

	@Override
	public String getBaseUrl() {
		return baseUrl  + "workTypesActivity"; //same url as instrumentation
	}
}
