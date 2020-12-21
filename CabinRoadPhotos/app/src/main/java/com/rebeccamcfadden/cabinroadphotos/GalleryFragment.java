package com.rebeccamcfadden.cabinroadphotos;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.*;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.proto.BatchCreateMediaItemsResponse;
import com.google.photos.library.v1.proto.Filters;
import com.google.photos.library.v1.proto.NewMediaItem;
import com.google.photos.library.v1.proto.NewMediaItemResult;
import com.google.photos.library.v1.proto.UpdateAlbumRequest;
import com.google.photos.library.v1.proto.UpdateAlbumRequestOrBuilder;
import com.google.photos.library.v1.upload.UploadMediaItemRequest;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.photos.types.proto.MediaItem;
import com.google.protobuf.CodedInputStream;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.listeners.OnImageChangeListener;
import com.veinhorn.scrollgalleryview.MediaInfo;
import com.veinhorn.scrollgalleryview.ScrollGalleryView;
import com.veinhorn.scrollgalleryview.builder.GallerySettings;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.cabriole.decorator.ColumnProvider;
import io.cabriole.decorator.GridMarginDecoration;
import ogbe.ozioma.com.glideimageloader.dsl.DSL;

import static ogbe.ozioma.com.glideimageloader.dsl.DSL.image;

public class GalleryFragment extends Fragment implements RecyclerViewAdapterGallery.ItemClickListener {

    private static final int PICK_IMAGE = 1;
    private PhotosLibraryClient photosLibraryClient;
    private String albumID;
    private StfalconImageViewer stfalconImageViewer;
    private AtomicReference<List<String>> finalImages;
    private int autoplayDuration;
    private RecyclerView galleryRecycler;
    private RecyclerViewAdapterGallery galleryAdapter;
    private String albumTitle;
    private AppCompatActivity mContext;
    private boolean isWriteable;

    private Toolbar actionbar;

    public GalleryFragment() {
        albumID = null;
        photosLibraryClient = null;
    }

    public void setPhotosLibraryClient(PhotosLibraryClient photosLibraryClient) {
        this.photosLibraryClient = photosLibraryClient;
    }

    public void setAlbumId(String albumId) {
        this.albumID = albumId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }

//    @Override
//    public void onResume() {
//        showSystemUI();
//        super.onResume();
//    }

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
//            ((TextView) mView.findViewById(R.id.album_title)).setText(albumTitle);
            SwipeRefreshLayout refreshGallery = mView.findViewById(R.id.refresh_gallery);

            if (mContext != null)
                mContext.runOnUiThread(() -> refreshGallery.setRefreshing(true));

            // Get Media Items from Album and add to String List
            AtomicReference<Iterable<MediaItem>> images = new AtomicReference<>(photosLibraryClient.searchMediaItems(albumID).iterateAll());
            finalImages = new AtomicReference<>(new ArrayList<>());

            // Add upload button if album is writeable
            isWriteable = photosLibraryClient.getAlbum(albumID).getIsWriteable();
            if (isWriteable) {
                finalImages.get().add("ADDIMAGEPICTURE");
            }
            AtomicReference<List<String>> videos = new AtomicReference<>(new ArrayList<>());
            AtomicReference<List<String>> notVideos = new AtomicReference<>(new ArrayList<>());
            for (MediaItem i : images.get()) {
                if (i.getMediaMetadata().hasVideo()) {
                    videos.get().add(i.getBaseUrl());
                } else {
                    notVideos.get().add(i.getBaseUrl());
                }
                finalImages.get().add(i.getBaseUrl());
            }

            // Initialize RecyclerView
            if (mContext != null) {
                galleryAdapter = new RecyclerViewAdapterGallery(getContext(), finalImages.get());
                galleryAdapter.setClickListener(this);
                if (mContext != null)
                    mContext.runOnUiThread(() -> {
                        galleryRecycler = mView.findViewById(R.id.gallery_recycler);

                        int numColumns = calculateNoOfColumns(mContext, 100);

                        ColumnProvider col = () -> numColumns;
                        galleryRecycler.setLayoutManager(new GridLayoutManager(mContext, numColumns));
                        galleryRecycler.addItemDecoration(new GridMarginDecoration(0, col, GridLayoutManager.VERTICAL, false, null));
                        galleryRecycler.setAdapter(galleryAdapter);
                    });
            }

            // Set start slideshow functionality
            ExtendedFloatingActionButton startSlideshowButton = mView.findViewById(R.id.start_slideshow_button);
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

                        LayoutInflater inflater2 = LayoutInflater.from(mView.getContext());
                        final View overlayView = inflater2.inflate(R.layout.gallery_overlay, null);

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

                        stfalconImageViewer = new StfalconImageViewer.Builder<>(
                                getContext(), isWriteable ? finalImages.get().subList(1, finalImages.get().size() - 1) : finalImages.get(), (imageView, image) ->
                                Glide.with(mContext).load(image).into(imageView))
                                .withOverlayView(overlayView)
                                .show();

