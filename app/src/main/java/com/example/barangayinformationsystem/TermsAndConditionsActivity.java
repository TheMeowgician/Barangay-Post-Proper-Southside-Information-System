package com.example.barangayinformationsystem;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class TermsAndConditionsActivity extends AppCompatActivity {

    private Button acceptButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_and_conditions);

        acceptButton = findViewById(R.id.acceptButton);
        acceptButton.setOnClickListener(v -> {
            Intent intent = new Intent(TermsAndConditionsActivity.this, RegistrationActivity.class);
            startActivity(intent);
            finish(); // This ensures user can't go back to terms screen using back button
        });
    }
}