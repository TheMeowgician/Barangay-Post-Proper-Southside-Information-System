package com.example.barangayinformationsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DocumentVerificationActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_verification);

        // Set up action bar with back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Verify Document");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button scanButton = findViewById(R.id.btn_scan_qr);
        scanButton.setOnClickListener(v -> {
            // Check camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                startQRScanner();
            }
        });
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan document QR code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
            } else {
                verifyDocument(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void verifyDocument(String signature) {
        // Show loading indicator
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verifying document...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // API endpoint URL
        String url = "https://postproperadminlaravel-a3c73529c6b6.herokuapp.com/api/android/verify-document";

        // Create request body
        RequestBody formBody = new FormBody.Builder()
                .add("signature", signature)
                .build();

        // Create request
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        // Create OkHttp client
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(DocumentVerificationActivity.this,
                            "Verification failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseData = response.body().string();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject json = new JSONObject(responseData);
                        boolean success = json.getBoolean("success");

                        if (success) {
                            // Document verified successfully
                            JSONObject docInfo = json.getJSONObject("document_info");
                            showVerificationResult(docInfo);
                        } else {
                            // Invalid document
                            String message = json.getString("message");
                            showError("Invalid Document", message);
                        }
                    } catch (JSONException e) {
                        showError("Error", "Failed to parse verification response");
                    }
                });
            }
        });
    }

    private void showVerificationResult(JSONObject docInfo) throws JSONException {
        // Create custom dialog to show document details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_document_verified, null);
        builder.setView(dialogView);

        TextView tvDocType = dialogView.findViewById(R.id.tv_document_type);
        TextView tvName = dialogView.findViewById(R.id.tv_name);
        TextView tvAddress = dialogView.findViewById(R.id.tv_address);
        TextView tvPurpose = dialogView.findViewById(R.id.tv_purpose);
        TextView tvIssued = dialogView.findViewById(R.id.tv_issued_date);
        ImageView ivVerified = dialogView.findViewById(R.id.iv_verified_badge);

        tvDocType.setText(docInfo.getString("document_type"));
        tvName.setText(docInfo.getString("requester_name"));
        tvAddress.setText(docInfo.getString("address"));
        tvPurpose.setText(docInfo.getString("purpose"));
        tvIssued.setText("Issued on: " + docInfo.getString("issued_date").substring(0, 10));
        ivVerified.setVisibility(View.VISIBLE);

        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        Button btnOk = dialogView.findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

    private void showError(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}