//package com.rebeccamcfadden.cabinroadphotos;
//
//import android.content.Context;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.util.Log;
//import android.view.View;
//import android.webkit.URLUtil;
//import android.widget.MediaController;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.widget.VideoView;
//
//import java.io.File;
//
//public class VideoPlayer {
//    private final VideoView mVideoView;
//    private static final String VIDEO_SAMPLE =
//            "https://developers.google.com/training/images/tacoma_narrows.mp4";
//    private MediaController controller;
//    private TextView mBufferingTextView;
//    private int mCurrentPosition;
//    private File filesDir;
//    private final Context mContext;
//
//    public VideoPlayer(View view, Context context) {
//        this.mContext = context;
//        this.mVideoView = (VideoView) view.findViewById(R.id.videoview);
//        this.controller = new MediaController(context);
//        this.filesDir = context.getFilesDir();
//
//        this.mBufferingTextView = view.findViewById(R.id.buffering_textview);
//    }
//
//    void initializePlayer(String url) {
//        if (getMedia(url)) {
//            controller.setMediaPlayer(mVideoView);
//            mVideoView.setMediaController(controller);
//            mVideoView.setOnPreparedListener(
//                    new MediaPlayer.OnPreparedListener() {
//                        @Override
//                        public void onPrepared(MediaPlayer mediaPlayer) {
//
//                            // Hide buffering message.
//                            mBufferingTextView.setVisibility(VideoView.INVISIBLE);
//                            mVideoView.requestFocus();
//                            mediaPlayer.start();
//                        }
//                    });
//            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mediaPlayer) {
//                    Toast.makeText(mContext, "Playback completed",
//                            Toast.LENGTH_SHORT).show();
//                    releasePlayer();
//                }
//            });
//            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
//                @Override
//                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
//                    Log.e("mediaPlayer", String.valueOf(i) + ", " + String.valueOf(i1));
//                    return false;
//                }
//            });
//            mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
//                @Override
//                public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
//                    Log.e("mediaPlayer", String.valueOf(i) + ", " + String.valueOf(i1));
//                    return false;
//                }
//            });
//            mVideoView.start();
//        }
//
//    }
//
//    void releasePlayer() {
//        mBufferingTextView.setVisibility(VideoView.INVISIBLE);
//        mVideoView.stopPlayback();
//    }
//
//    private boolean getMedia(String mediaName) {
//        if (URLUtil.isValidUrl(mediaName)) {
//            // media name is an external URL
//            mVideoView.setVideoPath(mediaName);
//            return true;
//        } else { // media name is a raw resource embedded in the app
//            String pathname = filesDir.getAbsolutePath() + "/" + mediaName;
//            File file = new File(pathname);
//            if (file.exists()) {
//                mVideoView.setVideoURI(Uri.parse(pathname));
//                return true;
//            }
//        }
//        return false;
//    }
//}
