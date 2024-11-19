package com.example.barangayinformationsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogInActivity extends AppCompatActivity {

    TextView forgotPasswordTextView;
    TextView signUpTextView;
    Button logInButton;
    ImageButton backImageButton;
    TextInputLayout usernameTextInputLayout;
    TextInputLayout passwordTextInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        initializeComponents();
    }

    private void initializeComponents() {
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        signUpTextView = findViewById(R.id.signUpTextView);
        logInButton = findViewById(R.id.logInButton);
        backImageButton = findViewById(R.id.backImageButton);
        usernameTextInputLayout = findViewById(R.id.usernameTextInputLayout);
        passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout);

        removeTextInputLayoutAnimation();
        addUnderlineToTextView();

        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameTextInputLayout.getEditText().getText().toString();
                String password = passwordTextInputLayout.getEditText().getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LogInActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    loginUser(username, password); // Call the method to send credentials
                }
            }
        });
    }

    private void loginUser(String username, String password) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<LoginResponse> call = apiService.loginUser(username, password);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if ("success".equals(loginResponse.getStatus())) {
                        // Save user ID in SharedPreferences for session management
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LogInActivity.this);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("user_id", loginResponse.getId()); // Save user ID
                        editor.apply(); // Commit changes

                        // Navigate to HomeActivity (or ProfileActivity)
                        Intent intent = new Intent(LogInActivity.this, HomeActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(LogInActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LogInActivity.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LogInActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void goToHomeActivity(View view) {
        Intent intent = new Intent(LogInActivity.this, HomeActivity.class);
        startActivity(intent);
    }

    public void goToRegisterActivity(View view) {
        Intent intent = new Intent(LogInActivity.this, RegistrationActivity.class);
        startActivity(intent);
    }

    public void back(View view) {
        finish();
    }

    private void addUnderlineToTextView() {
        forgotPasswordTextView.setPaintFlags(forgotPasswordTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        signUpTextView.setPaintFlags(signUpTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    private void removeTextInputLayoutAnimation() {
        usernameTextInputLayout.setHintAnimationEnabled(false);
        usernameTextInputLayout.setHintEnabled(false);

        passwordTextInputLayout.setHintAnimationEnabled(false);
        passwordTextInputLayout.setHintEnabled(false);
    }
}
