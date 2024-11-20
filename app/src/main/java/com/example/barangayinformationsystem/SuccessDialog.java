package com.example.barangayinformationsystem;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.widget.TextView;

import com.example.barangayinformationsystem.R;

public class SuccessDialog {

    public static void showSuccess(Context context, String message) {
        showSuccess(context, message, null, 2000); // Default 2 seconds duration
    }

    public static void showSuccess(Context context, String message, Intent nextActivity) {
        showSuccess(context, message, nextActivity, 2000); // Default 2 seconds duration
    }

    public static void showSuccess(Context context, String message, Intent nextActivity, int durationMs) {
        Dialog successDialog = new Dialog(context);
        successDialog.setContentView(R.layout.dialog_success);
        successDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        successDialog.setCancelable(false);

        // Set custom message if provided
        TextView messageText = successDialog.findViewById(R.id.successMessageText);
        if (messageText != null) {
            messageText.setText(message);
        }

        // Show dialog
        successDialog.show();

        // Dismiss after duration and handle navigation
        new Handler().postDelayed(() -> {
            successDialog.dismiss();
            if (nextActivity != null) {
                context.startActivity(nextActivity);
            }
        }, durationMs);
    }
}