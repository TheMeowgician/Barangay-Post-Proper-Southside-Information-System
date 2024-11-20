package com.example.barangayinformationsystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationActivity extends AppCompatActivity {

    private ImageButton backImageButton;
    private AppCompatButton btnRegister;
    private RadioGroup genderRadioGroup;
    private RadioButton maleRadioButton;
    private RadioButton femaleRadioButton;

    private TextInputEditText birthDateTextInputEditText;
    private TextInputEditText firstNameTextInputEditText;
    private TextInputEditText lastNameTextInputEditText;
    private TextInputEditText usernameTextInputEditText;
    private TextInputEditText passwordTextInputEditText;
    private TextInputEditText confirmPasswordTextInputEditText;
    private TextInputEditText ageTextInputEditText;
    private TextInputEditText houseNumberTextInputEditText;
    private TextInputEditText zoneTextInputEditText;
    private TextInputEditText streetTextInputEditText;

    private TextInputLayout birthdateTextInputLayout;
    private TextInputLayout firstNameTextInputLayout;
    private TextInputLayout lastNameTextInputLayout;
    private TextInputLayout usernameTextInputLayout;
    private TextInputLayout passwordTextInputLayout;
    private TextInputLayout confirmPasswordTextInputLayout;
    private TextInputLayout ageTextInputLayout;
    private TextInputLayout houseNumberTextInputLayout;
    private TextInputLayout zoneTextInputLayout;
    private TextInputLayout streetTextInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        if (!initializeComponents()) {
            Toast.makeText(this, "Error initializing components", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupClickListeners();
    }

    private boolean initializeComponents() {
        try {
            // Initialize all components
            backImageButton = findViewById(R.id.backImageButton);
            btnRegister = findViewById(R.id.btnRegister);
            genderRadioGroup = findViewById(R.id.genderRadioGroup);
            maleRadioButton = findViewById(R.id.maleRadioButton);
            femaleRadioButton = findViewById(R.id.femaleRadioButton);

            // Initialize TextInputEditText components
            birthDateTextInputEditText = findViewById(R.id.birthDateTextInputEditText);
            firstNameTextInputEditText = findViewById(R.id.firstNameTextInputEditText);
            lastNameTextInputEditText = findViewById(R.id.lastNameTextInputEditText);
            usernameTextInputEditText = findViewById(R.id.usernameTextInputEditText);
            passwordTextInputEditText = findViewById(R.id.passwordTextInputEditText);
            confirmPasswordTextInputEditText = findViewById(R.id.confirmPasswordTextInputEditText);
            ageTextInputEditText = findViewById(R.id.ageTextInputEditText);
            houseNumberTextInputEditText = findViewById(R.id.houseNumberTextInputEditText);
            zoneTextInputEditText = findViewById(R.id.zoneTextInputEditText);
            streetTextInputEditText = findViewById(R.id.streetTextInputEditText);

            // Initialize TextInputLayout components
            birthdateTextInputLayout = findViewById(R.id.birthDateTextInputLayout);
            firstNameTextInputLayout = findViewById(R.id.firstNameTextInputLayout);
            lastNameTextInputLayout = findViewById(R.id.lastNameTextInputLayout);
            usernameTextInputLayout = findViewById(R.id.usernameTextInputLayout);
            passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout);
            confirmPasswordTextInputLayout = findViewById(R.id.confirmPasswordTextInputLayout);
            ageTextInputLayout = findViewById(R.id.ageTextInputLayout);
            houseNumberTextInputLayout = findViewById(R.id.houseNumberTextInputLayout);
            zoneTextInputLayout = findViewById(R.id.zoneTextInputLayout);
            streetTextInputLayout = findViewById(R.id.streetTextInputLayout);

            // Verify that all required views were found
            if (anyViewsNull()) {
                return false;
            }

            removeTextInputLayoutAnimation();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean anyViewsNull() {
        return backImageButton == null || btnRegister == null ||
                genderRadioGroup == null || maleRadioButton == null || femaleRadioButton == null ||
                birthDateTextInputEditText == null || firstNameTextInputEditText == null ||
                lastNameTextInputEditText == null || usernameTextInputEditText == null ||
                passwordTextInputEditText == null || confirmPasswordTextInputEditText == null ||
                ageTextInputEditText == null || houseNumberTextInputEditText == null ||
                zoneTextInputEditText == null || streetTextInputEditText == null ||
                birthdateTextInputLayout == null || firstNameTextInputLayout == null ||
                lastNameTextInputLayout == null || usernameTextInputLayout == null ||
                passwordTextInputLayout == null || confirmPasswordTextInputLayout == null ||
                ageTextInputLayout == null || houseNumberTextInputLayout == null ||
                zoneTextInputLayout == null || streetTextInputLayout == null;
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });

        birthDateTextInputEditText.setOnClickListener(this::openDialog);
        backImageButton.setOnClickListener(v -> finish());
    }

    public void openDialog(View view) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> {
                    String formattedDate = String.format("%d-%02d-%02d", year, month + 1, day);
                    birthDateTextInputEditText.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Clear all previous errors
        clearAllErrors();

        // Validate required fields
        if (isEmpty(firstNameTextInputEditText)) {
            firstNameTextInputLayout.setError("First name is required");
            isValid = false;
        }

        if (isEmpty(lastNameTextInputEditText)) {
            lastNameTextInputLayout.setError("Last name is required");
            isValid = false;
        }

        if (isEmpty(usernameTextInputEditText)) {
            usernameTextInputLayout.setError("Username is required");
            isValid = false;
        }

        if (isEmpty(passwordTextInputEditText)) {
            passwordTextInputLayout.setError("Password is required");
            isValid = false;
        }

        // Validate password match
        String password = passwordTextInputEditText.getText().toString();
        String confirmPassword = confirmPasswordTextInputEditText.getText().toString();
        if (!password.equals(confirmPassword)) {
            confirmPasswordTextInputLayout.setError("Passwords don't match");
            isValid = false;
        }

        // Validate gender selection
        if (genderRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate age
        if (isEmpty(ageTextInputEditText)) {
            ageTextInputLayout.setError("Age is required");
            isValid = false;
        } else {
            try {
                int age = Integer.parseInt(ageTextInputEditText.getText().toString().trim());
                if (age <= 0 || age > 150) {
                    ageTextInputLayout.setError("Please enter a valid age");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                ageTextInputLayout.setError("Please enter a valid number");
                isValid = false;
            }
        }

        // Validate other required fields
        if (isEmpty(birthDateTextInputEditText)) {
            birthdateTextInputLayout.setError("Birth date is required");
            isValid = false;
        }

        if (isEmpty(houseNumberTextInputEditText)) {
            houseNumberTextInputLayout.setError("House number is required");
            isValid = false;
        }

        if (isEmpty(zoneTextInputEditText)) {
            zoneTextInputLayout.setError("Zone is required");
            isValid = false;
        }

        if (isEmpty(streetTextInputEditText)) {
            streetTextInputLayout.setError("Street is required");
            isValid = false;
        }

        return isValid;
    }

    private boolean isEmpty(TextInputEditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    private void clearAllErrors() {
        firstNameTextInputLayout.setError(null);
        lastNameTextInputLayout.setError(null);
        usernameTextInputLayout.setError(null);
        passwordTextInputLayout.setError(null);
        confirmPasswordTextInputLayout.setError(null);
        birthdateTextInputLayout.setError(null);
        ageTextInputLayout.setError(null);
        houseNumberTextInputLayout.setError(null);
        zoneTextInputLayout.setError(null);
        streetTextInputLayout.setError(null);
    }

    private void registerUser() {
        try {
            String firstName = firstNameTextInputEditText.getText().toString().trim();
            String lastName = lastNameTextInputEditText.getText().toString().trim();
            String username = usernameTextInputEditText.getText().toString().trim();
            String password = passwordTextInputEditText.getText().toString().trim();
            String birthDate = birthDateTextInputEditText.getText().toString().trim();
            int age = Integer.parseInt(ageTextInputEditText.getText().toString().trim());
            String houseNumber = houseNumberTextInputEditText.getText().toString().trim();
            String zone = zoneTextInputEditText.getText().toString().trim();
            String street = streetTextInputEditText.getText().toString().trim();
            String gender = maleRadioButton.isChecked() ? "Male" : "Female";

            ApiService apiService = RetrofitClient.getApiService();
            Call<RegistrationResponse> call = apiService.registerUser(
                    firstName, lastName, username, password,
                    age, birthDate, houseNumber, zone, street
            );

            call.enqueue(new Callback<RegistrationResponse>() {
                @Override
                public void onResponse(Call<RegistrationResponse> call, Response<RegistrationResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().isSuccess()) {
                            Toast.makeText(RegistrationActivity.this,
                                    "Registration successful!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(RegistrationActivity.this, LogInActivity.class);
                            intent.putExtra("username", username);
                            intent.putExtra("firstName", firstName);
                            intent.putExtra("lastName", lastName);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegistrationActivity.this,
                                    response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(RegistrationActivity.this,
                                "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<RegistrationResponse> call, Throwable t) {
                    Toast.makeText(RegistrationActivity.this,
                            "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error processing registration: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void removeTextInputLayoutAnimation() {
        TextInputLayout[] layouts = {
                firstNameTextInputLayout, lastNameTextInputLayout,
                usernameTextInputLayout, birthdateTextInputLayout,
                passwordTextInputLayout, confirmPasswordTextInputLayout,
                ageTextInputLayout, houseNumberTextInputLayout,
                zoneTextInputLayout, streetTextInputLayout
        };

        for (TextInputLayout layout : layouts) {
            layout.setHintAnimationEnabled(false);
            layout.setHintEnabled(false);
        }
    }
}