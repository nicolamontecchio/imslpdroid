package com.imslpdroid.gui;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.imslpdroid.R;

public class IntentUtils {
	
	public static boolean isPdfReaderAvailable(Context context) {
		PackageManager packageManager = context.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setType("application/pdf");
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	public static void noPdfReaderDialog(final Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(R.string.nopdfviewer).setCancelable(false).setNegativeButton("continue anyway", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		}).setPositiveButton("install Adobe Reader", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent goToMarket = null;
				goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.adobe.reader"));
				context.startActivity(goToMarket);
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

}
