package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.database.Cursor;
import android.os.AsyncTask;

class DownloadManagerCancelPendingTask extends AsyncTask<Void, Void, Void> {

    private final DownloadManagerReleaseDownloader mDownloader;

    private final long mDownloadId;

    DownloadManagerCancelPendingTask(DownloadManagerReleaseDownloader downloader, long downloadId) {
        mDownloader = downloader;
        mDownloadId = downloadId;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (isPending()) {
            mDownloader.clearDownloadId(mDownloadId);
            mDownloader.onDownloadError(new IllegalStateException("Failed to start downloading file due to timeout exception."));
        }
        return null;
    }

    private boolean isPending() {
        DownloadManager.Query query = new DownloadManager.Query()
                .setFilterById(mDownloadId)
                .setFilterByStatus(DownloadManager.STATUS_PENDING);
        try (Cursor cursor = mDownloader.getDownloadManager().query(query)) {
            return cursor != null && cursor.moveToFirst();
        }
    }
}
