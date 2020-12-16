package com.rebeccamcfadden.cabinroadphotos;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.types.proto.MediaItem;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumConfig;
import com.yanzhenjie.album.AlbumFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    private String accessToken;
    private PhotosLibraryClient photosLibraryClient;
    private Thread t1;
    private String idToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        idToken = intent.getStringExtra("idToken");

        getAccessToken();

        // Photos access thread
        t1 = new Thread(() -> {

            getPhotosLibrary();

            Fragment albumFragment = new AlbumFragment(photosLibraryClient);
            FragmentManager transaction = getSupportFragmentManager();
            transaction.beginTransaction()
                    .replace(R.id.main_layout, albumFragment) //<---replace a view in your layout (id: container) with the newFragment
                    .addToBackStack(null)
                    .commit();
        });

    }

    private void getPhotosLibrary() {

        // Build credentials for Photos request
        UserCredentials creds = UserCredentials.newBuilder()
                .setClientId(Properties.getWebAPIKey())
                .setClientSecret(Properties.getWebSecretKey())
                .setAccessToken(new AccessToken(accessToken, null))
                .build();

        // Set up the Photos Library Client that interacts with the API
        PhotosLibrarySettings settings = null;
        try {
            settings = PhotosLibrarySettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(creds)).build();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Fetch photos library
        assert settings != null;
        try {
            photosLibraryClient = PhotosLibraryClient.initialize(settings);

        } catch (IOException e) {

        }
    }

    private void getAccessToken() {
        // Build HttpClient
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormEncodingBuilder()
                .add("grant_type", "authorization_code")
                .add("client_id", Properties.getWebAPIKey())   // something like : ...apps.googleusercontent.com
                .add("client_secret", Properties.getWebSecretKey())
                .add("redirect_uri", "")
                .add("code", new SharedPreferencesManager(getApplicationContext()).retrieveString("code", "NULL") /*LoginActivity.getAccount().getServerAuthCode()*/) // device code.
                .add("id_token", idToken) // This is what we received in Step 5, the jwt token.
                .build();

        final Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v4/token")
                .post(requestBody)
                .build();

        // Make Http Request for access token
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Request request, final IOException e) {
                Log.e("Auth", e.toString());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    // Log response
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    final String message = jsonObject.toString(5);
                    accessToken = jsonObject.get("access_token").toString();
                    Log.i("Auth", message);

                    t1.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}