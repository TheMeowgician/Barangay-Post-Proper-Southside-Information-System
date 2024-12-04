package com.example.barangayinformationsystem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

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

    private boolean initializeComponents() {
        try {
            // Initialize all existing components
            lengthRequirement = findViewById(R.id.lengthRequirement);
            uppercaseRequirement = findViewById(R.id.uppercaseRequirement);
            lowercaseRequirement = findViewById(R.id.lowercaseRequirement);
            specialCharRequirement = findViewById(R.id.specialCharRequirement);

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
    }

    private void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(getExternalCacheDir(), "valid_id_photo.jpg");
        validIdUri = FileProvider.getUriForFile(this,
                "com.example.barangayinformationsystem.fileprovider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, validIdUri);
        cameraLauncher.launch(takePictureIntent);
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (data != null && data.getExtras() != null) {
                    Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                    if (imageBitmap != null) {
                        validIdPreview.setImageBitmap(imageBitmap);
                        validIdPreview.setVisibility(View.VISIBLE);
                        validIdUri = getImageUri(this, imageBitmap);
                    } else {
                        Toast.makeText(this, "Error capturing image", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                if (data != null && data.getData() != null) {
                    validIdUri = data.getData();
                    validIdPreview.setImageURI(validIdUri);
                    validIdPreview.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, "Error selecting image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        File outputFile = new File(context.getCacheDir(), "valid_id_" + System.currentTimeMillis() + ".jpg");
        outputFile.deleteOnExit(); // Ensures file is removed when app exits
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show();
            return null;
        }
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
        if (!validatePassword(password)) {
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
        if (validIdUri == null) {
            Toast.makeText(this, "Please provide a valid ID with address", Toast.LENGTH_SHORT).show();
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

            // Create request parts
            RequestBody firstNamePart = RequestBody.create(MediaType.parse("text/plain"), firstNameTextInputEditText.getText().toString().trim());
            RequestBody lastNamePart = RequestBody.create(MediaType.parse("text/plain"), lastNameTextInputEditText.getText().toString().trim());
            RequestBody usernamePart = RequestBody.create(MediaType.parse("text/plain"), usernameTextInputEditText.getText().toString().trim());
            RequestBody passwordPart = RequestBody.create(MediaType.parse("text/plain"), passwordTextInputEditText.getText().toString().trim());
            RequestBody agePart = RequestBody.create(MediaType.parse("text/plain"), ageTextInputEditText.getText().toString().trim());
            RequestBody birthDatePart = RequestBody.create(MediaType.parse("text/plain"), birthDateTextInputEditText.getText().toString().trim());
            RequestBody houseNumberPart = RequestBody.create(MediaType.parse("text/plain"), houseNumberTextInputEditText.getText().toString().trim());
            RequestBody zonePart = RequestBody.create(MediaType.parse("text/plain"), zoneTextInputEditText.getText().toString().trim());
            RequestBody streetPart = RequestBody.create(MediaType.parse("text/plain"), streetTextInputEditText.getText().toString().trim());
            RequestBody genderPart = RequestBody.create(MediaType.parse("text/plain"), maleRadioButton.isChecked() ? "male" : "female");

            RequestBody validIdRequestFile = RequestBody.create(MediaType.parse("image/jpeg"), compressedValidId);
            MultipartBody.Part validIdPart = MultipartBody.Part.createFormData("valid_id", compressedValidId.getName(), validIdRequestFile);

            // Make API call
            ApiService apiService = RetrofitClient.getApiService();
            Call<RegistrationResponse> call = apiService.registerUser(
                    firstNamePart, lastNamePart, usernamePart, passwordPart,
                    agePart, birthDatePart, houseNumberPart, zonePart, streetPart,
                    genderPart, validIdPart
            );

            call.enqueue(new Callback<RegistrationResponse>() {
                @Override
                public void onResponse(Call<RegistrationResponse> call, Response<RegistrationResponse> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().isSuccess()) {
                            Intent loginIntent = new Intent(RegistrationActivity.this, LogInActivity.class);
                            loginIntent.putExtra("username", usernameTextInputEditText.getText().toString().trim());
                            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                            SuccessDialog.showSuccess(
                                    RegistrationActivity.this,
                                    "Registration successful! Please wait for your account to be verified.",
                                    loginIntent
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