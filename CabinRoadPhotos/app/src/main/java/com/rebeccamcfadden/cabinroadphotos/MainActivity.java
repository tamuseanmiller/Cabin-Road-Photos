package com.rebeccamcfadden.cabinroadphotos;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static String accessToken;
    private static PhotosLibraryClient photosLibraryClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Photos access thread
        Thread t1 = new Thread(() -> {
            getAccessToken();
            getPhotosLibrary();
        });
        t1.start();

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
                .add("code", new SharedPreferencesManager(getApplicationContext()).retrieveString("code", "NULL")) // device code.
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

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}