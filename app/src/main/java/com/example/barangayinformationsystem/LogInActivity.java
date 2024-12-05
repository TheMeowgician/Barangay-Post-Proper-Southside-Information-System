package com.example.barangayinformationsystem;

import android.app.AlertDialog;
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
import com.example.barangayinformationsystem.PasswordHasher;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogInActivity extends AppCompatActivity {

    TextView signUpTextView;
    Button logInButton;
    ImageButton backImageButton;
    TextInputLayout usernameTextInputLayout;
    TextInputLayout passwordTextInputLayout;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            redirectToHome();
            return;
        }

        setContentView(R.layout.activity_log_in);
        initializeComponents();
    }

    private boolean isUserLoggedIn() {
        // Check if user_id exists in SharedPreferences
        return prefs.contains("user_id");
    }

    private void redirectToHome() {
        Intent intent = new Intent(LogInActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initializeComponents() {
        signUpTextView = findViewById(R.id.signUpTextView);
        logInButton = findViewById(R.id.logInButton);
        backImageButton = findViewById(R.id.backImageButton);
        usernameTextInputLayout = findViewById(R.id.usernameTextInputLayout);
        passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout);

        removeTextInputLayoutAnimation();
        addUnderlineToTextView();

        backImageButton.setOnClickListener(v -> finish());

        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameTextInputLayout.getEditText().getText().toString();
                String password = passwordTextInputLayout.getEditText().getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LogInActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    loginUser(username, password);
                }
            }
        });
    }

    private void loginUser(String username, String password) {
        logInButton.setEnabled(false);
        logInButton.setText("Logging in...");

        // Hash the password before sending
        String hashedPassword = PasswordHasher.hashPassword(password);
        if (hashedPassword == null) {
            logInButton.setEnabled(true);
            logInButton.setText("Log In");
            Toast.makeText(LogInActivity.this, "Error processing password. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        Call<LoginResponse> call = apiService.loginUser(username, hashedPassword);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                logInButton.setEnabled(true);
                logInButton.setText("Log In");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if ("success".equals(loginResponse.getStatus())) {
                        // Handle verified users
                        if ("verified".equals(loginResponse.getAccountStatus())) {
                            // Save login state and user ID
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("user_id", loginResponse.getId());
                            editor.apply();

                            Intent homeIntent = new Intent(LogInActivity.this, HomeActivity.class);
                            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            SuccessDialog.showSuccess(LogInActivity.this, "You have successfully logged in", homeIntent);
                        }
                        // Handle pending users
                        else if ("pending".equals(loginResponse.getAccountStatus())) {
                            // Save user ID but redirect to pending status
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("user_id", loginResponse.getId());
                            editor.apply();

                            Intent pendingIntent = new Intent(LogInActivity.this, PendingStatusActivity.class);
                            pendingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(pendingIntent);
                            finish();
                        }
                        // Handle rejected users
                        else if ("rejected".equals(loginResponse.getAccountStatus())) {
                            Toast.makeText(LogInActivity.this, "Your account has been rejected.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(LogInActivity.this, loginResponse.getMessage() != null ?
                                        loginResponse.getMessage() : "Invalid username or password",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LogInActivity.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                logInButton.setEnabled(true);
                logInButton.setText("Log In");
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
        signUpTextView.setPaintFlags(signUpTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    private void removeTextInputLayoutAnimation() {
        usernameTextInputLayout.setHintAnimationEnabled(false);
        usernameTextInputLayout.setHintEnabled(false);

        passwordTextInputLayout.setHintAnimationEnabled(false);
        passwordTextInputLayout.setHintEnabled(false);
    }
}