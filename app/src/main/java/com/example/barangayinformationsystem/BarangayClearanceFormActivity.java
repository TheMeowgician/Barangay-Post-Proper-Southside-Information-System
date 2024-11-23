package com.example.barangayinformationsystem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BarangayClearanceFormActivity extends AppCompatActivity {

    private static final String TAG = "BarangayClearanceForm";
    private ImageButton backButton;
    private TextInputEditText nameInput, aliasInput, ageInput, addressInput;
    private TextInputEditText citizenshipInput, lengthOfStayInput, tinInput, ctcInput, purposeInput;
    private RadioGroup genderRadioGroup, civilStatusRadioGroup;
    private ApiService apiService;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barangay_clearance_form);

        apiService = RetrofitClient.getApiService();
        initializeComponents();
        setupListeners();
    }

    private void initializeComponents() {
        backButton = findViewById(R.id.barangay_clearance_form_back_button);
        nameInput = findViewById(R.id.barangay_clearance_form_name_textInputEditText);
        aliasInput = findViewById(R.id.barangay_clearance_form_alias_textInputEditText);
        ageInput = findViewById(R.id.barangay_clearance_form_age_textInputEditText);
        addressInput = findViewById(R.id.barangay_clearance_form_address_textInputEditText);
        citizenshipInput = findViewById(R.id.barangay_clearance_form_citizenship_textInputEditText);
        lengthOfStayInput = findViewById(R.id.barangay_clearance_form_length_of_stay_textInputEditText);
        tinInput = findViewById(R.id.barangay_clearance_form_tin_textInputEditText);
        ctcInput = findViewById(R.id.barangay_clearance_form_ctc_textInputEditText);
        purposeInput = findViewById(R.id.barangay_clearance_form_purpose_textInputEditText);
        genderRadioGroup = findViewById(R.id.barangay_clearance_form_gender_radioGroup);
        civilStatusRadioGroup = findViewById(R.id.barangay_clearance_form_civil_status_radioGroup);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting request...");
        progressDialog.setCancelable(false);
    }

    private void setupListeners() {
        findViewById(R.id.barangay_clearance_form_submit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitForm();
            }
        });
    }

    private void submitForm() {
        try {
            // Validate inputs
            if (!validateInputs()) {
                return;
            }

            Log.d(TAG, "Starting form submission");
            progressDialog.show();

            // Get all form values with proper error handling
            String name = getEditTextValue(nameInput);
            String alias = getEditTextValue(aliasInput);
            String address = getEditTextValue(addressInput);
            String tin = getEditTextValue(tinInput);
            String ctc = getEditTextValue(ctcInput);
            String citizenship = getEditTextValue(citizenshipInput);
            String purpose = getEditTextValue(purposeInput);

            // Parse numeric values safely
            int age = parseIntSafely(ageInput.getText().toString(), "age");
            int lengthOfStay = parseIntSafely(lengthOfStayInput.getText().toString(), "length of stay");

            String gender = getSelectedRadioButtonText(genderRadioGroup);
            String civilStatus = getSelectedRadioButtonText(civilStatusRadioGroup);

            Log.d(TAG, "Form data prepared. Making API call...");

            // Create API call
            Call<DocumentRequestResponse> call = apiService.submitDocumentRequest(
                    "Barangay Clearance",
                    name,
                    address,
                    tin,
                    ctc,
                    alias,
                    age,
                    lengthOfStay,
                    citizenship,
                    gender,
                    civilStatus,
                    purpose,
                    1  // Default quantity
            );

            call.enqueue(new Callback<DocumentRequestResponse>() {
                @Override
                public void onResponse(Call<DocumentRequestResponse> call, Response<DocumentRequestResponse> response) {
                    try {
                        progressDialog.dismiss();

                        if (response.isSuccessful() && response.body() != null) {
                            DocumentRequestResponse body = response.body();
                            Log.d(TAG, "Response successful: " + body.isSuccess());

                            if (body.isSuccess()) {
                                int requestId = body.getRequestId();
                                Log.d(TAG, "Request ID received: " + requestId);
                                proceedToUploadRequirements(requestId);
                            } else {
                                String message = body.getMessage();
                                Log.e(TAG, "Server returned error: " + message);
                                showError("Server error: " + message);
                            }
                        } else {
                            String errorBody = response.errorBody() != null ?
                                    response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Response not successful: " + errorBody);
                            showError("Error submitting form: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response", e);
                        showError("Error processing response: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<DocumentRequestResponse> call, Throwable t) {
                    Log.e(TAG, "Network error", t);
                    progressDialog.dismiss();
                    showError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in form submission", e);
            progressDialog.dismiss();
            showError("Error preparing form submission: " + e.getMessage());
        }
    }

    private String getEditTextValue(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    private int parseIntSafely(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " value: " + value);
        }
    }

    private boolean validateInputs() {
        try {
            if (isEmpty(nameInput)) {
                nameInput.setError("Name is required");
                return false;
            }
            if (isEmpty(ageInput)) {
                ageInput.setError("Age is required");
                return false;
            }
            if (isEmpty(addressInput)) {
                addressInput.setError("Address is required");
                return false;
            }
            if (isEmpty(lengthOfStayInput)) {
                lengthOfStayInput.setError("Length of stay is required");
                return false;
            }
            if (isEmpty(citizenshipInput)) {
                citizenshipInput.setError("Citizenship is required");
                return false;
            }
            if (genderRadioGroup.getCheckedRadioButtonId() == -1) {
                showError("Please select gender");
                return false;
            }
            if (civilStatusRadioGroup.getCheckedRadioButtonId() == -1) {
                showError("Please select civil status");
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating inputs", e);
            showError("Error validating form: " + e.getMessage());
            return false;
        }
    }

    private boolean isEmpty(TextInputEditText input) {
        return input.getText() == null || input.getText().toString().trim().isEmpty();
    }

    private String getSelectedRadioButtonText(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "";
        RadioButton radioButton = findViewById(selectedId);
        return radioButton != null ? radioButton.getText().toString() : "";
    }

    private void proceedToUploadRequirements(int requestId) {
        try {
            Log.d(TAG, "Starting UploadRequirementsActivity with requestId: " + requestId);
            Intent intent = new Intent(this, UploadRequirementsActivity.class);
            intent.putExtra("requestId", requestId);
            intent.putExtra("documentType", "Barangay Clearance");
            startActivity(intent);
            // Don't finish this activity yet
        } catch (Exception e) {
            Log.e(TAG, "Error starting UploadRequirementsActivity", e);
            showError("Error proceeding to upload: " + e.getMessage());
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    public void back(View view) {
        finish();
    }
}