package com.example.barangayinformationsystem;

import android.app.AlertDialog;
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

import java.util.Calendar;
import java.util.TimeZone;

public class DocumentRequestFragment extends Fragment {

    // PROGRAMMER CONTROL: Set this to false to disable office hours restriction
    private static final boolean ENABLE_OFFICE_HOURS_RESTRICTION = true;

    public AppCompatButton document_request_barangay_clearance_material_cardview_select_button,
            document_request_cedula_material_cardview_select_button,
            document_request_barangay_certification_material_cardview_select_button,
            document_request_certificate_of_indigency_material_cardview_select_button,
            document_request_first_time_job_certificate_material_cardview_select_button;

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
        document_request_barangay_certification_material_cardview_select_button = view.findViewById(R.id.document_request_barangay_certification_material_cardview_select_button);
        document_request_certificate_of_indigency_material_cardview_select_button = view.findViewById(R.id.document_request_certificate_of_indigency_material_cardview_select_button);
        document_request_first_time_job_certificate_material_cardview_select_button = view.findViewById(R.id.document_request_first_time_job_certificate_material_cardview_select_button);

        document_request_barangay_clearance_material_cardview_select_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canProcessRequest()) {
                    Intent intent = new Intent(getActivity(), BarangayClearanceFormActivity.class);
                    startActivity(intent);
                }
            }
        });

        document_request_cedula_material_cardview_select_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canProcessRequest()) {
                    Intent intent = new Intent(getActivity(), CedulaFormActivity.class);
                    startActivity(intent);
                }
            }
        });

        document_request_barangay_certification_material_cardview_select_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canProcessRequest()) {
                    Intent intent = new Intent(getActivity(), BarangayCertificationFormActivity.class);
                    startActivity(intent);
                }
            }
        });

        document_request_certificate_of_indigency_material_cardview_select_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canProcessRequest()) {
                    Intent intent = new Intent(getActivity(), CertificateOfIndigencyFormActivity.class);
                    startActivity(intent);
                }
            }
        });

        document_request_first_time_job_certificate_material_cardview_select_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canProcessRequest()) {
                    Intent intent = new Intent(getActivity(), FirstTimeJobCertificateFormActivity.class);
                    startActivity(intent);
                }
            }
        });

        return view;
    }

    /**
     * Checks if the request can be processed based on office hours and the global flag
     */
    private boolean canProcessRequest() {
        // If office hours restriction is disabled, always allow processing
        if (!ENABLE_OFFICE_HOURS_RESTRICTION) {
            return true;
        }

        // Otherwise, check if within office hours
        if (isWithinOfficeHours()) {
            return true;
        } else {
            showOfficeHoursDialog();
            return false;
        }
    }

    /**
     * Check if current time is within office hours (Monday-Friday, 8AM-5PM Manila time)
     */
    private boolean isWithinOfficeHours() {
        // Set the timezone to Manila/Philippines
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Manila");
        Calendar calendar = Calendar.getInstance(timeZone);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        // Check if day is Monday through Friday (Calendar.MONDAY is 2, Calendar.FRIDAY is 6)
        boolean isWeekday = (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY);

        // Check if time is between 8AM (8:00) and 5PM (17:00)
        boolean isOfficeHours = (hourOfDay >= 8 && hourOfDay < 17);

        return isWeekday && isOfficeHours;
    }

    /**
     * Show dialog explaining office hours
     */
    private void showOfficeHoursDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Office Hours")
                .setMessage("Document requests can only be processed during office hours:\n\nMonday to Friday\n8:00 AM to 5:00 PM")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}