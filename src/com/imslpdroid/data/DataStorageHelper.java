package com.imslpdroid.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataStorageHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "IMSLPDROID_DB";
	private static int DB_VERSION = 5;

	public DataStorageHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table fileDownloaded(filename text, info text)");
		db.execSQL("create table genericlist(pageurl text, entry text)"); // from 4
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i("datastoragehelper", String.format("onupgrade called with oldversion = %d and newversion =%d", oldVersion, newVersion));
		if (oldVersion < 1) { // just for history reference
			db.execSQL("create table composers(composername text primary key)"); 
		}
		if (oldVersion < 3) {
			db.execSQL("create table timeperiod(period text primary key)");
			db.execSQL("create table nationality(nation text primary key)");
			db.execSQL("create table worktypes(worktype text primary key)");
		}
		if (oldVersion < 4) {
			db.execSQL("create table genericlist(pageurl text, entry text)");
		}
		if (oldVersion < 5) {
			db.execSQL("drop table composers");
			db.execSQL("drop table timeperiod");
			db.execSQL("drop table instrumentations");
			db.execSQL("drop table nationality");
			db.execSQL("drop table worktypes");
		}

	}

}
