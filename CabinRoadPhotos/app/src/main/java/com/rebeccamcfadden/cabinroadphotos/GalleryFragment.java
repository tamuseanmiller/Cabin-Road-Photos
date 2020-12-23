package com.rebeccamcfadden.cabinroadphotos;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.TextClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.collection.ArraySet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.api.client.googleapis.util.Utils;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.photos.types.proto.MediaItem;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.stfalcon.imageviewer.StfalconImageViewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.cabriole.decorator.ColumnProvider;
import io.cabriole.decorator.GridMarginDecoration;

import static com.rebeccamcfadden.cabinroadphotos.MainActivity.photosLibraryClient;

public class GalleryFragment extends Fragment implements RecyclerViewAdapterGallery.ItemClickListener {

    private static final int PICK_IMAGE = 1;
    private String albumID;
    private StfalconImageViewer stfalconImageViewer;
    private AtomicReference<List<String>> finalImages;
    private AtomicReference<List<MediaItem>> finalImagesRaw;
    private int autoplayDuration;
    private RecyclerView galleryRecycler;
    private RecyclerViewAdapterGallery galleryAdapter;
    private String albumTitle;
    private AppCompatActivity mContext;
    private boolean isWriteable;
    private AtomicReference<Iterable<MediaItem>> images;
    private SwipeRefreshLayout refreshGallery;

    private Timer slideshowTimer;

    private Toolbar actionbar;
    private File videoSaveDir;

    public GalleryFragment() {
        albumID = null;
    }

    public void setAlbumId(String albumId) {
        this.albumID = albumId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        autoplayDuration = new SharedPreferencesManager(getContext()).retrieveInt("autoplaySpeed", 20);
        Log.d("slideshow", "autoplay duration set to " + autoplayDuration + " seconds");

        this.videoSaveDir = MainActivity.videoSaveDir;

        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_gallery, container, false);

