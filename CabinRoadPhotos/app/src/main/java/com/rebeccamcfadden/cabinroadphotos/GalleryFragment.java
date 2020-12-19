package com.rebeccamcfadden.cabinroadphotos;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.*;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.proto.Filters;
import com.google.photos.library.v1.proto.UpdateAlbumRequest;
import com.google.photos.library.v1.proto.UpdateAlbumRequestOrBuilder;
import com.google.photos.types.proto.MediaItem;
import com.google.protobuf.CodedInputStream;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.listeners.OnImageChangeListener;
import com.veinhorn.scrollgalleryview.MediaInfo;
import com.veinhorn.scrollgalleryview.ScrollGalleryView;
import com.veinhorn.scrollgalleryview.builder.GallerySettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.cabriole.decorator.ColumnProvider;
import io.cabriole.decorator.GridMarginDecoration;
import ogbe.ozioma.com.glideimageloader.dsl.DSL;

import static ogbe.ozioma.com.glideimageloader.dsl.DSL.image;

public class GalleryFragment extends Fragment implements RecyclerViewAdapterGallery.ItemClickListener {

    private PhotosLibraryClient photosLibraryClient;
    private String albumID;
    private StfalconImageViewer stfalconImageViewer;
    private AtomicReference<List<String>> finalImages;
    private int autoplayDuration;
    private RecyclerView galleryRecycler;
    private RecyclerViewAdapterGallery galleryAdapter;
    private String albumTitle;
    private AppCompatActivity mContext;

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
                        stfalconImageViewer = new StfalconImageViewer.Builder<>(
                                getContext(), finalImages.get(), (imageView, image) ->
                                Glide.with(mContext).load(image).into(imageView))
                                .withOverlayView(overlayView)
                                //                            .withDismissListener(() -> {
                                //                                showSystemUI();
                                //                            })
                                .show();

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

    @Override
    public void onItemClick(View view, int position) {
//        hideSystemUI();

        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        final View overlayView = inflater.inflate(R.layout.gallery_overlay, null);

        Log.d("debug", "we are on this line");  // LMAO WHAT IS THIS
        new StfalconImageViewer.Builder<>(getContext(), finalImages.get(),
                (imageView, image) -> Glide.with(mContext).load(image).into(imageView))
//                .withDismissListener(() -> {
//                    showSystemUI();
//                })
                .withStartPosition(position)
                .withOverlayView(overlayView)
                .show();

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
        mContext.getSupportActionBar().setDisplayShowCustomEnabled(false);

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        actionbar.setTitle("Cabin Road Photos");

        try {
            Fragment fragment = mContext.getSupportFragmentManager().findFragmentByTag("gallery_fragment");
            FragmentTransaction ft = mContext.getSupportFragmentManager()
                    .beginTransaction();
            ft.remove(fragment);
            ft.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}