package com.example.barangayinformationsystem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.widget.ImageView;

public class RegistrationActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;

    private TextView lengthRequirement;
    private TextView uppercaseRequirement;
    private TextView lowercaseRequirement;
    private TextView specialCharRequirement;

    private ImageButton backImageButton;
    private AppCompatButton btnRegister;
    private RadioGroup genderRadioGroup;
    private RadioButton maleRadioButton;
    private RadioButton femaleRadioButton;

    private AppCompatButton takePhotoButton;
    private AppCompatButton choosePhotoButton;
    private ImageView validIdPreview;
    private Uri validIdUri;
    private Uri photoUri;
    private AppCompatButton takeBackPhotoButton;
    private AppCompatButton chooseBackPhotoButton;
    private ImageView backValidIdPreview;
    private Uri backValidIdUri;

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
        setupPasswordValidation();
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    validIdPreview.setImageURI(validIdUri);
                    validIdPreview.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    validIdUri = uri;
                    validIdPreview.setImageURI(uri);
                    validIdPreview.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<Intent> backCameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    backValidIdPreview.setImageURI(backValidIdUri);
                    backValidIdPreview.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> backGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    backValidIdUri = uri;
                    backValidIdPreview.setImageURI(uri);
                    backValidIdPreview.setVisibility(View.VISIBLE);
                }
            });

    private boolean initializeComponents() {
        try {
            // Initialize all existing components
            lengthRequirement = findViewById(R.id.lengthRequirement);
            uppercaseRequirement = findViewById(R.id.uppercaseRequirement);
            lowercaseRequirement = findViewById(R.id.lowercaseRequirement);
            specialCharRequirement = findViewById(R.id.specialCharRequirement);

            takeBackPhotoButton = findViewById(R.id.takeBackPhotoButton);
            chooseBackPhotoButton = findViewById(R.id.chooseBackPhotoButton);
            backValidIdPreview = findViewById(R.id.backValidIdPreview);
            backImageButton = findViewById(R.id.backImageButton);
            btnRegister = findViewById(R.id.btnRegister);
            genderRadioGroup = findViewById(R.id.genderRadioGroup);
            maleRadioButton = findViewById(R.id.maleRadioButton);
            femaleRadioButton = findViewById(R.id.femaleRadioButton);

            // Initialize Valid ID components
            takePhotoButton = findViewById(R.id.takePhotoButton);
            choosePhotoButton = findViewById(R.id.choosePhotoButton);
            validIdPreview = findViewById(R.id.validIdPreview);

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
            ageTextInputEditText.setFocusable(false);
            ageTextInputEditText.setClickable(false);
            houseNumberTextInputLayout = findViewById(R.id.houseNumberTextInputLayout);
            zoneTextInputLayout = findViewById(R.id.zoneTextInputLayout);
            streetTextInputLayout = findViewById(R.id.streetTextInputLayout);

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

        takePhotoButton.setOnClickListener(v -> dispatchTakePictureIntent());
        choosePhotoButton.setOnClickListener(v -> openGallery());

        takeBackPhotoButton.setOnClickListener(v -> dispatchTakeBackPictureIntent());
        chooseBackPhotoButton.setOnClickListener(v -> openBackGallery());
    }

    private void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = new File(getExternalCacheDir(), "valid_id_photo.jpg");
            validIdUri = FileProvider.getUriForFile(this,
                    "com.example.barangayinformationsystem.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, validIdUri);
            cameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void dispatchTakeBackPictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openBackCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openBackCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = new File(getExternalCacheDir(), "valid_id_back_photo.jpg");
            backValidIdUri = FileProvider.getUriForFile(this,
                    "com.example.barangayinformationsystem.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, backValidIdUri);
            backCameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openBackGallery() {
        backGalleryLauncher.launch("image/*");
    }

    public void openDialog(View view) {
        Calendar calendar = Calendar.getInstance();
        
        // Calculate the date 18 years ago to set as maximum allowed date
        Calendar maxDateCalendar = Calendar.getInstance();
        maxDateCalendar.add(Calendar.YEAR, -18);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> {
                    // Double-check if selected date makes user at least 18 years old
                    Calendar birthCalendar = Calendar.getInstance();
                    birthCalendar.set(year, month, day);
                    Calendar currentCalendar = Calendar.getInstance();
                    
                    int age = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);
                    
                    // Adjust age if birthday hasn't occurred this year
                    if (currentCalendar.get(Calendar.MONTH) < birthCalendar.get(Calendar.MONTH) || 
                        (currentCalendar.get(Calendar.MONTH) == birthCalendar.get(Calendar.MONTH) && 
                         currentCalendar.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH))) {
                        age--;
                    }
                    
                    // Only set the date if age is at least 18, otherwise show error
                    if (age >= 18) {
                        // Format and set the birth date
                        String formattedDate = String.format("%d-%02d-%02d", year, month + 1, day);
                        birthDateTextInputEditText.setText(formattedDate);
                        
                        // Set the calculated age
                        ageTextInputEditText.setText(String.valueOf(age));
                    } else {
                        Toast.makeText(RegistrationActivity.this, 
                                "You must be at least 18 years old to register", 
                                Toast.LENGTH_LONG).show();
                        // Clear any previously set date if the new one is invalid
                        birthDateTextInputEditText.setText("");
                        ageTextInputEditText.setText("");
                    }
                },
                calendar.get(Calendar.YEAR) - 18, // Start with 18 years ago as default
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set the maximum date to 18 years ago
        datePickerDialog.getDatePicker().setMaxDate(maxDateCalendar.getTimeInMillis());
        
        datePickerDialog.show();
    }

    private void setupPasswordValidation() {
        passwordTextInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                validatePasswordRequirements(password);
            }
        });
    }

    private boolean validatePassword(String password) {
        boolean isValid = true;

        if (password.length() < 8) {
            isValid = false;
        }
        if (password.equals(password.toLowerCase())) {
            isValid = false;
        }
        if (password.equals(password.toUpperCase())) {
            isValid = false;
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            isValid = false;
        }

        if (!isValid) {
            passwordTextInputLayout.setError("Password does not meet requirements");
        }

        return isValid;
    }

    private void validatePasswordRequirements(String password) {
        // Check length requirement
        boolean hasLength = password.length() >= 8;
        updateRequirement(lengthRequirement, hasLength);

        // Check uppercase requirement
        boolean hasUppercase = !password.equals(password.toLowerCase());
        updateRequirement(uppercaseRequirement, hasUppercase);

        // Check lowercase requirement
        boolean hasLowercase = !password.equals(password.toUpperCase());
        updateRequirement(lowercaseRequirement, hasLowercase);

        // Check special character requirement
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        updateRequirement(specialCharRequirement, hasSpecial);
    }

    private void updateRequirement(TextView requirement, boolean isMet) {
        Drawable icon = ContextCompat.getDrawable(this,
                isMet ? R.drawable.ic_check : R.drawable.ic_error);

        requirement.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        requirement.setTextColor(ContextCompat.getColor(this,
                isMet ? android.R.color.darker_gray : android.R.color.holo_red_light));
    }


    private boolean validateInputs() {
        boolean isValid = true;
        String errorMessage = "Please fill in: ";
        List<String> missingFields = new ArrayList<>();

        // Clear all previous errors
        clearAllErrors();

        if (isEmpty(firstNameTextInputEditText)) {
            firstNameTextInputLayout.setError("First name is required");
            missingFields.add("First Name");
            isValid = false;
        }

        if (isEmpty(lastNameTextInputEditText)) {
            lastNameTextInputLayout.setError("Last name is required");
            missingFields.add("Last Name");
            isValid = false;
        }

        if (isEmpty(usernameTextInputEditText)) {
            usernameTextInputLayout.setError("Username is required");
            missingFields.add("Username");
            isValid = false;
        }

        if (isEmpty(birthDateTextInputEditText)) {
            birthdateTextInputLayout.setError("Birth date is required");
            missingFields.add("Birth Date");
            isValid = false;
        } else {
            // Ensure age is at least 18, regardless of whether ageTextInputEditText is empty
            // This is a fallback in case the DatePicker restriction is bypassed
            int age = 0;
            try {
                age = isEmpty(ageTextInputEditText) ? 0 : 
                      Integer.parseInt(ageTextInputEditText.getText().toString().trim());
            } catch (NumberFormatException e) {
                age = 0;
            }
            
            if (age < 18) {
                birthdateTextInputLayout.setError("You must be at least 18 years old to register");
                Toast.makeText(this, "You must be at least 18 years old to register", Toast.LENGTH_LONG).show();
                isValid = false;
            }
        }

        if (genderRadioGroup.getCheckedRadioButtonId() == -1) {
            missingFields.add("Gender");
            isValid = false;
        }

        if (isEmpty(houseNumberTextInputEditText)) {
            houseNumberTextInputLayout.setError("House number is required");
            missingFields.add("House Number");
            isValid = false;
        }

        if (isEmpty(zoneTextInputEditText)) {
            zoneTextInputLayout.setError("Zone is required");
            missingFields.add("Zone");
            isValid = false;
        }

        if (isEmpty(streetTextInputEditText)) {
            streetTextInputLayout.setError("Street");
            missingFields.add("Street");
            isValid = false;
        }

        // Password validations
        String password = passwordTextInputEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordTextInputEditText.getText().toString().trim();

        if (isEmpty(passwordTextInputEditText)) {
            passwordTextInputLayout.setError("Password is required");
            missingFields.add("Password");
            isValid = false;
        } else if (!validatePassword(password)) {
            missingFields.add("Valid Password");
            isValid = false;
        }

        if (isEmpty(confirmPasswordTextInputEditText)) {
            confirmPasswordTextInputLayout.setError("Please confirm your password");
            missingFields.add("Confirm Password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordTextInputLayout.setError("Passwords do not match");
            missingFields.add("Matching Passwords");
            isValid = false;
        }

        if (validIdUri == null || backValidIdUri == null) {
            missingFields.add("Valid ID (Front and Back)");
            isValid = false;
        }

        // Show toast with missing fields if any
        if (!isValid) {
            String fieldsMessage = String.join(", ", missingFields);
            Toast.makeText(this, errorMessage + fieldsMessage, Toast.LENGTH_LONG).show();
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
            if (validIdUri == null) {
                Toast.makeText(this, "Please provide a valid ID image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress dialog
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Processing registration...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Process and compress the valid ID image
            File compressedValidId = ImageUploadUtil.prepareImageForUpload(this, validIdUri, "valid_id");
            File compressedBackValidId = ImageUploadUtil.prepareImageForUpload(this, backValidIdUri, "valid_id_back");

            // Create request parts
            RequestBody firstNamePart = RequestBody.create(MediaType.parse("text/plain"), firstNameTextInputEditText.getText().toString().trim());
            RequestBody lastNamePart = RequestBody.create(MediaType.parse("text/plain"), lastNameTextInputEditText.getText().toString().trim());
            RequestBody usernamePart = RequestBody.create(MediaType.parse("text/plain"), usernameTextInputEditText.getText().toString().trim());
            String hashedPassword = PasswordHasher.hashPassword(passwordTextInputEditText.getText().toString().trim());
            if (hashedPassword == null) {
                Toast.makeText(this, "Error processing password. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }
            RequestBody passwordPart = RequestBody.create(MediaType.parse("text/plain"), hashedPassword);
            RequestBody agePart = RequestBody.create(MediaType.parse("text/plain"), ageTextInputEditText.getText().toString().trim());
            RequestBody birthDatePart = RequestBody.create(MediaType.parse("text/plain"), birthDateTextInputEditText.getText().toString().trim());
            RequestBody houseNumberPart = RequestBody.create(MediaType.parse("text/plain"), houseNumberTextInputEditText.getText().toString().trim());
            RequestBody zonePart = RequestBody.create(MediaType.parse("text/plain"), zoneTextInputEditText.getText().toString().trim());
            RequestBody streetPart = RequestBody.create(MediaType.parse("text/plain"), streetTextInputEditText.getText().toString().trim());
            RequestBody genderPart = RequestBody.create(MediaType.parse("text/plain"), maleRadioButton.isChecked() ? "male" : "female");

            RequestBody validIdRequestFile = RequestBody.create(MediaType.parse("image/jpeg"), compressedValidId);
            MultipartBody.Part validIdPart = MultipartBody.Part.createFormData("valid_id", compressedValidId.getName(), validIdRequestFile);
            MultipartBody.Part backValidIdPart = MultipartBody.Part.createFormData("valid_id_back", compressedBackValidId.getName(), RequestBody.create(MediaType.parse("image/jpeg"), compressedBackValidId));

            // Make API call
            ApiService apiService = RetrofitClient.getApiService();
            Call<RegistrationResponse> call = apiService.registerUser(
                    firstNamePart, lastNamePart, usernamePart, passwordPart,
                    agePart, birthDatePart, houseNumberPart, zonePart, streetPart,
                    genderPart, validIdPart,backValidIdPart
            );

            call.enqueue(new Callback<RegistrationResponse>() {
                @Override
                public void onResponse(Call<RegistrationResponse> call, Response<RegistrationResponse> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().isSuccess()) {
                            // Save login state and user ID if provided in response
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RegistrationActivity.this);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("user_id", response.body().getId());
                            editor.apply();

                            // Show success dialog first, then go to PendingStatusActivity
                            Intent pendingIntent = new Intent(RegistrationActivity.this, PendingStatusActivity.class);
                            pendingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                            SuccessDialog.showSuccess(
                                    RegistrationActivity.this,
                                    "Registration successful! Please wait for your account to be verified.",
                                    pendingIntent,
                                    3000  // Show dialog for 3 seconds before redirecting
                            );
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
                    progressDialog.dismiss();
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