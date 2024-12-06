package com.example.barangayinformationsystem;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.textview.MaterialTextView;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadRequirementsActivity extends AppCompatActivity {
    private ImageView frontImagePreview;
    private ImageView backImagePreview;
    private Spinner copiesSpinner;
    private Uri frontIdUri;
    private Uri backIdUri;
    private int requestId;
    private String documentType;
    private ApiService apiService;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera(true); // Default to front ID camera
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> frontCameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    frontImagePreview.setImageURI(frontIdUri);
                    frontImagePreview.setVisibility(ImageView.VISIBLE);
                }
            });

    private final ActivityResultLauncher<Intent> backCameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    backImagePreview.setImageURI(backIdUri);
                    backImagePreview.setVisibility(ImageView.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> frontGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    frontIdUri = uri;
                    frontImagePreview.setImageURI(uri);
                    frontImagePreview.setVisibility(ImageView.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> backGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    backIdUri = uri;
                    backImagePreview.setImageURI(uri);
                    backImagePreview.setVisibility(ImageView.VISIBLE);
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
        frontImagePreview = findViewById(R.id.upload_requirements_front_preview);
        backImagePreview = findViewById(R.id.upload_requirements_back_preview);
        copiesSpinner = findViewById(R.id.upload_requirements_copies_spinner);

        MaterialTextView titleText = findViewById(R.id.upload_requirements_form_textview);
        titleText.setText("Upload Requirements for " + documentType);

        // Setup spinner
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new Integer[]{1, 2, 3, 4, 5}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        copiesSpinner.setAdapter(adapter);
        copiesSpinner.setSelection(0); // Default to 1 copy
    }

    private void setupListeners() {
        findViewById(R.id.upload_requirements_front_camera_button).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera(true);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        findViewById(R.id.upload_requirements_back_camera_button).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera(false);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        findViewById(R.id.upload_requirements_front_gallery_button).setOnClickListener(v ->
                frontGalleryLauncher.launch("image/*")
        );

        findViewById(R.id.upload_requirements_back_gallery_button).setOnClickListener(v ->
                backGalleryLauncher.launch("image/*")
        );

        findViewById(R.id.upload_requirements_submit_button).setOnClickListener(v -> uploadRequirements());
    }

    private void openCamera(boolean isFrontId) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(getExternalCacheDir(),
                isFrontId ? "valid_id_front.jpg" : "valid_id_back.jpg");

        Uri photoUri = FileProvider.getUriForFile(this,
                "com.example.barangayinformationsystem.fileprovider",
                photoFile);

        if (isFrontId) {
            frontIdUri = photoUri;
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, frontIdUri);
            frontCameraLauncher.launch(takePictureIntent);
        } else {
            backIdUri = photoUri;
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, backIdUri);
            backCameraLauncher.launch(takePictureIntent);
        }
    }

    private void uploadRequirements() {
        if (frontIdUri == null || backIdUri == null) {
            Toast.makeText(this, "Please provide both front and back images of your valid ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Show progress dialog
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Processing and uploading images...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Process and compress both ID images
            File compressedFrontId = ImageUploadUtil.prepareImageForUpload(this, frontIdUri, "front_id");
            File compressedBackId = ImageUploadUtil.prepareImageForUpload(this, backIdUri, "back_id");

            // Create request parts
            RequestBody requestIdPart = RequestBody.create(
                    MediaType.parse("text/plain"),
                    String.valueOf(requestId)
            );

            RequestBody quantityPart = RequestBody.create(
                    MediaType.parse("text/plain"),
                    String.valueOf(copiesSpinner.getSelectedItem())
            );

            MultipartBody.Part frontIdPart = MultipartBody.Part.createFormData(
                    "frontId",
                    compressedFrontId.getName(),
                    RequestBody.create(MediaType.parse("image/jpeg"), compressedFrontId)
            );

            MultipartBody.Part backIdPart = MultipartBody.Part.createFormData(
                    "backId",
                    compressedBackId.getName(),
                    RequestBody.create(MediaType.parse("image/jpeg"), compressedBackId)
            );

            // Make the API call
            Call<UploadRequirementsResponse> call = apiService.uploadRequirements(
                    requestIdPart,
                    quantityPart,
                    frontIdPart,
                    backIdPart
            );

            call.enqueue(new Callback<UploadRequirementsResponse>() {
                @Override
                public void onResponse(Call<UploadRequirementsResponse> call,
                                       Response<UploadRequirementsResponse> response) {
                    progressDialog.dismiss();

                    if (response.isSuccessful() && response.body() != null &&
                            response.body().isSuccess()) {
                        // Show success dialog and navigate to home
                        Intent homeIntent = new Intent(UploadRequirementsActivity.this,
                                DocumentStatusFragment.class);
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_NEW_TASK);

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
}