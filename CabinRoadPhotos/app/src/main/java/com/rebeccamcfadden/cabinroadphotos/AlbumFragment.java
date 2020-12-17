package com.rebeccamcfadden.cabinroadphotos;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.types.proto.Album;

import java.util.ArrayList;
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
            List<Album> preAlbum = new ArrayList<>();;
            for (Album album : photosLibraryClient.listAlbums().iterateAll()) {
                preAlbum.add(album);
            }
            AtomicReference<List<Album>> albums = new AtomicReference<List<Album>>(preAlbum);
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
            RelativeLayout bottomSheet = mView.findViewById(R.id.album_sheet);
            BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        });

        // Create Album Sheet
        TextInputEditText textField = mView.findViewById(R.id.textField);
        MaterialButton doneButton = mView.findViewById(R.id.done_button);

        // Create Album bottomsheet onClick
        doneButton.setOnClickListener(v -> {
            if (!textField.getText().toString().isEmpty()) {
                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getActivity());
                dialogBuilder.setTitle("Create Album");
                dialogBuilder.setMessage("Are you sure you want to create an album named " + textField.getText() + "?");
                dialogBuilder.setPositiveButton("Yes", (dialog, which) -> {
                    photosLibraryClient.createAlbum(String.valueOf(textField.getText()));
                });
                dialogBuilder.setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                });
                dialogBuilder.show();
            } else {
                textField.setError("Input A Name");
            }
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