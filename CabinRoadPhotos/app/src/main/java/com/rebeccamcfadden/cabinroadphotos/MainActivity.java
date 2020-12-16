package com.rebeccamcfadden.cabinroadphotos;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

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

    public static String accessToken;
    private static PhotosLibraryClient photosLibraryClient;
    private Thread t1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getAccessToken();

        AtomicReference<StfalconImageViewer> stfalconImageViewer = new AtomicReference<>(null);

        Thread t2 = null;

        // Photos access thread
        t1 = new Thread(() -> {


            getPhotosLibrary();
//            Arrays.asList(photosLibraryClient.listMediaItems().getPage().getResponse().getMediaItems(0).getProductUrl().trim());
            ArrayList<String> images = new ArrayList<>();
            ArrayList<AlbumFile> albums = new ArrayList<>();
            for (MediaItem image : photosLibraryClient.listMediaItems().getPage().getResponse().getMediaItemsList()) {
                images.add(image.getBaseUrl().trim());
                AlbumFile af = new AlbumFile();
                af.setPath(image.getBaseUrl().trim());
                albums.add(af);
            }

            Album.initialize(AlbumConfig.newBuilder(this)
                    .setAlbumLoader(new MediaLoader())
                    .build());

            MaterialButton gallery = findViewById(R.id.gallery_button);
            gallery.setOnClickListener(v -> {

                runOnUiThread(() -> {

                    Album.image(this)
                            .singleChoice() // Multi-Mode, Single-Mode: singleChoice().
                            .columnCount(4) // The number of columns in the page list.
                            .onResult(result -> {
                                ArrayList<String> results = new ArrayList<>();

                                // Add images before and after selected image
                                results.add(result.get(0).getPath());
                                results.addAll(images);
                                stfalconImageViewer.set(new StfalconImageViewer.Builder<>(this, results, (imageView, image) -> {
                                    Glide.with(this).load(image).into(imageView);
                                }).show());

                            })
                            .onCancel(result -> {
                                // The user canceled the operation.
                            })
                            .start();
                });
            });
        });

        t2 = new Thread(() -> {
            int cnt = 0;
            while (true) {
                if (stfalconImageViewer.get() != null) {
                    int finalCnt = cnt;
                    runOnUiThread(() -> stfalconImageViewer.get().setCurrentPosition(finalCnt));
                    cnt++;
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t2.start();

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
                .add("id_token", LoginActivity.getIdToken()) // This is what we received in Step 5, the jwt token.
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