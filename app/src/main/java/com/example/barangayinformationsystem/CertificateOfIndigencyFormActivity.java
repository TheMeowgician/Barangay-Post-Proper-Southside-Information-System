package com.example.barangayinformationsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CertificateOfIndigencyFormActivity extends AppCompatActivity {

    ImageButton certificate_of_indigency_form_back_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate_of_indigency_form);
        initializeComponents();
    }

    private void initializeComponents() {
        certificate_of_indigency_form_back_button = findViewById(R.id.certificate_of_indigency_form_back_button);
    }

    public void back(View view) {
        finish();
    }

}