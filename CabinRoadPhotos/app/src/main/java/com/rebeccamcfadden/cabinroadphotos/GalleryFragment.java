package com.rebeccamcfadden.cabinroadphotos;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
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
    private String albumTitle;

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
        actionbar = requireActivity().findViewById(R.id.toolbar_main);
        if (actionbar != null) {
            Log.d("debug", "action bar was non null");
            Drawable drawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_arrow_back);
            drawable.setTint(ContextCompat.getColor(getActivity(), R.color.white));
            actionbar.setNavigationIcon(drawable);
            actionbar.setNavigationOnClickListener(v -> {
                actionbar.setNavigationIcon(null);
                getActivity().onBackPressed();
            });
        }

        Thread thread = new Thread(() -> {
            ((TextView) mView.findViewById(R.id.album_title)).setText(albumTitle);
            SwipeRefreshLayout refreshGallery = mView.findViewById(R.id.refresh_gallery);
            requireActivity().runOnUiThread(() -> refreshGallery.setRefreshing(true));

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
            RecyclerViewAdapterGallery galleryAdapter = new RecyclerViewAdapterGallery(getContext(), finalImages.get());
            galleryAdapter.setClickListener(this);
            requireActivity().runOnUiThread(() -> {
                RecyclerView galleryRecycler = mView.findViewById(R.id.gallery_recycler);
                ColumnProvider col = () -> 10;
                galleryRecycler.setLayoutManager(new GridLayoutManager(getActivity(), 10));
                galleryRecycler.addItemDecoration(new GridMarginDecoration(0, col, GridLayoutManager.VERTICAL, false, null));
                galleryRecycler.setAdapter(galleryAdapter);
            });

            // Set start slideshow functionality
            ExtendedFloatingActionButton startSlideshowButton = mView.findViewById(R.id.start_slideshow_button);
            requireActivity().runOnUiThread(() -> {
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
                                            .from(getActivity().getSupportFragmentManager())
                                            .thumbnailSize(10)
                                            .enableZoom(true)
                                            .build()
                            )
                            .add(DSL.video(videos.get().get(0), R.drawable.placeholder_image))
                            .add(DSL.images(notVideos.get()))
                            .build();*/

//                    hideSystemUI();
                    stfalconImageViewer = new StfalconImageViewer.Builder<>(
                            getContext(), finalImages.get(), (imageView, image) ->
                            Glide.with(requireActivity()).load(image).into(imageView))
//                            .withDismissListener(() -> {
//                                showSystemUI();
//                            })
                            .show();
                });
            });

            // Swipe Refresh Actions
            refreshGallery.setOnRefreshListener(() -> {
                images.set(photosLibraryClient.searchMediaItems(albumID).iterateAll());
                finalImages.get().clear();
                ArrayList<String> temp = new ArrayList<>();
                for (MediaItem i : images.get()) {
                    temp.add(i.getBaseUrl());
                }
                finalImages.get().addAll(temp);
                temp.clear();
                requireActivity().runOnUiThread(galleryAdapter::notifyDataSetChanged);
                requireActivity().runOnUiThread(() -> refreshGallery.setRefreshing(false));
            });

            // Start slideshow
            Thread t2 = new Thread(() -> {
                int cnt = 0;

                // Traverse full album
                while (cnt <= finalImages.get().size()) {
                    if (stfalconImageViewer != null) {
                        int finalCnt = cnt;
                        requireActivity().runOnUiThread(() -> stfalconImageViewer.setCurrentPosition(finalCnt));
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

            requireActivity().runOnUiThread(() -> refreshGallery.setRefreshing(false));

        });
        thread.start();

        return mView;
    }

    @Override
    public void onItemClick(View view, int position) {
//        hideSystemUI();
        Log.d("debug", "we are on this line");  // LMAO WHAT IS THIS
        StfalconImageViewer stfalconImageViewer = new StfalconImageViewer.Builder<>(getContext(), finalImages.get(),
                (imageView, image) -> Glide.with(requireActivity()).load(image).into(imageView))
                .withDismissListener(() -> {
                    showSystemUI();
                })
                .show();

        stfalconImageViewer.setCurrentPosition(position);
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = requireActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void setAlbumTitle(String title) {
        this.albumTitle = title;
    }
}