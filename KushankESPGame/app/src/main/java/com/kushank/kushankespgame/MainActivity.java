package com.kushank.kushankespgame;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String BOOL_IS_SIGN_IN = "bIsSignIn";

    static boolean calledAlready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // to set persistence.
        if (!calledAlready) {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            calledAlready = true;
        }

        // if the user is logged in, send him to the main screen.
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null)
            //User is signed in
            startActivity(new Intent(MainActivity.this, SignInActivity.class));

        findViewById(R.id.bSignIn).setOnClickListener(this);
        findViewById(R.id.bCreateAccount).setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        Intent i = new Intent(this, SignInActivity.class);
        switch (view.getId()) {
            case R.id.bSignIn:
                // send the data with the intent for the activity to know which button is clicked.
                i.putExtra(BOOL_IS_SIGN_IN, true);
                startActivity(i);
                break;
            case R.id.bCreateAccount:
                // send the data with the intent for the activity to know which button is clicked.
                i.putExtra(BOOL_IS_SIGN_IN, false);
                startActivity(i);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            finish();
    }
}
