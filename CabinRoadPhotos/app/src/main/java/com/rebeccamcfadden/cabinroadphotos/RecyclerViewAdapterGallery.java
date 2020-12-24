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

public class RecyclerViewAdapterGallery extends RecyclerView.Adapter<RecyclerViewAdapterGallery.ViewHolder> {

    private static List<String> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    public RecyclerViewAdapterGallery(Context context, List<String> albums) {
        this.mInflater = LayoutInflater.from(context);
        mData = albums;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recycler_gallery, parent, false);
        return new ViewHolder(view);
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = mInflater.getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterGallery.ViewHolder holder, int position) {
        if (!mData.get(position).equals("ADDIMAGEPICTURE")) {
            String url = mData.get(position)/* + "=w" + dpToPx(75) + "-h" + dpToPx(75) + "-c"*/;
            Glide.with(holder.galleryImage.getContext())
                    .load(url)
                    .thumbnail(0.1f)
                    .override(dpToPx(75), dpToPx(75)) // resizes the image to these dimensions (in pixel)
                    .centerCrop()
                    .into(holder.galleryImage);
            holder.galleryImageCard.setElevation(1);

        } else {
            holder.galleryImageCard.setElevation(0);
            holder.addImage.setVisibility(View.VISIBLE);
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
        ShapeableImageView galleryImage;
        MaterialCardView galleryImageCard;
        ShapeableImageView addImage;

        ViewHolder(View itemView) {
            super(itemView);
            galleryImage = itemView.findViewById(R.id.gallery_image);
            galleryImageCard = itemView.findViewById(R.id.gallery_image_card);
            addImage = itemView.findViewById(R.id.add_image);

            galleryImageCard.setOnClickListener(this);
//            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            if (mClickListener != null) {
                mClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        Glide.with(mInflater.getContext()).clear(holder.galleryImage);
        super.onViewRecycled(holder);
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
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


