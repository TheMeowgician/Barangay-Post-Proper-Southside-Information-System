package com.example.barangayinformationsystem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class IncidentReportFragment extends Fragment {

    private static final int MAX_IMAGES = 3;
    private static final int MAX_VIDEO_SIZE_MB = 100; // Increased to 100MB to match PHP API
    private static final long MAX_VIDEO_DURATION_MS = 60000; // 1 minute in milliseconds

    private LinearLayout selectedImagesContainer;
    private LinearLayout videoContainer;
    private List<String> encodedImages;
    private String encodedVideo = null;
    private Uri selectedVideoUri = null;
    private boolean hasVideo = false;

    private TextInputLayout titleInputLayout;
    private TextInputLayout descriptionInputLayout;
    private TextInputEditText titleEditText;
    private TextInputEditText descriptionEditText;
    private Spinner titleSpinner;
    private MaterialButton uploadImageButton;
    private MaterialButton uploadVideoButton;
    private Button submitButton;
    private SharedPreferences prefs;
    private File preparedVideoFile = null; // Add this as a class field

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
                            String encodedImage = encodeImage(imageBitmap);
                            encodedImages.add(encodedImage);
                            addImageToContainer(imageBitmap);
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> videoGalleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri videoUri = result.getData().getData();
                    handleSelectedVideo(videoUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> videoRecordLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri videoUri = result.getData().getData();
                    if (videoUri != null) {
                        handleSelectedVideo(videoUri);
                    }
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incident_report, container, false);
        initializeViews(view);

        // Initialize the spinner
        titleSpinner = view.findViewById(R.id.titleSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.incident_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        titleSpinner.setAdapter(adapter);

        return view;
    }

    private void initializeViews(View view) {
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        encodedImages = new ArrayList<>();

        titleInputLayout = view.findViewById(R.id.usernameTextInputLayout);
        descriptionInputLayout = view.findViewById(R.id.incident_report_description_textInputLayout);
        titleEditText = view.findViewById(R.id.usernameTextInputEditText);
        descriptionEditText = view.findViewById(R.id.incident_report_description_textInputEditText);

        uploadImageButton = view.findViewById(R.id.upload_image_button);
        uploadVideoButton = view.findViewById(R.id.upload_video_button);

        submitButton = view.findViewById(R.id.incident_report_submit_button);
        selectedImagesContainer = view.findViewById(R.id.selected_images_container);
        videoContainer = view.findViewById(R.id.video_container);

        uploadImageButton.setOnClickListener(v -> {
            if (encodedImages.size() >= MAX_IMAGES) {
                Toast.makeText(getContext(), "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
                return;
            }
            showImagePickerDialog();
        });

        uploadVideoButton.setOnClickListener(v -> {
            if (hasVideo) {
                Toast.makeText(getContext(), "Only one video allowed. Remove existing video first.", Toast.LENGTH_SHORT).show();
                return;
            }
            showVideoPickerDialog();
        });

        submitButton.setOnClickListener(v -> submitReport());
    }

    private void addImageToContainer(Bitmap bitmap) {
        ImageView imageView = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(100),
                dpToPx(100)
        );
        params.setMargins(dpToPx(8), 0, dpToPx(8), 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bitmap);

        ImageView deleteButton = new ImageView(requireContext());
        int deleteSize = dpToPx(24);
        RelativeLayout.LayoutParams deleteParams = new RelativeLayout.LayoutParams(deleteSize, deleteSize);
        deleteParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        deleteParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setImageResource(R.drawable.ic_delete);
        deleteButton.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        RelativeLayout container = new RelativeLayout(requireContext());
        container.setLayoutParams(params);
        container.addView(imageView);
        container.addView(deleteButton);

        deleteButton.setOnClickListener(v -> {
            selectedImagesContainer.removeView(container);
            encodedImages.remove(encodedImages.size() - 1);

            if (encodedImages.isEmpty()) {
                uploadImageButton.setText("Upload Images");
            } else {
                uploadImageButton.setText("Add More Images (" + encodedImages.size() + "/" + MAX_IMAGES + ")");
            }
        });

        selectedImagesContainer.addView(container);

        uploadImageButton.setText("Add More Images (" + encodedImages.size() + "/" + MAX_IMAGES + ")");
    }

    private void addVideoToContainer(Bitmap thumbnail) {
        videoContainer.removeAllViews();

        ImageView videoThumbnail = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(150),
                dpToPx(150)
        );
        params.setMargins(dpToPx(8), 0, dpToPx(8), 0);
        videoThumbnail.setLayoutParams(params);
        videoThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        videoThumbnail.setImageBitmap(thumbnail);

        ImageView playIcon = new ImageView(requireContext());
        RelativeLayout.LayoutParams playParams = new RelativeLayout.LayoutParams(
                dpToPx(50),
                dpToPx(50)
        );
        playParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        playIcon.setLayoutParams(playParams);
        playIcon.setImageResource(android.R.drawable.ic_media_play);
        playIcon.setAlpha(0.7f);

        ImageView deleteButton = new ImageView(requireContext());
        int deleteSize = dpToPx(24);
        RelativeLayout.LayoutParams deleteParams = new RelativeLayout.LayoutParams(deleteSize, deleteSize);
        deleteParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        deleteParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setImageResource(R.drawable.ic_delete);
        deleteButton.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        RelativeLayout container = new RelativeLayout(requireContext());
        container.setLayoutParams(params);
        container.addView(videoThumbnail);
        container.addView(playIcon);
        container.addView(deleteButton);

        deleteButton.setOnClickListener(v -> {
            videoContainer.removeAllViews();
            encodedVideo = null;
            selectedVideoUri = null;
            hasVideo = false;
            uploadVideoButton.setText("Upload Video");
        });

        videoThumbnail.setOnClickListener(v -> {
            if (selectedVideoUri != null) {
                try {
                    // For Android 11+ use the system video player explicitly to avoid permission issues
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        // Try opening directly with the original URI first
                        Intent directIntent = new Intent(Intent.ACTION_VIEW);
                        directIntent.setDataAndType(selectedVideoUri, "video/*");
                        directIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        try {
                            startActivity(directIntent);
                            return; // Exit if successful
                        } catch (Exception e) {
                            Log.d("VideoPlayer", "Could not play directly, trying alternative method");
                            // Continue to the file-based approach if this fails
                        }
                    }

                    // If above approach fails or on older Android, use file-based approach
                    String uniqueFileName = "temp_video_" + System.currentTimeMillis() + ".mp4";
                    File videoFile = new File(requireContext().getCacheDir(), uniqueFileName);

                    // Log the process
                    Log.d("VideoPlayer", "Creating temp file: " + videoFile.getAbsolutePath());

                    // Make sure we're not creating the file in a background thread that might get killed
                    try (InputStream inputStream = requireActivity().getContentResolver().openInputStream(selectedVideoUri);
                         FileOutputStream outputStream = new FileOutputStream(videoFile)) {

                        if (inputStream == null) {
                            Toast.makeText(getContext(), "Could not open video stream", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Copy with progress updates
                        byte[] buffer = new byte[16384]; // Larger buffer for faster copying
                        int bytesRead;
                        long totalBytesRead = 0;
                        long fileSize = 0;

                        try {
                            fileSize = requireActivity().getContentResolver()
                                    .openFileDescriptor(selectedVideoUri, "r").getStatSize();
                        } catch (Exception e) {
                            Log.e("VideoPlayer", "Could not determine file size", e);
                        }

                        // Show a progress dialog for large files
                        AlertDialog progressDialog = null;
                        if (fileSize > 5 * 1024 * 1024) { // 5MB
                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                            builder.setTitle("Preparing Video");
                            builder.setMessage("Please wait...");
                            builder.setCancelable(false);
                            progressDialog = builder.create();
                            progressDialog.show();
                        }

                        final AlertDialog finalProgressDialog = progressDialog;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;

                            // Update progress dialog if needed
                            if (finalProgressDialog != null && fileSize > 0) {
                                final int progress = (int) ((totalBytesRead * 100) / fileSize);
                                if (progress % 10 == 0) { // Update every 10%
                                    requireActivity().runOnUiThread(() -> {
                                        finalProgressDialog.setMessage("Preparing video... " + progress + "%");
                                    });
                                }
                            }
                        }

                        // Ensure all bytes are written to disk
                        outputStream.flush();

                        // Dismiss progress dialog if shown
                        if (finalProgressDialog != null) {
                            finalProgressDialog.dismiss();
                        }
                    }

                    Log.d("VideoPlayer", "File created, size: " + videoFile.length() + " bytes");

                    // Create a content URI via FileProvider
                    Uri fileUri = FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            videoFile);

                    Log.d("VideoPlayer", "FileProvider URI: " + fileUri.toString());

                    // Create explicit intent for video playback
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, "video/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    // Check for available apps
                    if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                        Log.d("VideoPlayer", "Starting video activity");
                        startActivity(intent);
                    } else {
                        // Try with a more specific MIME type
                        intent.setDataAndType(fileUri, "video/mp4");
                        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                            Log.d("VideoPlayer", "Starting video activity with specific MIME type");
                            startActivity(intent);
                        } else {
                            // Try with Intent.createChooser as a last resort
                            try {
                                Log.d("VideoPlayer", "Trying createChooser approach");
                                startActivity(Intent.createChooser(intent, "Play video using"));
                            } catch (Exception e) {
                                Log.e("VideoPlayer", "All playback attempts failed", e);
                                Toast.makeText(getContext(), "No app available to play video. Please install a video player app.", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("VideoPlayer", "Error playing video", e);
                    Toast.makeText(getContext(), "Error playing video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        videoContainer.addView(container);
        uploadVideoButton.setText("Change Video");
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showImagePickerDialog() {
        if (encodedImages.size() >= MAX_IMAGES) {
            Toast.makeText(getContext(), "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void showVideoPickerDialog() {
        if (hasVideo) {
            Toast.makeText(getContext(), "Only one video allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Record Video", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Video");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    recordVideo();
                    break;
                case 1:
                    openVideoGallery();
                    break;
                case 2:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void openCamera() {
        if (encodedImages.size() >= MAX_IMAGES) {
            Toast.makeText(getContext(), "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(getContext(), "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        if (encodedImages.size() >= MAX_IMAGES) {
            Toast.makeText(getContext(), "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to open gallery", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void recordVideo() {
        if (hasVideo) {
            Toast.makeText(getContext(), "Only one video allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        // Specify video quality and duration limits
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60); // 1 minute max
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // High quality

        // Save to a specific file to ensure proper format
        File videoFile = new File(requireContext().getExternalCacheDir(), "recorded_video.mp4");
        if (videoFile.exists()) {
            videoFile.delete();
        }

        Uri videoUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                videoFile);

        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);

        if (takeVideoIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            videoRecordLauncher.launch(takeVideoIntent);
        } else {
            Toast.makeText(getContext(), "No video app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openVideoGallery() {
        if (hasVideo) {
            Toast.makeText(getContext(), "Only one video allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("video/*");
            videoGalleryLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to open gallery", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void handleSelectedImage(Uri imageUri) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            int maxDimension = 1024;
            int sampleSize = 1;

            while (options.outWidth / sampleSize > maxDimension ||
                    options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2;
            }

            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap != null) {
                float scale = Math.min(
                        (float) maxDimension / bitmap.getWidth(),
                        (float) maxDimension / bitmap.getHeight()
                );

                if (scale < 1) {
                    int scaledWidth = Math.round(bitmap.getWidth() * scale);
                    int scaledHeight = Math.round(bitmap.getHeight() * scale);
                    bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
                }

                String encodedImage = encodeImage(bitmap);
                encodedImages.add(encodedImage);
                addImageToContainer(bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Image Error", "Failed to process image. Please try another image.");
        }
    }

    private void handleSelectedVideo(Uri videoUri) {
        try {
            selectedVideoUri = videoUri;

            // Get video size - using content resolver for Android 10+
            long videoSize = 0;
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    try (InputStream is = requireActivity().getContentResolver().openInputStream(videoUri)) {
                        if (is != null) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                videoSize += bytesRead;
                            }
                        }
                    }
                } else {
                    videoSize = getVideoSize(videoUri);
                }
            } catch (Exception e) {
                Log.e("VideoUpload", "Error getting video size", e);
            }

            if (videoSize > MAX_VIDEO_SIZE_MB * 1024 * 1024) {
                showError("Video too large", "Please select a video smaller than " + MAX_VIDEO_SIZE_MB + "MB");
                return;
            }

            // Get video duration safely
            long duration = getVideoDuration(videoUri);
            if (duration > MAX_VIDEO_DURATION_MS) {
                showError("Video too long", "Please select a video shorter than 1 minute");
                return;
            }

            // Get thumbnail safely using MediaMetadataRetriever rather than path
            Bitmap thumbnail = getVideoThumbnailSafe(videoUri);
            if (thumbnail != null) {
                addVideoToContainer(thumbnail);
                hasVideo = true;
                compressAndEncodeVideo(videoUri);
            } else {
                showError("Video Error", "Failed to create video thumbnail");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Video Error", "Failed to process video: " + e.getMessage());
        }
    }

    private Bitmap getVideoThumbnailSafe(Uri videoUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(requireContext(), videoUri);
            return retriever.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private long getVideoSize(Uri videoUri) {
        try {
            return requireActivity().getContentResolver().openFileDescriptor(videoUri, "r").getStatSize();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private long getVideoDuration(Uri videoUri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(requireContext(), videoUri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return Long.parseLong(durationStr);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            retriever.release();
        }
    }

    private Bitmap getVideoThumbnail(Uri videoUri) {
        try {
            return ThumbnailUtils.createVideoThumbnail(
                    getPathFromUri(videoUri),
                    MediaStore.Images.Thumbnails.MINI_KIND);
        } catch (Exception e) {
            e.printStackTrace();

            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(requireContext(), videoUri);
                return retriever.getFrameAtTime();
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        try {
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
            File videoFile = File.createTempFile("video", ".mp4", requireContext().getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(videoFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
            return videoFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void compressAndEncodeVideo(Uri videoUri) {
        encodedVideo = "pending"; // Mark as pending

        AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Processing Video")
                .setMessage("Preparing video for upload...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Execute in background thread
        new Thread(() -> {
            try {
                // Get a file path from the URI
                String filePath = getRealPathFromURI(videoUri);
                File videoFile;

                if (filePath != null) {
                    videoFile = new File(filePath);
                } else {
                    // If we can't get the path directly, copy to a temp file
                    videoFile = createTempFileFromUri(videoUri);
                }

                if (videoFile == null || !videoFile.exists()) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showError("Video Error", "Could not access video file");
                        encodedVideo = null; // Reset pending status
                        hasVideo = false;
                        videoContainer.removeAllViews();
                    });
                    return;
                }

                // Check the file size
                long fileSize = videoFile.length();
                if (fileSize > 50 * 1024 * 1024) { // 50MB limit
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showError("Video Error", "Video file is too large (max 50MB)");
                        encodedVideo = null; // Reset pending status
                        hasVideo = false;
                        videoContainer.removeAllViews();
                    });
                    return;
                }

                // Create a copy in the cache directory for later upload
                preparedVideoFile = new File(requireContext().getCacheDir(), "video_to_upload_" + System.currentTimeMillis() + ".mp4");

                // Copy the file
                try (FileInputStream in = new FileInputStream(videoFile);
                     FileOutputStream out = new FileOutputStream(preparedVideoFile)) {

                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                // Mark video as ready for upload
                encodedVideo = "ready";

                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Video ready for submission", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("VideoUpload", "Error processing video", e);
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showError("Video Error", "Failed to process video: " + e.getMessage());
                    encodedVideo = null; // Reset pending status
                    hasVideo = false;
                    videoContainer.removeAllViews();
                });
            }
        }).start();
    }

    private String uploadVideoDirectly(File videoFile) {
        try {
            // Create a multipart HTTP request to Cloudinary using BuildConfig values instead of hardcoded credentials
            String cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME;
            String apiKey = BuildConfig.CLOUDINARY_API_KEY;
            String apiSecret = BuildConfig.CLOUDINARY_API_SECRET;
            String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/video/upload";

            // Generate timestamp and signature
            long timestamp = System.currentTimeMillis() / 1000;
            String folder = "incident_videos";
            String publicId = "incident_video_" + timestamp + "_" + generateRandomString(8);

            // Create signature string (without resource_type)
            String signatureStr = "folder=" + folder + "&public_id=" + publicId + "&timestamp=" + timestamp + apiSecret;
            String signature = generateSHA1(signatureStr);

            // Boundary for multipart form
            String boundary = "----" + System.currentTimeMillis();

            // Create connection
            URL url = new URL(uploadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);

            // Create output stream
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

            // Add form fields
            addFormField(outputStream, boundary, "api_key", apiKey);
            addFormField(outputStream, boundary, "timestamp", String.valueOf(timestamp));
            addFormField(outputStream, boundary, "signature", signature);
            addFormField(outputStream, boundary, "folder", folder);
            addFormField(outputStream, boundary, "public_id", publicId);
            addFormField(outputStream, boundary, "resource_type", "video");

            // Add file field
            addFilePart(outputStream, boundary, "file", videoFile);

            // End of multipart form
            outputStream.writeBytes("--" + boundary + "--\r\n");
            outputStream.flush();
            outputStream.close();

            // Get response code
            int responseCode = connection.getResponseCode();
            Log.d("VideoUpload", "Response code: " + responseCode);

            // Read response
            InputStream inputStream;
            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON response
            String responseStr = response.toString();
            Log.d("VideoUpload", "Response: " + responseStr);

            JSONObject jsonResponse = new JSONObject(responseStr);
            if (jsonResponse.has("secure_url")) {
                String secureUrl = jsonResponse.getString("secure_url");
                return secureUrl;
            } else if (jsonResponse.has("error")) {
                JSONObject error = jsonResponse.getJSONObject("error");
                String errorMessage = error.getString("message");
                Log.e("VideoUpload", "Cloudinary error: " + errorMessage);
                throw new IOException("Cloudinary error: " + errorMessage);
            }

            return null;
        } catch (Exception e) {
            Log.e("VideoUpload", "Error uploading video", e);
            return null;
        }
    }

    // Helper method to add a form field to multipart request
    private void addFormField(DataOutputStream outputStream, String boundary, String name, String value) throws IOException {
        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        outputStream.writeBytes(value + "\r\n");
    }

    // Helper method to add a file to multipart request
    private void addFilePart(DataOutputStream outputStream, String boundary, String fieldName, File file) throws IOException {
        String fileName = file.getName();
        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n");
        outputStream.writeBytes("Content-Type: video/mp4\r\n\r\n");

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        fileInputStream.close();
        outputStream.writeBytes("\r\n");
    }


    // Helper method to create a temporary file from a content URI
    private File createTempFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return null;
        }

        File outputFile = new File(requireContext().getCacheDir(), "upload_video.mp4");
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();

        return outputFile;
    }

    // Helper for SHA1 signature generation
    private String generateSHA1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // Helper to generate random string
    private String generateRandomString(int length) {
        String alphanumeric = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            result.append(alphanumeric.charAt(random.nextInt(alphanumeric.length())));
        }

        return result.toString();
    }

    // Helper method to get real path from URI
    private String getRealPathFromURI(Uri contentUri) {
        // For Android 10 (API 29) and above, we can't directly get the file path
        // Instead, we need to work with the URI and copy the file if needed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                return createTempFileFromUri(contentUri).getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        // For older Android versions, we can try to get the path directly
        String[] proj = {MediaStore.Video.Media.DATA};
        try {
            Cursor cursor = requireActivity().getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(column_index);
                cursor.close();
                return path;
            }
        } catch (Exception e) {
            Log.e("VideoUpload", "Error getting real path", e);
        }

        return contentUri.getPath();
    }

    private void submitReport() {
        String title = titleSpinner.getSelectedItem().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            showError("Authentication Error", "Please log in again");
            return;
        }

        // Check if video is still being processed
        if ("pending".equals(encodedVideo)) {
            showError("Video Processing", "Please wait for video processing to complete");
            return;
        }

        AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Submitting Report")
                .setMessage("Please wait while we upload your report...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        submitButton.setEnabled(false);
        completeSubmitReport(userId, title, description, progressDialog);
    }

    private void completeSubmitReport(int userId, String title, String description, AlertDialog progressDialog) {
        // Start a background thread for network operations
        new Thread(() -> {
            String cloudinaryVideoUrl = null;

            try {
                // Check if we need to upload a video first
                if ("ready".equals(encodedVideo) && preparedVideoFile != null && preparedVideoFile.exists()) {
                    // Update progress dialog on UI thread
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.setMessage("Uploading video...");
                    });

                    Log.d("IncidentReport", "Starting video upload, file size: " + preparedVideoFile.length());

                    // Upload the video - this network operation now happens on a background thread
                    cloudinaryVideoUrl = uploadVideoDirectly(preparedVideoFile);

                    Log.d("IncidentReport", "Video upload result URL: " + cloudinaryVideoUrl);

                    if (cloudinaryVideoUrl == null || cloudinaryVideoUrl.isEmpty()) {
                        requireActivity().runOnUiThread(() -> {
                            submitButton.setEnabled(true);
                            progressDialog.dismiss();
                            showError("Upload Failed", "Could not upload video to server. Check your internet connection and try again.");
                        });
                        return;
                    }

                    // Set the video URL for submission
                    final String videoUrl = cloudinaryVideoUrl;
                    encodedVideo = videoUrl;
                }

                // Prepare the JSON data
                JsonObject mediaObject = new JsonObject();
                JsonArray imagesJsonArray = new JsonArray();
                for (String img : encodedImages) {
                    imagesJsonArray.add(img);
                }
                mediaObject.add("images", imagesJsonArray);

                // For video, use the Cloudinary URL if available
                if (encodedVideo != null && !"pending".equals(encodedVideo) && !"ready".equals(encodedVideo)) {
                    mediaObject.addProperty("video", encodedVideo);
                    Log.d("IncidentReport", "Including video URL in submission: " + encodedVideo);
                }

                final String mediaJsonStr = mediaObject.toString();

                // Log request details
                Log.d("IncidentReport", "Media JSON: " + mediaJsonStr);

                // Update UI and make API call on UI thread
                final int finalUserId = userId;
                final String finalTitle = title;
                final String finalDescription = description;

                requireActivity().runOnUiThread(() -> {
                    progressDialog.setMessage("Submitting report...");

                    // Create API call
                    ApiService apiService = RetrofitClient.getApiService();
                    Call<IncidentReportResponse> call = apiService.submitIncidentReport(
                            finalUserId,
                            finalTitle,
                            finalDescription,
                            mediaJsonStr
                    );

                    // Execute API call
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
                                            "Your incident report has been successfully submitted and will be reviewed shortly.",
                                            null);
                                } else {
                                    String errorMessage = reportResponse.getMessage();
                                    Log.e("IncidentReport", "API Error: " + errorMessage);

                                    if (errorMessage != null && errorMessage.contains("Cloudinary")) {
                                        showError("Upload Error",
                                                "Failed to upload media. Please try again later or contact support if the problem persists.");
                                    } else {
                                        showError("Submission Failed",
                                                errorMessage != null ? errorMessage : "Failed to submit report. Please try again.");
                                    }
                                }
                            } else {
                                try {
                                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                                    Log.e("IncidentReport", "HTTP Error: " + response.code() + " - " + errorBody);
                                    showError("Submission Failed", "Server error: " + errorBody);
                                } catch (IOException e) {
                                    Log.e("IncidentReport", "Error reading error body", e);
                                    showError("Submission Failed",
                                            "Failed to submit report. Please check your connection and try again.");
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<IncidentReportResponse> call, Throwable t) {
                            submitButton.setEnabled(true);
                            progressDialog.dismiss();
                            Log.e("IncidentReport", "Network failure", t);
                            showError("Network Error", "Error: " + t.getMessage());
                        }
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("IncidentReport", "Exception in submit", e);
                requireActivity().runOnUiThread(() -> {
                    submitButton.setEnabled(true);
                    progressDialog.dismiss();
                    showError("Error", "Failed to prepare media data: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void clearForm() {
        titleSpinner.setSelection(0);
        descriptionEditText.setText("");
        selectedImagesContainer.removeAllViews();
        videoContainer.removeAllViews();
        encodedImages.clear();
        encodedVideo = null;
        selectedVideoUri = null;
        hasVideo = false;

        // Delete the prepared video file if it exists
        if (preparedVideoFile != null && preparedVideoFile.exists()) {
            preparedVideoFile.delete();
        }
        preparedVideoFile = null;

        uploadImageButton.setText("Upload Images");
        uploadVideoButton.setText("Upload Video");
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int quality = 80;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);

        int maxSizeBytes = 500000;
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

    // Interface for Cloudinary video upload
    public interface VideoUploadService {
        @Multipart
        @POST("v1_1/{cloudName}/video/upload")
        Call<VideoUploadResponse> uploadVideo(
                @Part("api_key") RequestBody apiKey,
                @Part("timestamp") RequestBody timestamp,
                @Part("signature") RequestBody signature,
                @Part("folder") RequestBody folder,
                @Part("public_id") RequestBody publicId,
                @Part MultipartBody.Part file
        );
    }

    // Response model for Cloudinary video upload
    public static class VideoUploadResponse {
        @SerializedName("secure_url")
        private String secureUrl;

        @SerializedName("error")
        private ErrorInfo error;

        public String getSecureUrl() {
            return secureUrl;
        }

        public ErrorInfo getError() {
            return error;
        }

        public static class ErrorInfo {
            @SerializedName("message")
            private String message;

            public String getMessage() {
                return message;
            }
        }
    }
}