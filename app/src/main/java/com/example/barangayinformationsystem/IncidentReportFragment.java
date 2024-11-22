package com.example.barangayinformationsystem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidentReportFragment extends Fragment {

    private TextInputLayout titleInputLayout;
    private TextInputLayout descriptionInputLayout;
    private TextInputEditText titleEditText;
    private TextInputEditText descriptionEditText;
    private ImageView uploadImageView;
    private Button submitButton;
    private String encodedImage = "";
    private SharedPreferences prefs;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    handleSelectedImage(imageUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            uploadImageView.setImageBitmap(imageBitmap);
                            encodedImage = encodeImage(imageBitmap);
                        }
                    }
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incident_report, container, false);
        initializeViews(view);
        return view;
    }

    private void initializeViews(View view) {
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        titleInputLayout = view.findViewById(R.id.usernameTextInputLayout);
        descriptionInputLayout = view.findViewById(R.id.incident_report_description_textInputLayout);
        titleEditText = view.findViewById(R.id.usernameTextInputEditText);
        descriptionEditText = view.findViewById(R.id.incident_report_description_textInputEditText);
        uploadImageView = view.findViewById(R.id.incident_report_upload_image_here_imageview_);
        submitButton = view.findViewById(R.id.incident_report_submit_button);

        uploadImageView.setOnClickListener(v -> showImagePickerDialog());
        submitButton.setOnClickListener(v -> submitReport());
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Photo");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    openCamera();
                    break;
                case 1:
                    openGallery();
                    break;
                case 2:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(getContext(), "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void handleSelectedImage(Uri imageUri) {
        try {
            // Calculate sample size to load a reasonably sized image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            int maxDimension = 1024; // Max width or height
            int sampleSize = 1;

            while (options.outWidth / sampleSize > maxDimension ||
                    options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2;
            }

            // Load the actual bitmap with calculated sample size
            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap != null) {
                // Scale if still too large
                float scale = Math.min(
                        (float) maxDimension / bitmap.getWidth(),
                        (float) maxDimension / bitmap.getHeight()
                );

                if (scale < 1) {
                    int scaledWidth = Math.round(bitmap.getWidth() * scale);
                    int scaledHeight = Math.round(bitmap.getHeight() * scale);
                    bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
                }

                // Update UI and store encoded image
                uploadImageView.setImageBitmap(bitmap);
                encodedImage = encodeImage(bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Image Error", "Failed to process image. Please try another image.");
        }
    }

    private void submitReport() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user ID
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            showError("Authentication Error", "Please log in again");
            return;
        }

        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Submitting Report")
                .setMessage("Please wait while we upload your report...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        submitButton.setEnabled(false);

        ApiService apiService = RetrofitClient.getApiService();
        Call<IncidentReportResponse> call = apiService.submitIncidentReport(
                userId,
                title,
                description,
                encodedImage
        );

        call.enqueue(new Callback<IncidentReportResponse>() {
            @Override
            public void onResponse(Call<IncidentReportResponse> call, Response<IncidentReportResponse> response) {
                submitButton.setEnabled(true);
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    IncidentReportResponse reportResponse = response.body();
                    if ("success".equals(reportResponse.getStatus())) {
                        clearForm();
                        SuccessDialog.showSuccess(requireContext(),
                                "Your incident report has been submitted successfully",
                                null);
                    } else {
                        showError("Submission Failed",
                                reportResponse.getMessage() != null ?
                                        reportResponse.getMessage() :
                                        "Failed to submit report. Please try again.");
                    }
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        showError("Submission Failed",
                                "Server error: " + errorBody);
                    } catch (IOException e) {
                        showError("Submission Failed",
                                "Failed to submit report. Please check your connection and try again.");
                    }
                }
            }

            @Override
            public void onFailure(Call<IncidentReportResponse> call, Throwable t) {
                submitButton.setEnabled(true);
                progressDialog.dismiss();
                showError("Network Error",
                        "Error: " + t.getMessage());
            }
        });
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void clearForm() {
        titleEditText.setText("");
        descriptionEditText.setText("");
        uploadImageView.setImageResource(R.drawable.incident_upload_image_here);
        encodedImage = "";
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Start with 80% quality
        int quality = 80;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);

        // If size is too large, gradually reduce quality
        int maxSizeBytes = 500000; // 500KB limit
        while (byteArrayOutputStream.size() > maxSizeBytes && quality > 20) {
            byteArrayOutputStream.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        }

        byte[] bytes = byteArrayOutputStream.toByteArray();
        String base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);

        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "data:image/jpeg;base64," + base64Image;
    }
}