package com.rebeccamcfadden.cabinroadphotos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.types.proto.MediaItem;
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
    private int albumIndex;
    private StfalconImageViewer stfalconImageViewer;
    private AtomicReference<List<String>> finalImages;

    public GalleryFragment() {
        albumIndex = 0;
        photosLibraryClient = null;
    }

    public void setPhotosLibraryClient(PhotosLibraryClient photosLibraryClient) {
        this.photosLibraryClient = photosLibraryClient;
    }

    public void setAlbumIndex(int albumIndex) {
        this.albumIndex = albumIndex;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_gallery, container, false);

        Thread thread = new Thread(() -> {
            SwipeRefreshLayout refreshGallery = mView.findViewById(R.id.refresh_gallery);
            requireActivity().runOnUiThread(() -> refreshGallery.setRefreshing(true));

            // Get AlbumID
            String albumID = photosLibraryClient.listAlbums().getPage().getResponse().getAlbums(albumIndex).getId();

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


                    stfalconImageViewer = new StfalconImageViewer.Builder<>(getContext(), finalImages.get(), (imageView, image) -> Glide.with(getActivity()).load(image).into(imageView)).show();
                });
            });

            // Swipe Refresh Actions
            refreshGallery.setOnRefreshListener(() -> {
                images.set(photosLibraryClient.searchMediaItems(albumID).iterateAll());
                finalImages.get().clear();
                for (MediaItem i : images.get()) {
                    finalImages.get().add(i.getBaseUrl());
                }
                galleryAdapter.notifyDataSetChanged();
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
                        Thread.sleep(50000);
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

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getActivity().getWindow().getDecorView();
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

    @Override
    public void onItemClick(View view, int position) {

        StfalconImageViewer stfalconImageViewer = null;
        stfalconImageViewer = new StfalconImageViewer.Builder<>(getContext(), finalImages.get(),
                (imageView, image) -> Glide.with(requireActivity()).load(image).into(imageView))
                .show();

        stfalconImageViewer.setCurrentPosition(position);

        // Start slideshow
        StfalconImageViewer finalStfalconImageViewer = stfalconImageViewer;
        Thread t2 = new Thread(() -> {
            int cnt = position;

            // Traverse full album
            while (cnt <= finalImages.get().size()) {
                if (finalStfalconImageViewer != null) {
                    int finalCnt = cnt;
                    requireActivity().runOnUiThread(() -> finalStfalconImageViewer.setCurrentPosition(finalCnt));
                    cnt++;
                }
                try {
                    // Sleep for 5 minutes
                    Thread.sleep(50000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t2.start();
    }
}