        // Set Action bar back button and colors
        actionbar = mContext.findViewById(R.id.toolbar_main);
        if (actionbar != null) {
            Log.d("debug", "action bar was non null");
            Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_arrow_back);
            drawable.setTint(ContextCompat.getColor(mContext, R.color.white));
            actionbar.setNavigationIcon(drawable);
            actionbar.setTitle(albumTitle);
            actionbar.setNavigationOnClickListener(v -> {
                actionbar.setNavigationIcon(null);
                mContext.onBackPressed();
            });
        }

        Thread thread = new Thread(() -> {
            refreshGallery = mView.findViewById(R.id.refresh_gallery);

            if (mContext != null)
                mContext.runOnUiThread(() -> refreshGallery.setRefreshing(true));

            // Get Media Items from Album and add to String List
            images = new AtomicReference<>(photosLibraryClient.searchMediaItems(albumID).iterateAll());
            ArrayList<Pair<String, String>> videos = new ArrayList<>();
            finalImages = new AtomicReference<>(new ArrayList<>());
            finalImagesRaw = new AtomicReference<>(new ArrayList<>());

            // Add upload button if album is writeable
            isWriteable = photosLibraryClient.getAlbum(albumID).getIsWriteable();
            if (isWriteable) {
                finalImages.get().add("ADDIMAGEPICTURE");
            }

            for (MediaItem i : images.get()) {
                if (i.getMediaMetadata().hasVideo()) {
                    Pair<String, String> video = new Pair<>(i.getBaseUrl() + "=dv", i.getId());
                    videos.add(video);
                }
                finalImagesRaw.get().add(i);
                finalImages.get().add(i.getBaseUrl());
            }

            fetchVideos(videos);

            // Initialize RecyclerView
            if (mContext != null) {
                galleryAdapter = new RecyclerViewAdapterGallery(mContext, finalImages.get());
                galleryAdapter.setClickListener(this);
                if (mContext != null)
                    mContext.runOnUiThread(() -> {
                        galleryRecycler = mView.findViewById(R.id.gallery_recycler);

                        int numColumns = calculateNoOfColumns(mContext, 100);

                        ColumnProvider col = () -> numColumns;
                        galleryRecycler.setLayoutManager(new GridLayoutManager(mContext, numColumns));
                        galleryRecycler.addItemDecoration(new GridMarginDecoration(0, col,
                                GridLayoutManager.VERTICAL, false, null));
                        galleryRecycler.setAdapter(galleryAdapter);
                    });
            }

            // Set start slideshow functionality
            ExtendedFloatingActionButton startSlideshowButton =
                    mView.findViewById(R.id.start_slideshow_button);
            if (mContext != null)
                mContext.runOnUiThread(() -> {
                    startSlideshowButton.setOnClickListener(v -> {
                        startSlideshow(0);

                    });
                });

            // Swipe Refresh Actions
            refreshGallery.setOnRefreshListener(this::onRefresh);

            if (mContext != null)
                mContext.runOnUiThread(() -> refreshGallery.setRefreshing(false));
        });

        if (mContext != null)
            thread.start();

        // Auto refresh images every 45 minutes
        Thread t3 = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2700000);
                    onRefresh();

                    // Check if slideshow is happening
                    if (stfalconImageViewer != null) {
                        startSlideshow(stfalconImageViewer.currentPosition());
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t3.start();

        return mView;
    }

    private void fetchVideos(ArrayList<Pair<String, String>> videos) {
        Thread videoThread = new Thread(() -> {
            String albumDir = videoSaveDir.getAbsolutePath() + "/" + albumID;
            File directory = new File(albumDir);
            if (!directory.exists()) {
                directory.mkdir();
            }
            for (Pair<String, String> video : videos) {
                try {
                    String videoUrl = video.first;
                    String videoId = video.second;
                    File file = new File(albumDir + "/" + videoId + ".mp4");
                    if (file.exists()) {
                        Log.d("fileDownload", "file " + videoId + ".mp4 exists");
                    } else {
                        HttpDownloadUtility.downloadFile(videoUrl, albumDir + "/", videoId);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        videoThread.start();
    }

    private void incrementSlideshow(StfalconImageViewer stfalconImageViewer) {
        if (mContext != null && stfalconImageViewer != null) {
            if (finalImages.get().size() - 1 != stfalconImageViewer.currentPosition()) {
                mContext.runOnUiThread(() -> stfalconImageViewer.setCurrentPosition(stfalconImageViewer.currentPosition() + 1));
            } else {
                mContext.runOnUiThread(() -> stfalconImageViewer.setCurrentPosition(0));
            }
        } else {
            Log.e("slideshow", "mContext or stfalconImageViewer were null");
        }
    }

    private void decrementSlideshow(StfalconImageViewer stfalconImageViewer) {
        if (mContext != null && stfalconImageViewer != null) {
            if (finalImages.get().size() - 1 >= stfalconImageViewer.currentPosition()) {
                mContext.runOnUiThread(() -> stfalconImageViewer.setCurrentPosition(stfalconImageViewer.currentPosition() - 1));
            } else {
                mContext.runOnUiThread(() -> stfalconImageViewer.setCurrentPosition(finalImages.get().size() - 1));
            }
        } else {
            Log.e("slideshow", "mContext or stfalconImageViewer were null");
        }
    }

    private void toggleVisibility(AppCompatImageButton button) {
        if (button.getVisibility() == View.VISIBLE) {
            button.setVisibility(View.INVISIBLE);
        } else button.setVisibility(View.VISIBLE);
    }

    public int fetchWidth() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = requireActivity().getWindowManager().getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            return windowMetrics.getBounds().width() - insets.left - insets.right;

        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }


    private void startSlideshow(int position) {
        LayoutInflater inflater2 = LayoutInflater.from(mContext);
        final View overlayView = inflater2.inflate(R.layout.gallery_overlay, null);

        AppCompatImageButton goRight = overlayView.findViewById(R.id.go_right);
        AppCompatImageButton goLeft = overlayView.findViewById(R.id.go_left);
        AppCompatImageButton playButton = overlayView.findViewById(R.id.play_button);
        TextClock clock = overlayView.findViewById(R.id.clock);
        TextClock date = overlayView.findViewById(R.id.date);

        int width = fetchWidth();
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, width / 25);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, width / 100);
//        goRight.setMinimumHeight(width / 100);

        goRight.setVisibility(View.INVISIBLE);
        goLeft.setVisibility(View.INVISIBLE);

        overlayView.setOnClickListener(view -> {
            toggleVisibility(goLeft);
            toggleVisibility(goRight);
        });

        overlayView.setOnTouchListener(new OnSwipeTouchListener(mContext) {
            public void onSwipeTop() {
                stfalconImageViewer.dismiss();
            }

            public void onSwipeRight() {
                decrementSlideshow(stfalconImageViewer);
            }

            public void onSwipeLeft() {
                incrementSlideshow(stfalconImageViewer);
            }

            public void onSwipeBottom() {
                stfalconImageViewer.dismiss();
            }
        });

        // If right chevron is clicked
        goRight.setOnClickListener(y -> {
            incrementSlideshow(stfalconImageViewer);
        });

        // If left chevron is clicked
        goLeft.setOnClickListener(y -> {
            decrementSlideshow(stfalconImageViewer);
        });

//         Play video button
        playButton.setOnClickListener(y -> {
            Log.d("debug", "overlay was clicked");
            MediaItem m = finalImagesRaw.get().get(isWriteable ? stfalconImageViewer.currentPosition() - 1 : stfalconImageViewer.currentPosition());
            if (m.getMediaMetadata().hasVideo()) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                File fileLoc = new File(videoSaveDir + "/" + albumID + "/" + m.getId() + ".mp4");
                Log.d("videoPlayback", "Playing file: " + fileLoc.getAbsolutePath());
                boolean isDownloaded = fileLoc.exists();
                Uri data = isDownloaded ? Uri.parse(fileLoc.getAbsolutePath()) : Uri.parse(m.getBaseUrl() + "=dv");
                intent.setDataAndType(data, "video/*");
                intent.setFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
                Toast.makeText(getContext(), "Playing video", Toast.LENGTH_LONG).show();
                startActivity(intent);
            }
        });

        // Build image viewer
        stfalconImageViewer = new StfalconImageViewer.Builder<>(
                getContext(), isWriteable ? finalImages.get().subList(1,
                finalImages.get().size()) : finalImages.get(), (imageView, image) ->
                Glide.with(mContext).load(image).into(imageView))
                .withOverlayView(overlayView)
                .withDismissListener(() -> {
                    stfalconImageViewer = null;
                    slideshowTimer.cancel();
                    slideshowTimer = null;
                })
                .withStartPosition(isWriteable ? position - 1 : position)
                .show();
        slideshowTimer = new Timer();
        slideshowTimer.scheduleAtFixedRate(new TimerTask() {
                                               @Override
                                               public void run() {
                                                   incrementSlideshow(stfalconImageViewer);
                                               }
                                           },
                //Set how long before to start calling the TimerTask (in milliseconds)
                autoplayDuration * 1000,
                //Set the amount of time between each execution (in milliseconds)
                autoplayDuration * 1000);
    }

    // When refresh is called
    private void onRefresh() {
        refreshGallery.setRefreshing(true);
        Thread t3 = new Thread(() -> {

            // Grab images in album
            images.set(photosLibraryClient.searchMediaItems(albumID).iterateAll());
            finalImages.get().clear();

            // Add all URLS to temp for finalImages
            ArrayList<String> temp = new ArrayList<>();
            ArrayList<MediaItem> temp2 = new ArrayList<>();
            ArrayList<Pair<String, String>> videos = new ArrayList<>();

            // If album is writeable, add an extra image
            if (isWriteable) {
                temp.add("ADDIMAGEPICTURE");
            }

            for (MediaItem i : images.get()) {
                if (i.getMediaMetadata().hasVideo()) {
                    Pair<String, String> video = new Pair<>(i.getBaseUrl() + "=dv", i.getId());
                    videos.add(video);
                }
                temp.add(i.getBaseUrl());
                temp2.add(i);
            }

            finalImages.get().addAll(temp);
            finalImagesRaw.get().addAll(temp2);
            fetchVideos(videos);
            temp.clear();
            temp2.clear();

            // Update views
            if (mContext != null) {
                mContext.runOnUiThread(galleryAdapter::notifyDataSetChanged);
                mContext.runOnUiThread(() -> refreshGallery.setRefreshing(false));
            }
        });
        t3.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE) {
            if (data != null) {
                List<NewMediaItem> newItems = new ArrayList<>();
                ClipData clipData = data.getClipData();
                if (clipData != null) {

                    Thread uploadThread = new Thread(() -> {

                        // Iterate through images chosen
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();

                            //In case you need image's absolute path
                            String pathToFile = getImageFilePath(uri);
                            if (pathToFile.equals("Not found")) {
                                pathToFile = getPathFromInputStreamUri(mContext, uri);
                            }
                            Log.d("media_path", pathToFile);

                            String file_extn = pathToFile.substring(pathToFile.lastIndexOf(".") + 1);

                            if (file_extn.equals("img") || file_extn.equals("jpg") ||
                                    file_extn.equals("jpeg") || file_extn.equals("gif") ||
                                    file_extn.equals("png")) {
                                //FINE
                                Log.v("Upload", "Valid File Extension");

                            } else {
                                //NOT IN REQUIRED FORMAT
                                Log.v("Upload", "Invalid File Extension");
                                return;
                            }

                            // Build and execute request for upload token
                            OkHttpClient client = new OkHttpClient();
                            RequestBody body = RequestBody.create(
                                    MediaType.parse("image/" + file_extn), new File(pathToFile));
                            Request request = new Request.Builder()
                                    .url("https://photoslibrary.googleapis.com/v1/uploads")
                                    .addHeader("Authorization", "Bearer " + MainActivity.accessToken)
                                    .addHeader("Content-type", "application/octet-stream")
                                    .addHeader("X-Goog-Upload-Protocol", "raw")
                                    .post(body)
                                    .build();
                            Call call = client.newCall(request);
                            try {
                                Response response1 = call.execute();
                                // If the upload is successful, get the uploadToken
                                String uploadToken;
                                if (response1.code() == 200) {
                                    uploadToken = response1.body().string();
                                } else {
                                    return;
                                }
                                // Use this upload token to create a media item

                                // Create a NewMediaItem with the following components:
                                // - uploadToken obtained above
                                // - filename that will be shown to the user in Google Photos
                                // - description that will be shown to the user in Google Photos
                                String fileName = "CabinRoadPhotosUpload";
                                String itemDescription = "Uploaded from Cabin Road Photos";
                                NewMediaItem newMediaItem = NewMediaItemFactory
                                        .createNewMediaItem(uploadToken, fileName, itemDescription);
                                newItems.add(newMediaItem);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // Create new media items in a specific album
                            if (!newItems.isEmpty()) {
                                BatchCreateMediaItemsResponse response =
                                        photosLibraryClient.batchCreateMediaItems(albumID, newItems);
                                for (NewMediaItemResult itemsResponse :
                                        response.getNewMediaItemResultsList()) {
                                    Status status = itemsResponse.getStatus();
                                    if (status.getCode() == Code.OK_VALUE) {
                                        // The item is successfully created in the user's library
                                        MediaItem createdItem = itemsResponse.getMediaItem();
                                        Log.v("Upload", "Upload Successful");

                                    } else {
                                        // The item could not be created. Check the status and try again
                                        Log.v("Upload", "Upload Unsuccessful");
                                    }
                                }
                                mContext.runOnUiThread(this::onRefresh);
                            }
                        }
                    });
                    uploadThread.start();
                }
            }
        }

    }

    public static String getPathFromInputStreamUri(Context context, Uri uri) {
        InputStream inputStream = null;
        String filePath = null;

        if (uri.getAuthority() != null) {
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
                File photoFile = createTemporalFileFrom(inputStream, context);

                filePath = photoFile.getPath();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return filePath;
    }

    private static File createTemporalFileFrom(InputStream inputStream, Context context) throws IOException {
        File targetFile = null;

        if (inputStream != null) {
            int read;
            byte[] buffer = new byte[8 * 1024];

            targetFile = createTemporalFile(context);
            OutputStream outputStream = new FileOutputStream(targetFile);

            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();

            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return targetFile;
    }

    private static File createTemporalFile(Context context) {
        return new File(new ContextWrapper(context).getFilesDir(), "tempPicture.jpg");
    }

    public String getImageFilePath(Uri uri) {
        String result = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = mContext.getContentResolver().query(uri, proj, null,
                null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(proj[0]);
                result = cursor.getString(column_index);
            }
            cursor.close();
        }
        if (result == null) {
            result = "Not found";
        }
        return result;
    }

    // When an image is clicked
    @Override
    public void onItemClick(View view, int position) {

        if (galleryAdapter.getItem(position).equals("ADDIMAGEPICTURE")) {
            try {
                // Check if we have permission to read images from storage
                ActivityCompat.requestPermissions(mContext, new
                                String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);

                // If permission was granted
                if (ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setAction(Intent.ACTION_PICK);
                    startActivityForResult(Intent.createChooser(intent,
                            "Select Picture"), PICK_IMAGE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        } else {
            startSlideshow(position);
        }

    }

    // Changes number of columns based on orientation change
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (galleryAdapter != null) {
            int numColumns = calculateNoOfColumns(mContext, 100);
            ColumnProvider col = () -> numColumns;
            galleryRecycler.setLayoutManager(new GridLayoutManager(mContext, numColumns));
            galleryRecycler.addItemDecoration(new GridMarginDecoration(0, col,
                    GridLayoutManager.VERTICAL, false, null));
            galleryRecycler.setAdapter(galleryAdapter);
        }

    }

    // To determine number of columns necessary for GridLayoutManager
    public static int calculateNoOfColumns(Context context, float columnWidthDp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (screenWidthDp / columnWidthDp + 0.5); // +0.5 for correct rounding to int.
        return noOfColumns - 1;
    }

    public void setAlbumTitle(String title) {
        this.albumTitle = title;
    }

    // Grab Application Context, since not started from Activity
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mContext = (AppCompatActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        actionbar.setTitle("Cabin Road Photos");
        actionbar.setNavigationIcon(null);
    }

    @Override
    public void onPause() {
        if (slideshowTimer != null) slideshowTimer.cancel();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (slideshowTimer != null) {
            slideshowTimer = new Timer();
            slideshowTimer.scheduleAtFixedRate(new TimerTask() {
                                                   @Override
                                                   public void run() {
                                                       incrementSlideshow(stfalconImageViewer);
                                                   }
                                               },
                    //Set how long before to start calling the TimerTask (in milliseconds)
                    autoplayDuration * 10,
                    //Set the amount of time between each execution (in milliseconds)
                    autoplayDuration * 1000);
        }
    }
}