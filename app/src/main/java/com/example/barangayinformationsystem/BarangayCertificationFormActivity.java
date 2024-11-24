package com.example.barangayinformationsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

public class BarangayCertificationFormActivity extends AppCompatActivity {

    private ImageButton barangay_certification_form_back_button;
    private TextInputEditText nameInput, aliasInput, ageInput, addressInput;
    private TextInputEditText citizenshipInput, lengthOfStayInput, tinInput, ctcInput, purposeInput;
    private TextInputEditText birthdayInput;
    private RadioGroup genderRadioGroup, civilStatusRadioGroup;
    private ApiService apiService;

    private static final String DOCUMENT_TYPE = "Barangay Certification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barangay_certification_form);

        apiService = RetrofitClient.getApiService();
        initializeComponents();
        setupListeners();
    }

    private void initializeComponents() {
        barangay_certification_form_back_button = findViewById(R.id.barangay_certification_form_back_button);
        nameInput = findViewById(R.id.barangay_certification_form_name_textInputEditText);
        aliasInput = findViewById(R.id.barangay_certification_form_alias_textInputEditText);
        ageInput = findViewById(R.id.barangay_certification_form_age_textInputEditText);
        addressInput = findViewById(R.id.barangay_certification_form_address_textInputEditText);
        citizenshipInput = findViewById(R.id.barangay_certification_form_citizenship_textInputEditText);
        lengthOfStayInput = findViewById(R.id.barangay_certification_form_length_of_stay_textInputEditText);
        tinInput = findViewById(R.id.barangay_certification_form_tin_textInputEditText);
        ctcInput = findViewById(R.id.barangay_certification_form_ctc_textInputEditText);
        purposeInput = findViewById(R.id.barangay_certification_form_purpose_textInputEditText);
        birthdayInput = findViewById(R.id.barangay_certification_form_date_of_birth_textInputEditText);
        genderRadioGroup = findViewById(R.id.barangay_certification_form_gender_radioGroup);
        civilStatusRadioGroup = findViewById(R.id.barangay_certification_form_civil_status_radioGroup);
    }

    private void setupListeners() {
        findViewById(R.id.barangay_certification_form_submit_button).setOnClickListener(v -> submitForm());
    }

    private void submitForm() {
        if (!validateInputs()) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("user_id", -1);

        // Get all form values
        String name = nameInput.getText().toString();
        String alias = aliasInput.getText().toString();
        int age = Integer.parseInt(ageInput.getText().toString());
        String address = addressInput.getText().toString();
        String citizenship = citizenshipInput.getText().toString();
        int lengthOfStay = Integer.parseInt(lengthOfStayInput.getText().toString());
        String tin = tinInput.getText().toString();
        String ctc = ctcInput.getText().toString();
        String purpose = purposeInput.getText().toString();
        String birthday = birthdayInput.getText().toString();
        String gender = getSelectedRadioButtonText(genderRadioGroup);
        String civilStatus = getSelectedRadioButtonText(civilStatusRadioGroup);

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
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    int requestId = response.body().getRequestId();
                    proceedToUploadRequirements(requestId);
                } else {
                    Toast.makeText(BarangayCertificationFormActivity.this,
                            "Error submitting form", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DocumentRequestResponse> call, Throwable t) {
                Toast.makeText(BarangayCertificationFormActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInputs() {
        if (nameInput.getText().toString().isEmpty()) {
            nameInput.setError("Name is required");
            return false;
        }
        if (ageInput.getText().toString().isEmpty()) {
            ageInput.setError("Age is required");
            return false;
        }
        if (addressInput.getText().toString().isEmpty()) {
            addressInput.setError("Address is required");
            return false;
        }
        if (lengthOfStayInput.getText().toString().isEmpty()) {
            lengthOfStayInput.setError("Length of stay is required");
            return false;
        }
        if (citizenshipInput.getText().toString().isEmpty()) {
            citizenshipInput.setError("Citizenship is required");
            return false;
        }
        if (birthdayInput.getText().toString().isEmpty()) {
            birthdayInput.setError("Birthday is required");
            return false;
        }
        if (purposeInput.getText().toString().isEmpty()) {
            purposeInput.setError("Purpose is required");
            return false;
        }
        if (genderRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (civilStatusRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select civil status", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String getSelectedRadioButtonText(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return "";
        RadioButton radioButton = findViewById(selectedId);
        return radioButton.getText().toString();
    }

    private void proceedToUploadRequirements(int requestId) {
        Intent intent = new Intent(this, UploadRequirementsActivity.class);
        intent.putExtra("requestId", requestId);
        intent.putExtra("documentType", DOCUMENT_TYPE);
        startActivity(intent);
        finish();
    }

    public void back(View view) {
        finish();
    }
}