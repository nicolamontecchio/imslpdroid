package com.imslpdroid;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.imslpdroid.gui.StorableRestrictableListView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class NationalityActivity extends StorableRestrictableListView {

	private String baseUrl = "http://imslp.org/wiki/Category:People_by_nationality";

	@Override
	public List<String> downloadList(Handler handler, AtomicBoolean stopFlag) throws IOException {
		String url = baseUrl;
		List<String> res = new LinkedList<String>();
		Document doc = Jsoup.connect(url).get();
		Element tables = doc.getElementById("mw-subcategories");
		if (tables != null) {
			Elements contents = tables.getElementsByTag("a");
			for (Element content : contents) {
				String str = content.attr("title");
				res.add(str.substring(9, str.length() - 7)); // cut "people"
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
				String nationality = (String) ((TextView) view).getText() + " people";
				Intent newIntent = new Intent();
				Bundle bun = new Bundle();
				bun.putString("group", nationality);
				//				bun.putInt("type", )

				newIntent.setClass(parent.getContext(), RestrictedComposersActivity.class);
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