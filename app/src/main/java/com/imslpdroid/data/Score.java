package com.imslpdroid.data;

import java.io.Serializable;
import java.util.List;

public class Score implements Serializable {

	private static final long serialVersionUID = 1L;

	private String scoreId;
	private String author;
	private String piece;
	private String title;
	private String publisherInfo;
	private String pagesAndCo;
	private boolean blocked;
	private boolean separator;
	private int separatorLevel;

	public String getPublisherInfo() {
		return publisherInfo;
	}

	public String getPagesAndCo() {
		return pagesAndCo;
	}

	public String getTitle() {
		return title;
	}

	public String getScoreId() {
		return scoreId;
	}

	public String getAuthor() {
		return author;
	}

	public String getPiece() {
		return piece;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

	public boolean isBlocked() {
		return blocked;
	}
	
	public void setSeparator(boolean separator) {
		this.separator = separator;
	}

	public boolean isSeparator() {
		return separator;
	}

	public void setSeparatorLevel(int separatorLevel) {
		this.separatorLevel = separatorLevel;
	}

	public int getSeparatorLevel() {
		return separatorLevel;
	}

	public boolean isDownloaded() {
		boolean downloaded = false;
		if (DataStorage.getExternalDownloadPath().exists()) {
			List<String> files = DataStorage
					.getListOfFilesInDownloadDirectory();
			for (String file : files)
				if (file.contains(getScoreId()))
					downloaded = true;
		}
		return downloaded;
	}

	public String getVisualizationString() {
		return this.author + " - " + this.piece + " - " + this.title
				+ " - IMSLP" + this.scoreId;
	}

	public Score(String scoreId, String author, String piece,
			String publisherInfo, String title, String pagesAndCo,
			boolean blocked, int separatorLevel) {
		if (separatorLevel != -1){
			this.author = author;
			this.scoreId = "";
			this.separator = true;
			this.separatorLevel = separatorLevel;
		}
		else {
			try {
				this.scoreId = "" + Integer.parseInt(scoreId);
			} catch (NumberFormatException e) {
				String[] scoreIdSplitted = scoreId.split("/");
				this.scoreId = scoreIdSplitted[scoreIdSplitted.length - 1];
			}
			this.author = author;
			this.piece = piece;
			if (publisherInfo.contains("Amazon"))
				publisherInfo = publisherInfo.substring(0, publisherInfo
						.indexOf("Amazon"));
			if (publisherInfo.contains("Purchase Copy"))
				publisherInfo = publisherInfo.substring(0, publisherInfo
						.indexOf("Purchase Copy"));
			this.publisherInfo = publisherInfo;
			this.title = title;
			this.blocked = blocked;
			this.pagesAndCo = pagesAndCo.split(" - ")[1].trim();
			this.separator = false;
			this.separatorLevel = -1;
		}
	}
}
