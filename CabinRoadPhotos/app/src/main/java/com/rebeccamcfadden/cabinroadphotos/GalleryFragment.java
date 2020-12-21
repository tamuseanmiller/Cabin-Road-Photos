package com.rebeccamcfadden.cabinroadphotos;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.photos.library.v1.PhotosLibraryClient;
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
import com.stfalcon.imageviewer.listeners.OnDismissListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.cabriole.decorator.ColumnProvider;
import io.cabriole.decorator.GridMarginDecoration;

import static com.rebeccamcfadden.cabinroadphotos.MainActivity.photosLibraryClient;

public class GalleryFragment extends Fragment implements RecyclerViewAdapterGallery.ItemClickListener {

    private static final int PICK_IMAGE = 1;
    private String albumID;
    private StfalconImageViewer stfalconImageViewer;
    private AtomicReference<List<String>> finalImages;
    private int autoplayDuration;
    private RecyclerView galleryRecycler;
    private RecyclerViewAdapterGallery galleryAdapter;
    private String albumTitle;
    private AppCompatActivity mContext;
    private boolean isWriteable;
    private AtomicReference<Iterable<MediaItem>> images;
    private SwipeRefreshLayout refreshGallery;

    private Toolbar actionbar;

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
            finalImages = new AtomicReference<>(new ArrayList<>());

            // Add upload button if album is writeable
            isWriteable = photosLibraryClient.getAlbum(albumID).getIsWriteable();
            if (isWriteable) {
                finalImages.get().add("ADDIMAGEPICTURE");
            }
            for (MediaItem i : images.get()) {
                finalImages.get().add(i.getBaseUrl());
            }

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

                        /*ScrollGalleryView scrollGalleryView = mView.findViewById(R.id.scroll_gallery_view);
                        scrollGalleryView.setVisibility(View.VISIBLE);

                        hideSystemUI();

                        scrollGalleryView
                                .setThumbnailSize(200)
                                .setZoom(true)
                                .withHiddenThumbnails(true);

                        ScrollGalleryView
                                .from(scrollGalleryView)
                                .settings(
                                        GallerySettings
                                                .from(mContext.getSupportFragmentManager())
                                                .thumbnailSize(10)
                                                .enableZoom(true)
                                                .build()
                                )
                                .add(DSL.video(videos.get().get(0), R.drawable.placeholder_image))
                                .add(DSL.images(notVideos.get()))
                                .build();*/

                        //                    hideSystemUI();
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

    private void startSlideshow(int position) {
        LayoutInflater inflater2 = LayoutInflater.from(mContext);
        final View overlayView = inflater2.inflate(R.layout.gallery_overlay, null);

        AppCompatImageButton goRight = overlayView.findViewById(R.id.go_right);
        AppCompatImageButton goLeft = overlayView.findViewById(R.id.go_left);
        goLeft.setVisibility(View.VISIBLE);
        goRight.setVisibility(View.VISIBLE);

        // If right chevron is clicked
        goRight.setOnClickListener(y -> {
            if (finalImages.get().size() - 1 != stfalconImageViewer.currentPosition()) {
                stfalconImageViewer.setCurrentPosition(stfalconImageViewer.currentPosition() + 1);
            }
        });

        // If left chevron is clicked
        goLeft.setOnClickListener(y -> {
            if (finalImages.get().size() - 1 >= stfalconImageViewer.currentPosition()) {
                stfalconImageViewer.setCurrentPosition(stfalconImageViewer.currentPosition() - 1);
            }
        });

        // Build image viewer
        stfalconImageViewer = new StfalconImageViewer.Builder<>(
                getContext(), isWriteable ? finalImages.get().subList(1,
                finalImages.get().size()) : finalImages.get(), (imageView, image) ->
                Glide.with(mContext).load(image).into(imageView))
                .withOverlayView(overlayView)
                .withDismissListener(() -> stfalconImageViewer = null)
                .show();

        stfalconImageViewer.setCurrentPosition(position);

        // Start Slideshow Thread
        Thread t2 = new Thread(() -> {

            // Traverse full album
            while (true) {
                try {
                    // Sleep for x minutes, then switch picture
                    Thread.sleep(autoplayDuration * 1000);
                    if (mContext != null && stfalconImageViewer != null) {
                        if (stfalconImageViewer.currentPosition() + 2 < finalImages.get().size()) {
                            mContext.runOnUiThread(() -> stfalconImageViewer.setCurrentPosition(
                                    stfalconImageViewer.currentPosition() + 1));
                        } else {
                            mContext.runOnUiThread(() -> stfalconImageViewer.setCurrentPosition(0));
                        }
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t2.start();
    }

    // When refresh is called
    private void onRefresh() {
        refreshGallery.setRefreshing(true);
        Thread t3 = new Thread(() -> {

            // Grab images in album
            images.set(photosLibraryClient.searchMediaItems(albumID).iterateAll());
            finalImages.get().clear();
            ArrayList<String> temp = new ArrayList<>();

            // If album is writeable, add an extra image
            if (isWriteable) {
                temp.add("ADDIMAGEPICTURE");
            }

            // Add all URLS to temp for finalImages
            for (MediaItem i : images.get()) {
                temp.add(i.getBaseUrl());
            }
            finalImages.get().addAll(temp);
            temp.clear();

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

            // Open Full screen image view and set overlay button clicks
            LayoutInflater inflater = LayoutInflater.from(view.getContext());
            final View overlayView = inflater.inflate(R.layout.gallery_overlay, null);

            AppCompatImageButton goRight = overlayView.findViewById(R.id.go_right);
            AppCompatImageButton goLeft = overlayView.findViewById(R.id.go_left);
            goLeft.setVisibility(View.VISIBLE);
            goRight.setVisibility(View.VISIBLE);

            goRight.setOnClickListener(y -> {
                if (finalImages.get().size() - 1 != stfalconImageViewer.currentPosition()) {
                    stfalconImageViewer.setCurrentPosition(stfalconImageViewer.currentPosition() + 1);
                }
            });

            goLeft.setOnClickListener(y -> {
                if (finalImages.get().size() - 1 >= stfalconImageViewer.currentPosition()) {
                    stfalconImageViewer.setCurrentPosition(stfalconImageViewer.currentPosition() - 1);
                }
            });

            Log.d("debug", "we are on this line");  // LMAO WHAT IS THIS
            stfalconImageViewer = new StfalconImageViewer.Builder<>(getContext(), isWriteable ?
                    finalImages.get().subList(1, finalImages.get().size()) : finalImages.get(),
                    (imageView, image) -> Glide.with(mContext).load(image).into(imageView))
                    .withOverlayView(overlayView)
                    .withStartPosition(isWriteable ? position - 1 : position)
                    .show();

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
}