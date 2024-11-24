package com.example.barangayinformationsystem;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BarangayClearanceFormActivity extends AppCompatActivity {

    private TextInputEditText birthdayInput;
    private Calendar calendar;
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

        calendar = Calendar.getInstance();
        apiService = RetrofitClient.getApiService();
        initializeComponents();
        setupListeners();
        setupDateInputHandling();

    }

    private void initializeComponents() {
        birthdayInput = findViewById(R.id.barangay_clearance_form_date_of_birth_textInputEditText);
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

    private void setupDateInputHandling() {
        // Set up date format watcher
        birthdayInput.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private String mmddyy = "MMDDYY";
            private Calendar cal = Calendar.getInstance();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d.]|\\.","");
                    String cleanC = current.replaceAll("[^\\d.]|\\.","");

                    int cl = clean.length();
                    int sel = cl;
                    for (int i = 2; i <= cl && i < 6; i += 2) {
                        sel++;
                    }
                    //Fix for pressing delete next to a forward slash
                    if (clean.equals(cleanC)) sel--;

                    if (clean.length() < 6){
                        clean = clean + mmddyy.substring(clean.length());
                    }else{
                        //This part makes sure that when we finish entering numbers
                        //the date is correct, fixing it if necessary
                        int mon  = Integer.parseInt(clean.substring(0,2));
                        int day  = Integer.parseInt(clean.substring(2,4));
                        int year = Integer.parseInt(clean.substring(4,6));

                        mon = mon < 1 ? 1 : mon > 12 ? 12 : mon;
                        cal.set(Calendar.MONTH, mon-1);
                        year = year + 2000;
                        cal.set(Calendar.YEAR, year);

                        // Adjust day based on month and year
                        int maxDays = cal.getActualMaximum(Calendar.DATE);
                        day = day < 1 ? 1 : day > maxDays ? maxDays : day;

                        clean = String.format(Locale.US, "%02d%02d%02d", mon, day, year % 100);
                    }

                    clean = String.format("%s-%s-%s", clean.substring(0,2),
                            clean.substring(2,4), clean.substring(4,6));

                    sel = sel < 0 ? 0 : sel;
                    current = clean;
                    birthdayInput.setText(current);
                    birthdayInput.setSelection(sel < current.length() ? sel : current.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up date picker dialog
        birthdayInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        String dateFormat = String.format(Locale.US, "%02d-%02d-%02d",
                                month + 1, dayOfMonth, year % 100);
                        birthdayInput.setText(dateFormat);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
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

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            int userId = prefs.getInt("user_id", -1);

            Log.d(TAG, "Starting form submission");
            progressDialog.show();

            // Get all form values with proper error handling
            String birthday = birthdayInput.getText().toString().trim();
            if (!isValidDate(birthday)) {
                birthdayInput.setError("Please enter a valid date (MM-DD-YY)");
                return;
            }
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
                    userId,  // Add userId here
                    "Barangay Clearance",
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
                    1
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

    private boolean isValidDate(String date) {
        if (date == null || !date.matches("\\d{2}-\\d{2}-\\d{2}")) {
            return false;
        }
        try {
            String[] parts = date.split("-");
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            if (month < 1 || month > 12) return false;
            if (day < 1 || day > 31) return false;
            if (year < 0 || year > 99) return false;

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 2000 + year);
            cal.set(Calendar.MONTH, month - 1);

            return day <= cal.getActualMaximum(Calendar.DATE);
        } catch (Exception e) {
            return false;
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