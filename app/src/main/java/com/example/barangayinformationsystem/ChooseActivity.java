package com.example.barangayinformationsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

public class ChooseActivity extends AppCompatActivity {

    Button logInButton;
    Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.contains("user_id")) {
            // User is logged in, redirect to HomeActivity
            Intent intent = new Intent(ChooseActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_choose);
        initializeComponents();
    }

    public void moveToLogInActivity(View view) {
        Intent intent = new Intent(ChooseActivity.this, LogInActivity.class);
        startActivity(intent);
    }

    public void moveToRegistrationActivity(View view) {
        Intent intent = new Intent(ChooseActivity.this, TermsAndConditionsActivity.class);
        startActivity(intent);
    }

    private void initializeComponents() {
        logInButton = findViewById(R.id.logInButton);
        registerButton = findViewById(R.id.registerButton);
    }
}