package com.imslpdroid.data;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class DataStorage {

	private static Lock dbLock = new ReentrantLock();

	private static final String DIVISOR = "---";

	private static final String EXTERNAL_DOWNLOAD_PATH = "imslpdroid_data";

	public static File getDownloadedScoreFile(Score s) {
		return new File(getExternalDownloadPath(), s.getScoreId() + ".pdf");
	}

	public static File getExternalDownloadPath() {
		return new File(Environment.getExternalStorageDirectory(), EXTERNAL_DOWNLOAD_PATH);
	}

	public static List<String> getListOfFilesInDownloadDirectory() {
		String[] files = getExternalDownloadPath().list();
		return new LinkedList<String>(files != null ? Arrays.asList(files) : new LinkedList<String>());
	}

	/** write to DB a new file */
	public static void addDownloadedFileInDB(Context context, Score score) {
		try {
			dbLock.lock();
			DataStorageHelper sdh = new DataStorageHelper(context);
			SQLiteDatabase db = sdh.getWritableDatabase();
			ContentValues m = new ContentValues();
			m.put("filename", score.getScoreId() + ".pdf");
			m.put("info", score.getVisualizationString() + DIVISOR + score.getTitle() + DIVISOR + score.getPagesAndCo() + DIVISOR + score.getPublisherInfo());
			db.insert("fileDownloaded", null, m);
			db.close();
		} catch (Exception e) {
			Log.e("datastorage", "exception while addDownloadedFileInDB: " + e.toString());
		} finally {
			dbLock.unlock();
		}
	}

	/** return filenames */
	public static List<String> getDownloadedFilesName(Context context) {
		List<String> files = new LinkedList<String>();
		try {
			dbLock.lock();
			DataStorageHelper sdh = new DataStorageHelper(context);
			SQLiteDatabase db = sdh.getReadableDatabase();
			Cursor cursor = db.query("fileDownloaded", new String[] { "info" }, null, null, null, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				String info = cursor.getString(0);
				String name = info;
				if (info.contains(DIVISOR))
					name = info.split(DIVISOR)[0];
				files.add(name);
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
		} catch (Exception e) {
			Log.e("datastorage", "exception while getDownloadedFilesName: " + e.toString());
		} finally {
			dbLock.unlock();
		}
		return files;
	}

	/** return fileinfo by ID */
	public static String[] getDownloadedFileinfo(Context context, String ID) {
		String[] infoFile = new String[3];
		try {
			dbLock.lock();
			DataStorageHelper sdh = new DataStorageHelper(context);
			SQLiteDatabase db = sdh.getReadableDatabase();
			Cursor cursor = db.query("fileDownloaded", new String[] { "info" }, "filename='" + ID + ".pdf'", null, null, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				String info = cursor.getString(0);
				if (info.contains(ID) && info.contains(DIVISOR)) {
					infoFile[0] = info.split(DIVISOR)[1];
					infoFile[1] = info.split(DIVISOR)[2];
					infoFile[2] = info.split(DIVISOR)[3];
					break;
				}
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
		} catch (Exception e) {
			Log.e("datastorage", "exception while getDownloadedFilesName: " + e.toString());
		} finally {
			dbLock.unlock();
		}
		return infoFile;
	}

	/** synchronize db with the download directory */
	public static void syncronizeDownloadedFileTable(Context context) {
		try {
			dbLock.lock();
			DataStorageHelper sdh = new DataStorageHelper(context);
			SQLiteDatabase db = sdh.getWritableDatabase();
			List<String> filesInDB = new LinkedList<String>();
			Cursor cursor = db.query("fileDownloaded", new String[] { "filename" }, null, null, null, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				filesInDB.add(cursor.getString(0));
				cursor.moveToNext();
			}
			cursor.close();
			List<String> filesInDir = getListOfFilesInDownloadDirectory();
			//delete files that are in the DB but aren't in the Directory
			if (!filesInDB.isEmpty()) {
				for (String dbfile : filesInDB) {
					if (!filesInDir.contains(dbfile))
						db.delete("fileDownloaded", "filename=?", new String[] { dbfile });
				}
			}
			//delete files that are in the Directory but aren't in the DB
			if (!filesInDir.isEmpty()) {
				for (String dirfile : filesInDir) {
					if (!filesInDB.contains(dirfile)) {
						File toDelete = new File(getExternalDownloadPath(), dirfile);
						toDelete.delete();
					}
				}
			}
			db.close();
		} catch (Exception e) {
			Log.e("datastorage", "exception while syncronizeDownloadedFileTable: " + e.toString());
		} finally {
			dbLock.unlock();
		}
	}

	public static void deleteDownloadedFile(Context context, Score score) {
		DataStorage.getDownloadedScoreFile(score).delete();
		syncronizeDownloadedFileTable(context);
	}

	public static void deleteDownloadedFile(Context context, String score) {
		File toDelete = new File(getExternalDownloadPath(), score);
		toDelete.delete();
		syncronizeDownloadedFileTable(context);
	}

	public static void deleteAllDownloadedFiles(Context context) {
		List<String> filesInDir = getListOfFilesInDownloadDirectory();
		// delete all files that are in the Directory
		for (String dirfile : filesInDir)
			deleteDownloadedFile(context, dirfile);
		try {
			dbLock.lock();
			DataStorageHelper sdh = new DataStorageHelper(context);
			SQLiteDatabase dbw = sdh.getWritableDatabase();
			dbw.delete("fileDownloaded", null, null);
			dbw.close();
		} catch (Exception e) {
			Log.e("datastorage", "exception while syncronizeDownloadedFileTable: " + e.toString());
		} finally {
			dbLock.unlock();
		}
	}

	public static List<String> readGenericListFromDB(Context context, String baseUrl) {
		List<String> list = new LinkedList<String>();
		try {
			dbLock.lock();
			SQLiteDatabase db = new DataStorageHelper(context).getWritableDatabase();
			Cursor cursor = db.query("genericlist", new String[] { "entry" }, String.format("pageurl='%s'", baseUrl), null, null, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				list.add(cursor.getString(0));
				cursor.moveToNext();
			}
			cursor.close();
			db.close();
		} catch (Exception e) {
			Log.e("datastorage", "exception while readGenericListFromDB: " + e.toString());
		} finally {
			dbLock.unlock();
		}
		return list;
	}
	
	public static void writeGenericListToDB(Context context, String baseUrl, List<String> entries) {
		try {
			dbLock.lock();
			SQLiteDatabase db = new DataStorageHelper(context).getWritableDatabase();
			db.delete("genericlist", String.format("pageurl='%s'", baseUrl), null);
			db.beginTransaction();
			for (String r : entries) {
				ContentValues m = new ContentValues();
				m.put("pageurl", baseUrl);
				m.put("entry", r);
				db.insert("genericlist", null, m);
			}
			db.setTransactionSuccessful();
			db.endTransaction();
			db.close();
		} catch (Exception e) {
			Log.e("datastorage", "exception while writeGenericListToDB: " + e.toString());
		} finally {
			dbLock.unlock();
		}

	}

}
