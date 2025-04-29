package com.example.barangayinformationsystem;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BarangayCertificationFormActivity extends AppCompatActivity {
    private static final String TAG = "CertificationFormActivity";
    private static final String DOCUMENT_TYPE = "Barangay Certification";

    // UI Components
    private TextInputEditText nameInput;
    private TextInputEditText aliasInput;
    private TextInputEditText ageInput;
    private TextInputEditText birthdayInput;
    private TextInputEditText addressInput;
    private TextInputEditText citizenshipInput;
    private TextInputEditText lengthOfStayInput;
    private TextInputEditText tinInput;
    private TextInputEditText ctcInput;
    private TextInputEditText purposeInput;
    private TextInputEditText occupationInput;
    private TextInputEditText placeOfBirthInput;
    private RadioGroup genderRadioGroup;
    private RadioGroup civilStatusRadioGroup;
    private Button submitButton;
    private ImageButton backButton;
    private TextInputLayout birthdayInputLayout;

    // Utility variables
    private Calendar calendar;
    private ApiService apiService;
    private ProgressDialog progressDialog;
    private int userId;
    private SimpleDateFormat displayDateFormat;
    private SimpleDateFormat databaseDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barangay_certification_form);

        initializeVariables();
        initializeViews();
        setupListeners();
        loadUserData();
    }

    private void initializeVariables() {
        calendar = Calendar.getInstance();
        apiService = RetrofitClient.getApiService();
        displayDateFormat = new SimpleDateFormat("MM-dd-yy", Locale.US);
        databaseDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);

        // Get userId from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        userId = prefs.getInt("user_id", -1);
    }

    private void initializeViews() {
        // Initialize all TextInputEditText fields
        nameInput = findViewById(R.id.barangay_certification_form_name_textInputEditText);
        aliasInput = findViewById(R.id.barangay_certification_form_alias_textInputEditText);
        ageInput = findViewById(R.id.barangay_certification_form_age_textInputEditText);
        birthdayInput = findViewById(R.id.barangay_certification_form_date_of_birth_textInputEditText);
        addressInput = findViewById(R.id.barangay_certification_form_address_textInputEditText);
        citizenshipInput = findViewById(R.id.barangay_certification_form_citizenship_textInputEditText);
        lengthOfStayInput = findViewById(R.id.barangay_certification_form_length_of_stay_textInputEditText);
        tinInput = findViewById(R.id.barangay_certification_form_tin_textInputEditText);
        ctcInput = findViewById(R.id.barangay_certification_form_ctc_textInputEditText);
        purposeInput = findViewById(R.id.barangay_certification_form_purpose_textInputEditText);
        occupationInput = findViewById(R.id.barangay_certification_form_occupation_textInputEditText);
        placeOfBirthInput = findViewById(R.id.barangay_certification_form_place_of_birth_textInputEditText);

        // Initialize RadioGroups
        genderRadioGroup = findViewById(R.id.barangay_certification_form_gender_radioGroup);
        civilStatusRadioGroup = findViewById(R.id.barangay_certification_form_civil_status_radioGroup);

        // Initialize Buttons and Layouts
        submitButton = findViewById(R.id.barangay_certification_form_submit_button);
        backButton = findViewById(R.id.barangay_certification_form_back_button);
        birthdayInputLayout = findViewById(R.id.barangay_certification_form_date_of_birth_textInputLayout);
    }

    private void setupListeners() {
        birthdayInputLayout.setEndIconOnClickListener(v -> showDatePicker());
        submitButton.setOnClickListener(v -> submitForm());
        backButton.setOnClickListener(v -> finish());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String dateStr = displayDateFormat.format(calendar.getTime());
                    birthdayInput.setText(dateStr);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadUserData() {
        if (userId == -1) {
            showError("User ID not found. Please log in again.");
            finish();
            return;
        }

        progressDialog.setMessage("Fetching user details...");
        progressDialog.show();

        Call<UserDetailsResponse> call = apiService.getUserDetails(userId);
        call.enqueue(new Callback<UserDetailsResponse>() {
            @Override
            public void onResponse(Call<UserDetailsResponse> call, Response<UserDetailsResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    UserDetailsResponse.User user = response.body().getUser();
                    if (user != null) {
                        populateUserDetails(user);
                    }
                } else {
                    showError("Error fetching user details");
                }
            }

            @Override
            public void onFailure(Call<UserDetailsResponse> call, Throwable t) {
                progressDialog.dismiss();
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void populateUserDetails(UserDetailsResponse.User user) {
        try {
            // Set name
            String fullName = String.format("%s %s", user.getFirstName(), user.getLastName());
            nameInput.setText(fullName);

            // Set age
            ageInput.setText(String.valueOf(user.getAge()));

            // Set birthday
            if (!TextUtils.isEmpty(user.getDateOfBirth())) {
                try {
                    Date date = databaseDateFormat.parse(user.getDateOfBirth());
                    if (date != null) {
                        birthdayInput.setText(displayDateFormat.format(date));
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date: " + e.getMessage());
                }
            }

            // Set address - using the new formatted address method
            String formattedAddress = user.getFormattedAddress();
            if (!TextUtils.isEmpty(formattedAddress)) {
                addressInput.setText(formattedAddress);
            } else {
                // Fallback to original address or empty string if everything fails
                addressInput.setText(user.getAddress() != null ? user.getAddress() : "");
            }

            // Set gender
            if (user.getGender() != null) {
                int radioButtonId = user.getGender().equalsIgnoreCase("male") ?
                        R.id.barangay_certification_form_male_radiobutton :
                        R.id.barangay_certification_form_female_radiobutton;
                genderRadioGroup.check(radioButtonId);
            }

            // Disable pre-filled fields
            nameInput.setEnabled(false);
            ageInput.setEnabled(false);
            birthdayInput.setEnabled(false);
            addressInput.setEnabled(false);
            genderRadioGroup.setEnabled(false);

        } catch (Exception e) {
            Log.e(TAG, "Error populating user details: " + e.getMessage());
            showError("Error loading user details");
        }
    }

    private void submitForm() {
        if (!validateInputs()) {
            return;
        }

        progressDialog.setMessage("Submitting request...");
        progressDialog.show();

        try {
            String name = Objects.requireNonNull(nameInput.getText()).toString().trim();
            String alias = Objects.requireNonNull(aliasInput.getText()).toString().trim();
            int age = Integer.parseInt(Objects.requireNonNull(ageInput.getText()).toString().trim());
            String address = Objects.requireNonNull(addressInput.getText()).toString().trim();
            String tin = Objects.requireNonNull(tinInput.getText()).toString().trim();
            String ctc = Objects.requireNonNull(ctcInput.getText()).toString().trim();
            String citizenship = Objects.requireNonNull(citizenshipInput.getText()).toString().trim();
            int lengthOfStay = Integer.parseInt(Objects.requireNonNull(lengthOfStayInput.getText()).toString().trim());
            String purpose = Objects.requireNonNull(purposeInput.getText()).toString().trim();
            String placeOfBirth = Objects.requireNonNull(placeOfBirthInput.getText()).toString().trim();
            String occupation = Objects.requireNonNull(occupationInput.getText()).toString().trim();
            String birthday = Objects.requireNonNull(birthdayInput.getText()).toString().trim();

            RadioButton selectedGenderButton = findViewById(genderRadioGroup.getCheckedRadioButtonId());
            RadioButton selectedCivilStatusButton = findViewById(civilStatusRadioGroup.getCheckedRadioButtonId());

            String gender = selectedGenderButton.getText().toString();
            String civilStatus = selectedCivilStatusButton.getText().toString();

            Call<DocumentRequestResponse> call = apiService.submitDocumentRequest(
                    userId,
                    DOCUMENT_TYPE,
                    name,
                    address,
                    tin,
                    ctc,
                    alias,
                    age,
                    birthday,
                    placeOfBirth,
                    occupation,
                    lengthOfStay,
                    citizenship,
                    gender,
                    civilStatus,
                    purpose,
                    1
            );

            call.enqueue(new Callback<DocumentRequestResponse>() {
                @Override
                public void onResponse(Call<DocumentRequestResponse> call, Response<DocumentRequestResponse> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful() && response.body() != null) {
                        DocumentRequestResponse body = response.body();
                        if (body.isSuccess()) {
                            proceedToUploadRequirements(body.getRequestId());
                        } else {
                            showError(body.getMessage());
                        }
                    } else {
                        showError("Error submitting request");
                    }
                }

                @Override
                public void onFailure(Call<DocumentRequestResponse> call, Throwable t) {
                    progressDialog.dismiss();
                    showError("Network error: " + t.getMessage());
                }
            });

        } catch (NumberFormatException e) {
            progressDialog.dismiss();
            showError("Please enter valid numbers for age and length of stay");
        } catch (Exception e) {
            progressDialog.dismiss();
            showError("Error submitting form: " + e.getMessage());
        }
    }

    private boolean validateInputs() {
        if (isEmpty(tinInput)) {
            showError("Please enter TIN number");
            return false;
        }
        if (isEmpty(ctcInput)) {
            showError("Please enter CTC number");
            return false;
        }
        if (isEmpty(lengthOfStayInput)) {
            showError("Please enter length of stay");
            return false;
        }
        if (isEmpty(citizenshipInput)) {
            showError("Please enter citizenship");
            return false;
        }
        if (isEmpty(purposeInput)) {
            showError("Please enter purpose");
            return false;
        }
        if (isEmpty(placeOfBirthInput)) {
            showError("Please enter place of birth");
            return false;
        }
        if (isEmpty(occupationInput)) {
            showError("Please enter occupation");
            return false;
        }
        if (civilStatusRadioGroup.getCheckedRadioButtonId() == -1) {
            showError("Please select civil status");
            return false;
        }

        // Validate TIN and CTC format (12 digits)
        String tin = Objects.requireNonNull(tinInput.getText()).toString().trim();
        String ctc = Objects.requireNonNull(ctcInput.getText()).toString().trim();

        if (!tin.matches("\\d{12}")) {
            showError("TIN number must be 12 digits");
            return false;
        }
        if (!ctc.matches("\\d{12}")) {
            showError("CTC number must be 12 digits");
            return false;
        }

        return true;
    }

    private boolean isEmpty(TextInputEditText input) {
        return input.getText() == null || input.getText().toString().trim().isEmpty();
    }

    private void proceedToUploadRequirements(int requestId) {
        Intent intent = new Intent(this, UploadRequirementsActivity.class);
        intent.putExtra("requestId", requestId);
        intent.putExtra("documentType", DOCUMENT_TYPE);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    public void back(View view) {
        finish();
    }
}