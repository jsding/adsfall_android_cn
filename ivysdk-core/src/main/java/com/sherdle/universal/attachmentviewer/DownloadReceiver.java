package com.sherdle.universal.attachmentviewer;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */
public class DownloadReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    try {
      long receivedID = intent.getLongExtra(
        DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
      DownloadManager mgr = (DownloadManager)
        context.getSystemService(Context.DOWNLOAD_SERVICE);

      DownloadManager.Query query = new DownloadManager.Query();
      query.setFilterById(receivedID);
      try (Cursor cur = mgr.query(query)) {
        int index = cur.getColumnIndex(
          DownloadManager.COLUMN_STATUS);
        if (cur.moveToFirst()) {
          if (cur.getInt(index) ==
            DownloadManager.STATUS_SUCCESSFUL) {
            // do something
          }
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
