package com.example.barangayinformationsystem;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DocumentRequestFragment extends Fragment {

    public AppCompatButton document_request_barangay_clearance_material_cardview_select_button,
            document_request_cedula_material_cardview_select_button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_document_request, container, false);
        document_request_barangay_clearance_material_cardview_select_button = view.findViewById(R.id.document_request_barangay_clearance_material_cardview_select_button);
        document_request_cedula_material_cardview_select_button = view.findViewById(R.id.document_request_cedula_material_cardview_select_button);

        document_request_barangay_clearance_material_cardview_select_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), BarangayClearanceFormActivity.class);
                startActivity(intent);
            }
        });

        document_request_cedula_material_cardview_select_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CedulaFormActivity.class);
                startActivity(intent);
            }
        });

        return view;

    }

}