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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    public static String accessToken;
    public static PhotosLibraryClient photosLibraryClient;
    private Thread t1;
    private String idToken;
    private GoogleSignInOptions gso;
    private AlbumFragment albumFragment;
    private SharedPreferencesManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize
        prefs = new SharedPreferencesManager(getApplicationContext());
        Intent intent = getIntent();
        idToken = intent.getStringExtra("idToken");

        // Toolbar customization
        Toolbar toolbar = findViewById(R.id.toolbar_main);
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
            FragmentManager transaction = getSupportFragmentManager();
            transaction.beginTransaction()
                    .replace(R.id.main_layout, albumFragment) //<---replace a view in your layout (id: container) with the newFragment
                    .addToBackStack(null)
                    .commit();
        });

        // Check if we have a refresh token, if we do then we can
        // retrieve an access token without caliing getAccessToken()
        if (prefs.retrieveString("refresh_token", "NULL").equals("NULL")) {
            getAccessToken();

        } else {
            getAccessTokenFromRefreshToken();

        }

        // Auto-refresh every 45 minutes
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2700000);
                    getAccessTokenFromRefreshToken();
                    getPhotosLibrary();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

    }

    private void getAccessTokenFromRefreshToken() {
        // Create access token builder
        UserCredentials creds = UserCredentials.newBuilder()
                .setClientId(Properties.getWebAPIKey())
                .setClientSecret(Properties.getWebSecretKey())
                .setRefreshToken(prefs.retrieveString("refresh_token", "NULL"))
                .build();
        try {
            accessToken = creds.refreshAccessToken().getTokenValue();
            t1.start();
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
                .add("code", prefs.retrieveString("code", "NULL") /*LoginActivity.getAccount().getServerAuthCode()*/) // device code.
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
                        prefs.storeString("refresh_token", jsonObject.getString("refresh_token"));
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

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
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
                            .hide(Objects.requireNonNull(getSupportFragmentManager().findFragmentByTag("gallery_fragment")))
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
                prefs.storeString("refresh_token", "NULL");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}