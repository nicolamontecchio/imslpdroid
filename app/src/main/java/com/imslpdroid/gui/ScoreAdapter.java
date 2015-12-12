package com.imslpdroid.gui;

import java.util.List;

import org.jsoup.Jsoup;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.imslpdroid.R;
import com.imslpdroid.data.Score;

public class ScoreAdapter extends ArrayAdapter<Score> {

	private List<Score> items;

	public ScoreAdapter(Context context, int textViewResourceId,
			List<Score> items) {
		super(context, textViewResourceId, items);
		this.items = items;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;

		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.row, null);
		}
		
		Score o = items.get(position);
		if (o != null) {
			TextView upText = (TextView) v.findViewById(R.id.toptext);
			TextView bottomText = (TextView) v.findViewById(R.id.bottomtext);
			ImageView iconSx = (ImageView) v.findViewById(R.id.score_listitem_icon);
			if (o.isSeparator()) {
				String tab = "";
				for(int i = 3; i < o.getSeparatorLevel(); i++)
					tab += "&nbsp;";
				upText.setText(Html.fromHtml(String.format("<h"+o.getSeparatorLevel()+">%s</h"+o.getSeparatorLevel()+"> ", tab + o.getAuthor().trim())));
				bottomText.setText("");
				iconSx.setVisibility(View.GONE);				
				bottomText.setVisibility(View.GONE);
				
			} else {
				upText.setText(Html.fromHtml(String.format(
						"<h3>%s</h3> - <i>%s<i>", o.getTitle().trim(), o
								.getPagesAndCo())));
				bottomText.setText(Jsoup.parse(o.getPublisherInfo()).text());
				if (o.isBlocked())
					iconSx.setImageResource(R.drawable.score_blocked);
				else if (o.isDownloaded())
					iconSx.setImageResource(R.drawable.score_downloaded);
				else
					iconSx.setImageResource(R.drawable.score_downloadable);
				
				iconSx.setVisibility(View.VISIBLE);
				bottomText.setVisibility(View.VISIBLE);
//				v.setBackgroundDrawable(null);
			}
		}

		return v;
	}
}