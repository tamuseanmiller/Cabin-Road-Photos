package com.rebeccamcfadden.cabinroadphotos;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class RecyclerViewAdapterAlbums extends RecyclerView.Adapter<RecyclerViewAdapterAlbums.ViewHolder> {

    private static List<CustomAlbum> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    public RecyclerViewAdapterAlbums(Context context, List<CustomAlbum> albums) {
        this.mInflater = LayoutInflater.from(context);
        mData = albums;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recycler_album, parent, false);
        return new ViewHolder(view);
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = mInflater.getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterAlbums.ViewHolder holder, int position) {
        CustomAlbum a = mData.get(position);
        String url = a.getCoverPhotoBaseUrl();
//                +((a.getId() == "fullLibrary") ?
//                        "?height=" + dpToPx(100) + "&width=" + dpToPx(150)
//                        : "=w" + dpToPx(150) + "-h" + dpToPx(100) + "-c");
        Glide.with(holder.albumImage.getContext())
                .load(url)
                .thumbnail(0.1f)
                .override(dpToPx(150), dpToPx(100)) // resizes the image to these dimensions (in pixel)
                .centerCrop()
                .into(holder.albumImage);
        if (mData.get(position).getTitle().length() > 26) {
            holder.albumName.setText(mData.get(position).getTitle().substring(0, 25) + "...");
        } else {
            holder.albumName.setText(mData.get(position).getTitle());
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView albumName;
        ShapeableImageView albumImage;
        MaterialCardView albumImageCard;

        ViewHolder(View itemView) {
            super(itemView);
            albumImage = itemView.findViewById(R.id.album_image);
            albumName = itemView.findViewById(R.id.album_name);
            albumImageCard = itemView.findViewById(R.id.album_image_card);

//            albumImage.setOnClickListener(this);
//            itemView.setOnClickListener(this);
            albumImageCard.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            if (mClickListener != null) {
                mClickListener.onItemClick(view, getAdapterPosition());
            }
        }

    }

    // convenience method for getting data at click position
    CustomAlbum getItem(int id) {
        return mData.get(id);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        Glide.with(mInflater.getContext()).clear(holder.albumImage);
        super.onViewRecycled(holder);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);

    }
}


