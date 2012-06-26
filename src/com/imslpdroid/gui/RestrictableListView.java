package com.imslpdroid.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.imslpdroid.R;

/**
 * List view which shows only elements with a specific substring match.
 * It is useful when extended by other activities that display long lists.
 */
public class RestrictableListView extends Activity {

	private class ListItemComparator implements Comparator<Pattern> {
		private String pattern;

		public ListItemComparator(String p) {
			pattern = (p != null ? p : null);
		}

		@Override
		public int compare(Pattern a, Pattern b) {
			if (pattern != null) {
				if (a.PatternModified.startsWith(pattern) && !b.PatternModified.startsWith(pattern))
					return -1;
				if (b.PatternModified.startsWith(pattern) && !a.PatternModified.startsWith(pattern))
					return 1;
			}
			return a.PatternModified.compareTo(b.PatternModified);
		}
	}

	private class Pattern {
		private String Pattern;
		private String PatternModified;

		public Pattern(String pattern) {
			Pattern = pattern;
			PatternModified = patterMod(pattern);
		}

		public String getPattern() {
			return Pattern;
		}

		public String getPatternModified() {
			return PatternModified;
		}

	}

	private static Map<Integer, String> accentsTable = null;

	public static String noAccents(String word) {
		StringBuilder sb = new StringBuilder(word.length() * 2);
		char[] charS = word.toCharArray();
		for (int i = 0; i < charS.length; i++) {
			if (charS[i] > 127) {
				String ss = accentsTable.get((int) charS[i]);
				if (ss == null) {
					sb.append(charS[i]);
				} else {
					sb.append(ss);
				}
			} else {
				sb.append(charS[i]);
			}
		}

		return sb.toString();
	}

	private List<Pattern> completeList = new LinkedList<Pattern>();

	private String composerNameToStrip = null;
	private String matchPattern = null;
	private boolean sortingDisabled = false;

	private void createAccentsTable() {
		accentsTable = new HashMap<Integer, String>(210); // for now, 193 cases
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("accents.csv")));
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0) {
					Integer k = Integer.parseInt(line.substring(0, line.indexOf(",")));
					String v = line.substring(line.indexOf(",") + 1);
					accentsTable.put(k, v);
					line = br.readLine();
				}
			}
		} catch (IOException e) {
			Log.e("restrictablelistview", "this should NEVER EVER happen");
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (accentsTable == null)
			createAccentsTable();
		setContentView(R.layout.restrictablelistview);
		EditText patternTextBox = (EditText) findViewById(R.id.patternEditText);
		patternTextBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				setMatchingPattern(s.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		LinearLayout layout = (LinearLayout) findViewById(R.id.rlv_adspacelayout);
		layout.removeAllViews();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	public String patterMod(String pattern) {
		if (pattern == null)
			return null;
		return noAccents(pattern).toLowerCase().trim();
	}

	private void renderList() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				List<Pattern> restrictedList = new LinkedList<Pattern>();
				for (Pattern listElement : completeList) {
					if (matchPattern == null)
						restrictedList.add(listElement);
					else {
						if (listElement.getPatternModified().contains(matchPattern)) {
							restrictedList.add(listElement);
						}
					}
				}
				if (!sortingDisabled)
					Collections.sort(restrictedList, new ListItemComparator(matchPattern));
				ListView lv = (ListView) findViewById(R.id.rlv_listview);
				List<String> restrictedListStr = toArrayString(restrictedList);
				lv.setAdapter(new ArrayAdapter<String>(lv.getContext(), android.R.layout.simple_list_item_1, restrictedListStr));
				lv.setTextFilterEnabled(true);
				TextView tw = (TextView) findViewById(R.id.rlv_totalcountlabel);
				tw.setText(String.format("total: %d", restrictedListStr.size()));
			}
		});
	}

	public void setComposerNameToStrip(String composerName) {
		this.composerNameToStrip = composerName;
	}

	public void setListViewElements(List<String> data) {
		completeList = toArrayPattern(data);
		renderList();
	}

	/** set match pattern */
	public void setMatchingPattern(String match) {
		if (match.trim().length() == 0)
			match = null;
		this.matchPattern = patterMod(match);
		renderList();
	}

	public List<Pattern> toArrayPattern(List<String> listSt) {
		List<Pattern> res = new LinkedList<Pattern>();
		for (String element : listSt)
			res.add(new Pattern(element));
		return res;
	}

	public List<String> toArrayString(List<Pattern> listPt) {
		List<String> res = new LinkedList<String>();
		for (Pattern element : listPt) {
			if (composerNameToStrip != null) { // Eliminate Composer name from text of the piece
				if (element.getPattern().contains(composerNameToStrip)
						&& element.getPattern().lastIndexOf(composerNameToStrip) == element.getPattern().length() - composerNameToStrip.length()) {
					String piece = element.getPattern().substring(0, element.getPattern().lastIndexOf(composerNameToStrip));
					if (res.isEmpty())
						res.add(piece);
					else if (!res.get(res.size() - 1).equals(piece))
						res.add(piece);
				} else {
					if (res.isEmpty())
						res.add(element.getPattern());
					else if (!res.get(res.size() - 1).equals(element.getPattern()))
						res.add(element.getPattern());
				}
			} else {
				res.add(element.getPattern());
			}
		}
		return res;
	}

	public void setSortingDisabled(boolean sortingDisabled) {
		this.sortingDisabled = sortingDisabled;
	}

}