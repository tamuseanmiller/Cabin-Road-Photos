package com.rebeccamcfadden.cabinroadphotos;

import android.util.Log;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpDownloadUtility {
    private static final int BUFFER_SIZE = 4096;
    public static void downloadFile(String fileURL, String saveDir, String mediaItemId)
            throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();
 
        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();
 
            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }
 
            Log.d("httpDownloader", "Content-Type = " + contentType);
            Log.d("httpDownloader", "Content-Disposition = " + disposition);
            Log.d("httpDownloader", "Content-Length = " + contentLength);
            Log.d("httpDownloader", "fileName = " + fileName);
 
            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();
            String saveFilePath = saveDir + File.separator + fileName;
            String fileExt = FilenameUtils.getExtension(fileName);
             
            // opens an output stream to save into file
            FileOutputStream outputStream = new FileOutputStream(saveFilePath);
 
            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
 
            outputStream.close();
            inputStream.close();

            File savedFile = new File(saveFilePath);
            savedFile.renameTo(new File(saveDir + File.separator + mediaItemId + "." + fileExt));
 
            Log.d("httpDownloader", "File downloaded");
        } else {
            Log.d("httpDownloader", "No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }
}