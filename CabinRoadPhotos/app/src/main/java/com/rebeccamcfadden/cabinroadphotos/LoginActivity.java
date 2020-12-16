package com.rebeccamcfadden.cabinroadphotos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Create Sign in options for photos access and device code and idtoken
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Properties.getWebAPIKey())
                .requestServerAuthCode(Properties.getWebAPIKey())
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary"))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
//            Don't reach this page

        }

        // On Click Listener for login
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                account = task.getResult(ApiException.class);
                assert account != null;

                // Add to SharedPreferences
                new SharedPreferencesManager(this).storeString("code", account.getServerAuthCode());
                Log.d("sign_in", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());

            } catch (ApiException e) {
                // Google Sign In failed
                Log.w("sign_in", "Google sign in failed", e);
                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this, R.style.MyDialogTheme);
                dialogBuilder.setTitle("Authentication Failed");
                dialogBuilder.setMessage("Please Try Again");
                dialogBuilder.setNeutralButton("Okay", (dialogInterface, i) -> dialogInterface.dismiss());
                dialogBuilder.create().show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success

                        // Get Id
                        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
                        assert mUser != null;
                        mUser.getIdToken(true)
                                .addOnCompleteListener(task2 -> {

                                    // Sign in completes, create intent
                                    if (task2.isSuccessful()) {
                                        Intent intent = new Intent(this, MainActivity.class);

                                        // Add idToken to intent then start
                                        intent.putExtra("idToken", Objects.requireNonNull(task2.getResult()).getToken());
                                        startActivity(intent);
                                        finish();
                                    }
                                });

                    } else {
                        // If sign in fails, display a message to the user.
                        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this, R.style.MyDialogTheme);
                        dialogBuilder.setTitle("Authentication Failed");
                        dialogBuilder.setMessage("Please Try Again");
                        dialogBuilder.setNeutralButton("Okay", (dialogInterface, i) -> dialogInterface.dismiss());
                        dialogBuilder.create().show();

                    }

                });
    }
}