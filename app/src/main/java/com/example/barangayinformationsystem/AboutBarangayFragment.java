package com.example.barangayinformationsystem;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.textview.MaterialTextView;

public class AboutBarangayFragment extends Fragment {

    MaterialTextView about_barangay_our_mission_textview;
    MaterialTextView about_barangay_our_vision_textview;
    MaterialTextView about_barangay_our_goal_textview;
    MaterialTextView about_barangay_historical_background_textview;
    MaterialTextView about_barangay_location_and_physical_features_textview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about_barangay, container, false);
        initializeViews(view);
        underlineTextviews();
        return view;
    }

    private void initializeViews(View view) {

        about_barangay_our_mission_textview = view.findViewById(R.id.about_barangay_our_mission_textview);
        about_barangay_our_vision_textview = view.findViewById(R.id.about_barangay_our_vision_textview);
        about_barangay_our_goal_textview = view.findViewById(R.id.about_barangay_our_goal_textview);
        about_barangay_historical_background_textview = view.findViewById(R.id.about_barangay_historical_background_textview);
        about_barangay_location_and_physical_features_textview = view.findViewById(R.id.about_barangay_location_and_physical_features_textview);

    }

    private void underlineTextviews() {
        underlineTextView(about_barangay_our_mission_textview);
        underlineTextView(about_barangay_our_vision_textview);
        underlineTextView(about_barangay_our_goal_textview);
        underlineTextView(about_barangay_historical_background_textview);
        underlineTextView(about_barangay_location_and_physical_features_textview);
    }

    private void underlineTextView(MaterialTextView textView) {
        if (textView != null) {
            TextPaint paint = textView.getPaint();
            paint.setFlags(paint.getFlags() | Paint.UNDERLINE_TEXT_FLAG);

            // Set underline color directly (works on most API levels)
            paint.setColor(Color.parseColor("#C099D9"));

            // Increase underline thickness (may not work on all API levels)
            // You can experiment with different values to find the desired thickness
            paint.setStrokeWidth(5);

            textView.setText(textView.getText(), MaterialTextView.BufferType.SPANNABLE);
        }
    }
}