                        // Add upload offset if album is writeable
                        if (isWriteable) {
                            stfalconImageViewer.setCurrentPosition(1);
                        }

                    });
                });

            // Swipe Refresh Actions
            refreshGallery.setOnRefreshListener(() -> {
                Thread t3 = new Thread(() -> {
                    images.set(photosLibraryClient.searchMediaItems(albumID).iterateAll());
                    finalImages.get().clear();
                    ArrayList<String> temp = new ArrayList<>();
                    for (MediaItem i : images.get()) {
                        temp.add(i.getBaseUrl());
                    }
                    finalImages.get().addAll(temp);
                    temp.clear();
                    if (mContext != null) {
                        mContext.runOnUiThread(galleryAdapter::notifyDataSetChanged);
                        mContext.runOnUiThread(() -> refreshGallery.setRefreshing(false));
                    }
                });
                t3.start();
            });

            // Start slideshow
            Thread t2 = new Thread(() -> {
                int cnt = 0;

                // Traverse full album
                while (cnt <= finalImages.get().size()) {
                    if (stfalconImageViewer != null) {
                        int finalCnt = cnt;
                        if (mContext != null)
                            mContext.runOnUiThread(() -> stfalconImageViewer.setCurrentPosition(finalCnt));
                        cnt++;
                    }
                    try {
                        // Sleep for 5 minutes
                        Thread.sleep(autoplayDuration * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t2.start();

            if (mContext != null)
                mContext.runOnUiThread(() -> refreshGallery.setRefreshing(false));

        });

        if (mContext != null)
            thread.start();


        return mView;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE) {
//            Uri selectedImageUri = data.getData();
//            String pathToFile = getImageFilePath(selectedImageUri);
//            Log.d("media_path", pathToFile);

            if (data != null) {
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri uri = item.getUri();

                        //In case you need image's absolute path
                        String pathToFile = getImageFilePath(uri);
                        Log.d("media_path", pathToFile);

                        // Open the file and automatically close it after upload
                        try (RandomAccessFile file = new RandomAccessFile(pathToFile, "r")) {
                            // Create a new upload request
                            UploadMediaItemRequest uploadRequest =
                                    UploadMediaItemRequest.newBuilder()
                                            // The file to upload
                                            .setDataFile(file)
                                            .build();
                            // Upload and capture the response
                            UploadMediaItemResponse uploadResponse = photosLibraryClient.uploadMediaItem(uploadRequest);
                            if (uploadResponse.getError().isPresent()) {
                                // If the upload results in an error, handle it
                                UploadMediaItemResponse.Error error = uploadResponse.getError().get();

                                Log.v("Upload", "Error uploading file", error.getCause());

                            } else {
                                // If the upload is successful, get the uploadToken
                                String uploadToken = uploadResponse.getUploadToken().get();
                                // Use this upload token to create a media item

                                // Create a NewMediaItem with the following components:
                                // - uploadToken obtained above
                                // - filename that will be shown to the user in Google Photos
                                // - description that will be shown to the user in Google Photos
                                String fileName = "CabinRoadPhotosUpload";
                                String itemDescription = "Uploaded from Cabin Road Photos";
                                NewMediaItem newMediaItem = NewMediaItemFactory
                                        .createNewMediaItem(uploadToken, fileName, itemDescription);
                                List<NewMediaItem> newItems = Arrays.asList(newMediaItem);

                                // Create new media items in a specific album
                                BatchCreateMediaItemsResponse response = photosLibraryClient.batchCreateMediaItems(albumID, newItems);
                                for (NewMediaItemResult itemsResponse : response.getNewMediaItemResultsList()) {
                                    Status status = itemsResponse.getStatus();
                                    if (status.getCode() == Code.OK_VALUE) {
                                        // The item is successfully created in the user's library
                                        MediaItem createdItem = itemsResponse.getMediaItem();

                                    } else {
                                        // The item could not be created. Check the status and try again
                                    }
                                }
                            }
                        } catch (IOException e) {
                            // Error accessing the local file
                        }
                    }
                }
            }
        }

    }

    public String getImageFilePath(Uri uri) {
        String result = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = mContext.getContentResolver().query(uri, proj, null, null, null);
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

    @Override
    public void onItemClick(View view, int position) {
//        hideSystemUI();

        if (galleryAdapter.getItem(position).equals("ADDIMAGEPICTURE")) {
            try {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setAction(Intent.ACTION_PICK);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }

        } else {
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
            stfalconImageViewer = new StfalconImageViewer.Builder<>(getContext(), isWriteable ? finalImages.get().subList(1, finalImages.get().size() - 1) : finalImages.get(),
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
            galleryRecycler.addItemDecoration(new GridMarginDecoration(0, col, GridLayoutManager.VERTICAL, false, null));
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

//    private void hideSystemUI() {
//        // Enables regular immersive mode.
//        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
//        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        View decorView = mContext.getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_IMMERSIVE
//                        // Set the content to appear under the system bars so that the
//                        // content doesn't resize when the system bars hide and show.
//                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        // Hide the nav bar and status bar
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
//    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = mContext.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void setAlbumTitle(String title) {
        this.albumTitle = title;
    }

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
        try {
            Fragment fragment = mContext.getSupportFragmentManager().findFragmentByTag("gallery_fragment");
            FragmentTransaction ft = mContext.getSupportFragmentManager()
                    .beginTransaction();
            ft.remove(fragment);
            ft.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mContext = null;
    }
}