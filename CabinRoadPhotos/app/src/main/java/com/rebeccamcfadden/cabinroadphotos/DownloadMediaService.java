package com.rebeccamcfadden.cabinroadphotos;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

public class DownloadMediaService extends JobIntentService {
    private static final String DOWNLOAD_PATH = "com.rebeccamcfadden.cabinroadphotos_DOWNLOAD_PATH";
    private static final String DESTINATION_PATH = "com.rebeccamcfadden.cabinroadphotos_DESTINATION_PATH";
    private static final String MIMETYPE = "com.rebeccamcfadden.cabinroadphotos_MIMETYPE";
    public DownloadMediaService() {
        super();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String downloadPath = intent.getStringExtra(DOWNLOAD_PATH);
        String destinationPath = intent.getStringExtra(DESTINATION_PATH);
        String mimeType = intent.getStringExtra(MIMETYPE);
        startDownload(downloadPath, destinationPath, mimeType);
    }

    public static Intent getDownloadService(final @NonNull Context callingClassContext, final @NonNull String downloadPath, final @NonNull String destinationPath, final @NonNull String mimeType) {
        return new Intent(callingClassContext, DownloadMediaService.class)
                .putExtra(DOWNLOAD_PATH, downloadPath)
                .putExtra(DESTINATION_PATH, destinationPath)
                .putExtra(MIMETYPE, mimeType);
    }
    
    private void startDownload(String downloadPath, String destinationPath, String mimeType) {
        Log.d("download", "Downloading a file to " + destinationPath);
        Uri uri = Uri.parse(downloadPath); // Path where you want to download file
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);  // Tell on which network you want to download file.
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);  // This will show notification on top when downloading the file.
        request.setTitle("Downloading a file"); // Title for notification.
        request.setMimeType(mimeType);
        request.setVisibleInDownloadsUi(true);
        request.setDestinationInExternalPublicDir(destinationPath, uri.getLastPathSegment());  // Storage directory path
        ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request); // This will start downloading
    }
}