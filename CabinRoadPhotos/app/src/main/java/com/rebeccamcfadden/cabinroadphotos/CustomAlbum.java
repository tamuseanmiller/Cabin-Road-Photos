package com.rebeccamcfadden.cabinroadphotos;

import android.content.Intent;

import androidx.annotation.Nullable;

import com.google.photos.types.proto.Album;

public class CustomAlbum extends Object{
    private final String coverPhotoUrl;
    private final String title;
    private final String id;

    public CustomAlbum() {
        this.id = null;
        this.title = null;
        this.coverPhotoUrl = null;
    }

    public CustomAlbum(String id, String name) {
        this.id = id;
        this.title = name;
        this.coverPhotoUrl = "https://sites.google.com/a/pressatgoogle.com/googlephotos/_/rsrc/1441229614786/images-logos-and-video/logo_lockup_photos_icon_vertical.jpg";
    }

    public CustomAlbum(Album a) {
        this.id = a.getId();
        this.title = a.getTitle();
        this.coverPhotoUrl = a.getCoverPhotoBaseUrl();
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getCoverPhotoBaseUrl() {
        return this.coverPhotoUrl;
    }

    public boolean equals( CustomAlbum obj) {
        return this.id == obj.getId();
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
