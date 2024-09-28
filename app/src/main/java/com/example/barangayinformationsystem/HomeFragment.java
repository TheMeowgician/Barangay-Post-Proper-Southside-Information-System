package com.example.barangayinformationsystem;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    TextView notableProjectsTextView, seeInfoTextView1, seeInfoTextView2, seeInfoTextView3;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Find the TextView after inflating the layout
        notableProjectsTextView = view.findViewById(R.id.notableProjectsOfOurBarangayTextView);
        seeInfoTextView1 = view.findViewById(R.id.seeInfoTextView1);
        seeInfoTextView2 = view.findViewById(R.id.seeInfoTextView2);
        seeInfoTextView3 = view.findViewById(R.id.seeInfoTextView3);

        seeInfoTextView1.setText("See Info"); // Replace with your actual text

        // Add underline using SpannableStringBuilder
        SpannableStringBuilder spannableStringBuilder1 = new SpannableStringBuilder("See Info");
        spannableStringBuilder1.setSpan(new UnderlineSpan(), 0, spannableStringBuilder1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        seeInfoTextView1.setText(spannableStringBuilder1);
        seeInfoTextView2.setText(spannableStringBuilder1);
        seeInfoTextView3.setText(spannableStringBuilder1);


        // Change the color of "Project"
        String text = notableProjectsTextView.getText().toString();
        SpannableStringBuilder spannableStringBuilder2 = new SpannableStringBuilder(text);
        int start = text.indexOf("Projects");
        int end = start + "Projects".length();
        spannableStringBuilder2.setSpan(new ForegroundColorSpan(Color.parseColor("#61009F")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        notableProjectsTextView.setText(spannableStringBuilder2);

        return view;
    }
}