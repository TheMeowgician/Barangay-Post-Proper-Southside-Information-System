package com.example.barangayinformationsystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.example.barangayinformationsystem.PasswordHasher;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
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
    }    private void initializeComponents() {
        signUpTextView = findViewById(R.id.signUpTextView);
        logInButton = findViewById(R.id.logInButton);
        backImageButton = findViewById(R.id.backImageButton);
        usernameTextInputLayout = findViewById(R.id.usernameTextInputLayout);
        passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout);

        removeTextInputLayoutAnimation();
        addUnderlineToTextView();
        setupTextChangeListeners();

        backImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(LogInActivity.this, ChooseActivity.class);
            startActivity(intent);
            finish();
        });        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear previous error states
                clearErrorStates();
                
                String username = usernameTextInputLayout.getEditText().getText().toString();
                String password = passwordTextInputLayout.getEditText().getText().toString();

                boolean hasError = false;
                  if (username.isEmpty()) {
                    usernameTextInputLayout.setError("Username is required");
                    setErrorStrokeColor(usernameTextInputLayout);
                    hasError = true;
                }
                
                if (password.isEmpty()) {
                    passwordTextInputLayout.setError("Password is required");
                    setErrorStrokeColor(passwordTextInputLayout);
                    hasError = true;
                }
                
                if (!hasError) {
                    loginUser(username, password);
                }
            }
        });
    }    private void loginUser(String username, String password) {
        logInButton.setEnabled(false);
        logInButton.setText("Logging in...");          // Clear any previous error states
        clearErrorStates();// Hash the password before sending
        String hashedPassword = PasswordHasher.hashPassword(password);
        if (hashedPassword == null) {
            logInButton.setEnabled(true);
            logInButton.setText("Log In");
            passwordTextInputLayout.setError("Error processing password. Please try again.");
            setErrorStrokeColor(passwordTextInputLayout);
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

                            // Initialize first login timestamp for this user
                            initializeFirstLoginTimestamp(loginResponse.getId());

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

                            // Initialize first login timestamp for this user
                            initializeFirstLoginTimestamp(loginResponse.getId());

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
                        // Show appropriate error based on the message
                        String errorMessage = loginResponse.getMessage() != null ? 
                                loginResponse.getMessage() : "Invalid username or password";                        // Set error message under the specific field
                        if ("Username not found".equals(errorMessage)) {
                            usernameTextInputLayout.setError(errorMessage);
                            setErrorStrokeColor(usernameTextInputLayout);
                        } else if ("Invalid password".equals(errorMessage)) {
                            passwordTextInputLayout.setError(errorMessage);
                            setErrorStrokeColor(passwordTextInputLayout);
                        } else {
                            // General error if we can't determine which field is wrong
                            Toast.makeText(LogInActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
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
        Intent intent = new Intent(LogInActivity.this, TermsAndConditionsActivity.class);
        startActivity(intent);
    }
    public void back(View view) {
        Intent intent = new Intent(LogInActivity.this, ChooseActivity.class);
        startActivity(intent);
        finish();
    }

    private void addUnderlineToTextView() {
        signUpTextView.setPaintFlags(signUpTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }    private void removeTextInputLayoutAnimation() {
        usernameTextInputLayout.setHintAnimationEnabled(false);
        usernameTextInputLayout.setHintEnabled(false);

        passwordTextInputLayout.setHintAnimationEnabled(false);
        passwordTextInputLayout.setHintEnabled(false);
    }
    
    private void setupTextChangeListeners() {
        // Add text watchers to clear errors when user starts typing
        TextInputEditText usernameEditText = findViewById(R.id.usernameTextInputEditText);
        TextInputEditText passwordEditText = findViewById(R.id.passwordTextInputEditText);
        
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear error when user types
                usernameTextInputLayout.setError(null);
                // This will reset to the default selector behavior
                usernameTextInputLayout.refreshDrawableState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear error when user types
                passwordTextInputLayout.setError(null);
                // This will reset to the default selector behavior
                passwordTextInputLayout.refreshDrawableState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }    @Override
    public void onBackPressed() {
        Intent intent = new Intent(LogInActivity.this, ChooseActivity.class);
        startActivity(intent);
        finish();
    }
      /**
     * Helper method to clear all error states from input fields
     */
    private void clearErrorStates() {
        usernameTextInputLayout.setError(null);
        passwordTextInputLayout.setError(null);
        usernameTextInputLayout.refreshDrawableState();
        passwordTextInputLayout.refreshDrawableState();
    }
    
    /**
     * Helper method to set the error stroke color for TextInputLayout
     * This directly sets the box stroke color to show red only when there's an error
     */
    private void setErrorStrokeColor(TextInputLayout textInputLayout) {
        // Using reflection to access the boxStrokeErrorColor field
        try {
            java.lang.reflect.Field boxStrokeColorField = TextInputLayout.class.getDeclaredField("boxStrokeErrorColor");
            boxStrokeColorField.setAccessible(true);
            
            // Create a ColorStateList with error color
            android.content.res.ColorStateList colorStateList = 
                android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.error));
            
            boxStrokeColorField.set(textInputLayout, colorStateList);
            
            // Force the view to refresh
            textInputLayout.invalidate();
        } catch (Exception e) {
            // Fallback method if reflection fails
            textInputLayout.setBoxStrokeColor(getResources().getColor(R.color.error));
        }
    }

    /**
     * Initialize first login timestamp for this user
     */
    private void initializeFirstLoginTimestamp(int userId) {
        String firstLoginKey = "first_login_timestamp_" + userId;
        
        // Check if this is the first time this user is logging in on this device
        long firstLoginTimestamp = prefs.getLong(firstLoginKey, 0);
        
        if (firstLoginTimestamp == 0) {
            // This is the first login for this user on this device
            firstLoginTimestamp = System.currentTimeMillis();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(firstLoginKey, firstLoginTimestamp);
            editor.apply();
            
            android.util.Log.d("LogInActivity", "First login detected for user " + userId + " at timestamp: " + firstLoginTimestamp);
        } else {
            android.util.Log.d("LogInActivity", "Existing user " + userId + " first logged in at timestamp: " + firstLoginTimestamp);
        }
    }
}