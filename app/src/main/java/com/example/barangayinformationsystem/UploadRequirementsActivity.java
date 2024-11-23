package com.example.barangayinformationsystem;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadRequirementsActivity extends AppCompatActivity {
    private ImageView imagePreview;
    private TextInputEditText copiesInput;
    private Uri validIdUri;
    private int requestId;
    private String documentType;
    private ApiService apiService;

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
                    imagePreview.setImageURI(validIdUri);
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    validIdUri = uri;
                    imagePreview.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_requirements);

        requestId = getIntent().getIntExtra("requestId", -1);
        documentType = getIntent().getStringExtra("documentType");

        if (requestId == -1 || documentType == null) {
            Toast.makeText(this, "Invalid request", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService();
        initializeComponents();
        setupListeners();
    }

    private void initializeComponents() {
        imagePreview = findViewById(R.id.upload_requirements_browse_file_imageview);
        copiesInput = findViewById(R.id.upload_requirements_number_of_copies_textInputEditText);

        MaterialTextView titleText = findViewById(R.id.upload_requirements_form_textview);
        titleText.setText("Upload Requirements for " + documentType);
    }

    private void setupListeners() {
        findViewById(R.id.upload_requirements_browse_file_imageview).setOnClickListener(v ->
                galleryLauncher.launch("image/*")
        );

        findViewById(R.id.upload_requirements_open_camera_take_photo_button).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        findViewById(R.id.upload_requirements_submit_button).setOnClickListener(v -> uploadRequirements());
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

    private void uploadRequirements() {
        if (validIdUri == null) {
            Toast.makeText(this, "Please select or capture a valid ID image", Toast.LENGTH_SHORT).show();
            return;
        }

        String copies = copiesInput.getText().toString();
        if (copies.isEmpty()) {
            Toast.makeText(this, "Please enter number of copies", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Show progress dialog
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Processing and uploading images...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Process and compress the valid ID image
            File compressedValidId = ImageUploadUtil.prepareImageForUpload(this, validIdUri, "valid_id");

            // Create request parts
            RequestBody requestIdPart = RequestBody.create(
                    MediaType.parse("text/plain"),
                    String.valueOf(requestId)
            );

            RequestBody quantityPart = RequestBody.create(
                    MediaType.parse("text/plain"),
                    copies
            );

            RequestBody validIdRequestFile = RequestBody.create(
                    MediaType.parse("image/jpeg"),
                    compressedValidId
            );

            MultipartBody.Part validIdPart = MultipartBody.Part.createFormData(
                    "validId",
                    compressedValidId.getName(),
                    validIdRequestFile
            );

            // Make the API call
            Call<UploadRequirementsResponse> call = apiService.uploadRequirements(
                    requestIdPart,
                    quantityPart,
                    validIdPart
            );

            call.enqueue(new Callback<UploadRequirementsResponse>() {
                @Override
                public void onResponse(Call<UploadRequirementsResponse> call, Response<UploadRequirementsResponse> response) {
                    progressDialog.dismiss();

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        // Show success dialog and navigate to home
                        Intent homeIntent = new Intent(UploadRequirementsActivity.this, HomeActivity.class);
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                        SuccessDialog.showSuccess(
                                UploadRequirementsActivity.this,
                                "Your request has been submitted. You will be notified when the document is ready for pickup.",
                                homeIntent,
                                2000
                        );
                    } else {
                        Toast.makeText(UploadRequirementsActivity.this,
                                "Error uploading requirements", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<UploadRequirementsResponse> call, Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(UploadRequirementsActivity.this,
                            "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error preparing upload: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private File createFileFromUri(Uri uri) {
        try {
            File destinationFile = new File(getCacheDir(), "temp_upload_file");
            InputStream inputStream = getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return destinationFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}