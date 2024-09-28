package com.example.barangayinformationsystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ButtonBarLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChooseActivity extends AppCompatActivity {

    Button logInButton;
    Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        initializeComponents();
    }

    public void moveToLogInActivity(View view) {//this code will take the user to Log In Activity
        Intent intent = new Intent(ChooseActivity.this, LogInActivity.class);
        startActivity(intent);
    }

    public void moveToRegistrationActivity(View view) {//This code will take the user to Registration Activity
        Intent intent = new Intent(ChooseActivity.this, RegistrationActivity.class);
        startActivity(intent);
    }

    private void initializeComponents() {

        logInButton = findViewById(R.id.logInButton);
        registerButton = findViewById(R.id.registerButton);

    }
}