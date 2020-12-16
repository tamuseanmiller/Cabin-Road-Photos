package com.rebeccamcfadden.cabinroadphotos;

import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.types.proto.MediaItem;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;
import com.stfalcon.imageviewer.viewer.builder.BuilderData;

import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.cabriole.decorator.ColumnProvider;
import io.cabriole.decorator.GridMarginDecoration;

public class GalleryFragment extends Fragment {

    private final PhotosLibraryClient photosLibraryClient;
    private final int albumIndex;
    private StfalconImageViewer stfalconImageViewer;

    public GalleryFragment(PhotosLibraryClient photosLibraryClient, int index) {
        // Required empty public constructor
        this.photosLibraryClient = photosLibraryClient;
        albumIndex = index;
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

            // Get AlbumID
            String albumID = photosLibraryClient.listAlbums().getPage().getResponse().getAlbums(albumIndex).getId();

            // Get Media Items from Album and add to String List
            List<MediaItem> images = photosLibraryClient.searchMediaItems(albumID).getPage().getResponse().getMediaItemsList();
            List<String> finalImages = new ArrayList<>();
            for (MediaItem i : images) {
                finalImages.add(i.getBaseUrl());
            }

            // Initialize RecyclerView
            RecyclerViewAdapterGallery galleryAdapter = new RecyclerViewAdapterGallery(getContext(), finalImages);
            requireActivity().runOnUiThread(() -> {
                RecyclerView galleryRecycler = mView.findViewById(R.id.gallery_recycler);
                ColumnProvider col = () -> 9;
                galleryRecycler.setLayoutManager(new GridLayoutManager(getActivity(), 9));
                galleryRecycler.addItemDecoration(new GridMarginDecoration(0, col, GridLayoutManager.VERTICAL, false, null));
                galleryRecycler.setAdapter(galleryAdapter);
            });

            // Set start slideshow functionality
            ExtendedFloatingActionButton startSlidedshowButton = mView.findViewById(R.id.start_slideshow_button);
            requireActivity().runOnUiThread(() -> {
                startSlidedshowButton.setOnClickListener(v -> {
                    stfalconImageViewer = new StfalconImageViewer.Builder<>(getContext(), finalImages, (imageView, image) -> Glide.with(getActivity()).load(image).into(imageView)).show();
                });
            });

            // Start slideshow
            Thread t2 = new Thread(() -> {
                int cnt = 0;

                // Traverse full album
                while (cnt <= images.size()) {
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

        });
        thread.start();

        return mView;
    }
}