package com.example.barangayinformationsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BarangayCertificationFormActivity extends AppCompatActivity {

    ImageButton barangay_certification_form_back_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barangay_certification_form);
        initializeComponents();
    }

    private void initializeComponents() {
        barangay_certification_form_back_button = findViewById(R.id.barangay_certification_form_back_button);
    }

    public void back(View view) {
        finish();
    }
}