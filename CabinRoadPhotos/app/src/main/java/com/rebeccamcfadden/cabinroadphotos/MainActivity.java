package com.rebeccamcfadden.cabinroadphotos;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.material.button.MaterialButton;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.types.proto.Album;
import com.google.photos.types.proto.MediaItem;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.yanzhenjie.album.AlbumConfig;
import com.yanzhenjie.album.AlbumFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import io.flutter.embedding.android.FlutterFragment;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL = "slideshowChannel/mediaItems";
    private String accessToken;
    private PhotosLibraryClient photosLibraryClient;
    private Thread t1;
    private String idToken;
    private GoogleSignInOptions gso;
    private AlbumFragment albumFragment;

    private Toolbar toolbar;
    private String slideshowData;

    // tag String to represent the FlutterFragment within this Activity's FragmentManager
    public static final String TAG_FLUTTER_FRAGMENT = "slideshow_fragment";
    public FlutterFragment slideshowFragment;
    FlutterEngine flutterEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        idToken = intent.getStringExtra("idToken");
        flutterEngine = new FlutterEngine(this);

        // Toolbar customization
        toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        // Sign in Options
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Properties.getWebAPIKey())
                .requestServerAuthCode(Properties.getWebAPIKey())
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary"))
                .build();

        // Photos access thread
        t1 = new Thread(() -> {

            getPhotosLibrary();

            albumFragment = new AlbumFragment();
            albumFragment.setPhotosLibraryClient(photosLibraryClient);
            FragmentManager transaction = getSupportFragmentManager();
            transaction.beginTransaction()
                    .replace(R.id.main_layout, albumFragment) //<---replace a view in your layout (id: container) with the newFragment
                    .addToBackStack(null)
                    .commit();
        });

        // Check if we have a refresh token, if we do then we can
        // retrieve an access token without caliing getAccessToken()
        if (new SharedPreferencesManager(getApplicationContext()).retrieveString("refresh_token", "NULL").equals("NULL")) {
            getAccessToken();

        } else {

            // Create access token builder
            UserCredentials creds = UserCredentials.newBuilder()
                    .setClientId(Properties.getWebAPIKey())
                    .setClientSecret(Properties.getWebSecretKey())
                    .setRefreshToken(new SharedPreferencesManager(getApplicationContext()).retrieveString("refresh_token", "NULL"))
                    .build();
            try {
                accessToken = creds.refreshAccessToken().getTokenValue();
                t1.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                    if (jsonObject.getString("refresh_token") != null) {
                        new SharedPreferencesManager(getApplicationContext()).storeString("refresh_token", jsonObject.getString("refresh_token"));
                    }
                    if (jsonObject.get("access_token") != null) {
                        accessToken = jsonObject.get("access_token").toString();
                    }
                    Log.i("Auth", message);
                    t1.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void createSlideshowFragment(Fragment callingFragment, String albumID, String albumTitle, Iterable<MediaItem> mediaItemList) {
        JSONArray mediaItemJson = new JSONArray();
        JSONObject albumData = new JSONObject();
        try {
            albumData.put("id", albumID);
            albumData.put("title", albumTitle);
            for (MediaItem m : mediaItemList) {
                JSONObject mObj = new JSONObject();
                mObj.put("id", m.getId());
                mObj.put("description", m.getDescription());
                mObj.put("baseUrl", m.getBaseUrl());
                if (m.getMediaMetadata().hasVideo()){
                    mObj.put("type", "video");
                } else {
                    mObj.put("type", "photo");
                }
                mediaItemJson.put(mObj);
            }
            albumData.put("mediaItems", mediaItemJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String mediaItemData = albumData.toString();
        // Start executing Dart code in the FlutterEngine.
        flutterEngine.getDartExecutor().executeDartEntrypoint(
                DartExecutor.DartEntrypoint.createDefault()
        );

        // Cache the pre-warmed FlutterEngine to be used later by FlutterFragment.
        FlutterEngineCache
                .getInstance()
                .put("slideshow_engine", flutterEngine);
        MethodChannel mc = new MethodChannel(
                FlutterEngineCache
                        .getInstance().get("slideshow_engine")
                        .getDartExecutor()
                        .getBinaryMessenger(),
                CHANNEL);
        mc.setMethodCallHandler((methodCall, result) ->
                {
                    if (methodCall.method.equals("test")) {
                        result.success("Hai from android and this is the data you sent me " + methodCall.argument("data"));
//Accessing data sent from flutter
                    } else if (methodCall.method.equals("getMediaItems")) {
                        result.success(mediaItemData);
                        Log.d("mediaDisplay","getMediaItems called - sent: " + mediaItemData);
                    } else {
                        Log.i("new method came", methodCall.method);
                    }
                }
        );

        slideshowFragment = (FlutterFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_FLUTTER_FRAGMENT);
        slideshowFragment = FlutterFragment.createDefault();
        getSupportFragmentManager()
                .beginTransaction()
                .hide(callingFragment)
                .add(
                        R.id.main_layout,
                        slideshowFragment,
                        TAG_FLUTTER_FRAGMENT
                )
                .commit();
//        getWindow().addFlags(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        } else if (slideshowFragment != null) {
           slideshowFragment.onBackPressed();
           slideshowFragment = null;
        } else {
            super.onBackPressed();
        }
    }

    // Overflow Menu overrides ---------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                SettingsFragment settingsFragment = new SettingsFragment();
                FragmentManager transaction = getSupportFragmentManager();

                if (getSupportFragmentManager().findFragmentByTag("gallery_fragment") != null) {
                    transaction.beginTransaction()
//                        .hide(getSupportFragmentManager().getFragments().get(getSupportFragmentManager().getFragments().size() - 2))
                            .hide(albumFragment)
                            .hide(getSupportFragmentManager().findFragmentByTag("gallery_fragment"))
                            .replace(R.id.main_layout, settingsFragment, "settings_fragment") //<---replace a view in your layout (id: container) with the newFragment
                            .addToBackStack(null)
                            .commit();
                } else {
                    transaction.beginTransaction()
//                        .hide(getSupportFragmentManager().getFragments().get(getSupportFragmentManager().getFragments().size() - 2))
                            .hide(albumFragment)
                            .add(R.id.main_layout, settingsFragment, "settings_fragment") //<---replace a view in your layout (id: container) with the newFragment
                            .addToBackStack(null)
                            .commit();
                }

                return true;
            case R.id.sign_out:
                // Sign out
                GoogleSignIn.getClient(this, gso).signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        if (slideshowFragment != null) slideshowFragment.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );
    }

    @Override
    public void onUserLeaveHint() {
        if (slideshowFragment != null) slideshowFragment.onUserLeaveHint();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (slideshowFragment != null) slideshowFragment.onTrimMemory(level);
    }
}