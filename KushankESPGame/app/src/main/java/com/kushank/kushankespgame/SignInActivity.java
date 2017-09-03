package com.kushank.kushankespgame;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    boolean isSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        isSignIn = getIntent().getBooleanExtra(MainActivity.BOOL_IS_SIGN_IN, true);

        mAuth = FirebaseAuth.getInstance();

        //when the user will login, send him to TasksActivity.
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    //User is signed in
                    startActivity(new Intent(SignInActivity.this, TasksActivity.class));
                    finish();
                }
            }
        };

        final ProgressBar pbLogin = (ProgressBar) findViewById(R.id.progressBar);
        pbLogin.setVisibility(View.GONE);

        final EditText etEmail, etPass;
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPass = (EditText) findViewById(R.id.etPassword);

        final Button bSubmit;
        bSubmit = (Button) findViewById(R.id.bSignIn);

        // if the user had come to signin, set the button to read sign in, else create account.
        if(isSignIn)
            bSubmit.setText("Sign In");
        else
            bSubmit.setText("Create Account");

        //on click of the button.
        bSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard();
                String email, password;
                email = etEmail.getText().toString().trim();
                password = etPass.getText().toString().trim();
                if(email.equals("")) {
                    etEmail.setError("It is required");
                }else if(password.equals("")) {
                    etPass.setError("It is required");
                }else{
                    pbLogin.setVisibility(View.VISIBLE);
                    //if the user had come to login, verify an provide a login.
                    if(isSignIn) {
                        mAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (!task.isSuccessful()) {
                                            // If sign in fails, display a message to the user.
                                            Toast.makeText(SignInActivity.this, "Login Unsuccessful", Toast.LENGTH_SHORT).show();
                                        }
                                        pbLogin.setVisibility(View.GONE);
                                    }
                                });
                    }
                    //if the user had come to create account, verify and also make a login.
                    else {
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (!task.isSuccessful()) {
                                            Toast.makeText(SignInActivity.this, "Account Creation Unsuccessful", Toast.LENGTH_SHORT).show();
                                        }
                                        pbLogin.setVisibility(View.GONE);
                                    }
                                });
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAuth.removeAuthStateListener(mAuthListener);
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            finish();
    }

    //hideKeyboard, when the user clicks a button.
    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(getCurrentFocus()!=null)
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

}
