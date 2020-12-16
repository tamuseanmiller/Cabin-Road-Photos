package com.rebeccamcfadden.cabinroadphotos;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.types.proto.Album;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.cabriole.decorator.ColumnProvider;
import io.cabriole.decorator.GridMarginDecoration;

public class AlbumFragment extends Fragment implements RecyclerViewAdapterAlbums.ItemClickListener {

    private PhotosLibraryClient photosLibraryClient;

    public AlbumFragment() {

    }

    public void setPhotosLibraryClient(PhotosLibraryClient photosLibraryClient) {
        this.photosLibraryClient = photosLibraryClient;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_album, container, false);

        Thread thread = new Thread(() -> {
            SwipeRefreshLayout refreshAlbum = mView.findViewById(R.id.refresh_album);
            requireActivity().runOnUiThread(() -> refreshAlbum.setRefreshing(true));

            // Fetch albums
            AtomicReference<List<Album>> albums = new AtomicReference<>(photosLibraryClient.listAlbums().getPage().getResponse().getAlbumsList());
            RecyclerViewAdapterAlbums albumAdapter = new RecyclerViewAdapterAlbums(getContext(), albums.get());
            RecyclerView albumRecycler = mView.findViewById(R.id.album_recycler);
            albumAdapter.setClickListener(this);
            ColumnProvider col = () -> 7;

            // Initialize Recylerview
            requireActivity().runOnUiThread(() -> {
                albumRecycler.setLayoutManager(new GridLayoutManager(getActivity(), 7));
                albumRecycler.addItemDecoration(new GridMarginDecoration(0, col, GridLayoutManager.VERTICAL, false, null));
                albumRecycler.setAdapter(albumAdapter);

                // Swipe Refresh
                refreshAlbum.setNestedScrollingEnabled(true);
                refreshAlbum.setRefreshing(false);
            });

            refreshAlbum.setOnRefreshListener(() -> {
                albums.set(photosLibraryClient.listAlbums().getPage().getResponse().getAlbumsList());
                albumAdapter.notifyDataSetChanged();
                requireActivity().runOnUiThread(() -> refreshAlbum.setRefreshing(false));
            });
        });
        thread.start();

        // Create Album Button
        ExtendedFloatingActionButton createAlbumButton = mView.findViewById(R.id.create_album_button);
        createAlbumButton.setOnClickListener(v -> {
//            photosLibraryClient.createAlbum();
        });

        return mView;
    }

    // RecyclerView onClick
    @Override
    public void onItemClick(View view, int position) {
        GalleryFragment galleryFragment = new GalleryFragment();
        galleryFragment.setPhotosLibraryClient(photosLibraryClient);
        galleryFragment.setAlbumIndex(position);
        FragmentManager transaction = getActivity().getSupportFragmentManager();
        transaction.beginTransaction()
                .replace(R.id.main_layout, galleryFragment) //<---replace a view in your layout (id: container) with the newFragment
                .addToBackStack(null)
                .commit();

    }
}