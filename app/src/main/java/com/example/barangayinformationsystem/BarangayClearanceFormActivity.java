package com.example.barangayinformationsystem;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BarangayClearanceFormActivity extends AppCompatActivity {

    ImageButton barangay_clearance_form_back_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barangay_clearance_form);
        initializeComponents();
    }

    private void initializeComponents() {

        barangay_clearance_form_back_button = (ImageButton) findViewById(R.id.barangay_clearance_form_back_button);

    }

    public void back(View view) {
        finish();
    }
}