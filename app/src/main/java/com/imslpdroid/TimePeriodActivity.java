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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class TimePeriodActivity extends StorableRestrictableListView {

	private static final String baseUrl = "http://imslp.org/wiki/Browse_people_by_time_period";
	public static final String PAGENAME = "timeperiod";
	public static final String PRIMARYKEY = "period";

	@Override
	public String getBaseUrl() {
		return baseUrl;
	}

	@Override
	public List<String> downloadList(Handler handler, AtomicBoolean stopFlag) throws IOException {
		String url = baseUrl;
		List<String> res = new LinkedList<String>();
		Document doc = Jsoup.connect(url).get();
		Element content = doc.getElementById("bodyContent");
		if (content != null) {
			Elements links = content.getElementsByTag("a");
			for (Element link : links) {
				String str = link.attr("title");
				if (str.length() > 25) {
					str = str.substring(25); // cut "Category:People from the "
					String years = link.parents().parents().parents().get(1).text().replace(link.parents().parents().parents().get(0).text(), "").trim();
					str = str + ": " + years;
					res.add(str);
				}
			}
		}
		return res;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setSortingDisabled(true);
		// click on list item
		ListView lv = (ListView) findViewById(R.id.rlv_listview);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String timeperiod = (String) ((TextView) view).getText();
				timeperiod = "People from the " + timeperiod.split(":")[0].trim();
				Intent newIntent = new Intent();
				Bundle bun = new Bundle();
				bun.putString("group", timeperiod);
				newIntent.setClass(parent.getContext(), RestrictedComposersActivity.class);
				newIntent.putExtras(bun);
				startActivity(newIntent);
			}
		});
	}
}
