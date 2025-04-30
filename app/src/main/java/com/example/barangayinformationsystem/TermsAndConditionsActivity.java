package com.example.barangayinformationsystem;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.barangayinformationsystem.utils.PrivacyPolicy;

public class TermsAndConditionsActivity extends AppCompatActivity {

    private Button acceptButton;
    private Button declineButton;
    private TextView termsTextView;
    private TextView privacyPolicyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_and_conditions);

        // Initialize views
        acceptButton = findViewById(R.id.acceptButton);
        declineButton = findViewById(R.id.declineButton);
        termsTextView = findViewById(R.id.termsTextView);
        privacyPolicyTextView = findViewById(R.id.privacyPolicyTextView);
        
        // Make text views scrollable
        if (termsTextView != null) {
            termsTextView.setMovementMethod(new ScrollingMovementMethod());
        }
        if (privacyPolicyTextView != null) {
            privacyPolicyTextView.setMovementMethod(new ScrollingMovementMethod());
            
            // Set privacy policy content with HTML formatting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                privacyPolicyTextView.setText(Html.fromHtml(PrivacyPolicy.PRIVACY_POLICY_TEXT, Html.FROM_HTML_MODE_COMPACT));
            } else {
                privacyPolicyTextView.setText(Html.fromHtml(PrivacyPolicy.PRIVACY_POLICY_TEXT));
            }
        }

        // Set button click listeners
        acceptButton.setOnClickListener(v -> {
            Intent intent = new Intent(TermsAndConditionsActivity.this, RegistrationActivity.class);
            startActivity(intent);
            finish(); // This ensures user can't go back to terms screen using back button
        });
        
        if (declineButton != null) {
            declineButton.setOnClickListener(v -> {
                Toast.makeText(this, "You must accept the Terms and Privacy Policy to register", Toast.LENGTH_LONG).show();
                finish(); // Return to previous screen
            });
        }
    }
}