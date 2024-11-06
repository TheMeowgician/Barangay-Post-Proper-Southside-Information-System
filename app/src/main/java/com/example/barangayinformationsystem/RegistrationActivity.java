package com.example.barangayinformationsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageButton;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Year;

public class RegistrationActivity extends AppCompatActivity {

    ImageButton backImageButton;
    TextInputEditText birthDateTextInputEditText;
    TextInputEditText firstNameTextInputEditText;
    TextInputEditText lastNameTextInputEditText;
    TextInputEditText usernameTextInputEditText;
    TextInputEditText passwordTextInputEditText;
    TextInputEditText confirmPasswordTextInputEditText;
    TextInputEditText ageTextInputEditText;
    TextInputEditText houseNumberTextInputEditText;
    TextInputEditText zoneTextInputEditText;
    TextInputEditText streetTextInputEditText;

    TextInputLayout birthdateTextInputLayout;
    TextInputLayout firstNameTextInputLayout;
    TextInputLayout lastNameTextInputLayout;
    TextInputLayout usernameTextInputLayout;
    TextInputLayout passwordTextInputLayout;
    TextInputLayout confirmPasswordTextInputLayout;
    TextInputLayout ageTextInputLayout;
    TextInputLayout houseNumberTextInputLayout;
    TextInputLayout zoneTextInputLayout;
    TextInputLayout streetTextInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        initilizeComponents();
    }

    public void openDialog(View view) {//This function opens date picker dialog

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                birthDateTextInputEditText.setText(String.valueOf(day) + "/" + String.valueOf(month + 1) + "/" + String.valueOf(year));
            }
        }, 2024, 1, 18);

        datePickerDialog.show();
    }

    public void back(View view) {//This function will take the user back to the last activity
        finish();
    }

    private void initilizeComponents() {

        backImageButton = findViewById(R.id.backImageButton);

        birthDateTextInputEditText = findViewById(R.id.birthDateTextInputEditText);
        firstNameTextInputEditText = findViewById(R.id.firstNameTextInputEditText);
        lastNameTextInputEditText = findViewById(R.id.lastNameTextInputEditText);
        usernameTextInputEditText = findViewById(R.id.usernameTextInputEditText);
        passwordTextInputEditText = findViewById(R.id.passwordTextInputEditText);
        confirmPasswordTextInputEditText = findViewById(R.id.confirmPasswordTextInputEditText);
        ageTextInputEditText = findViewById(R.id.ageTextInputEditText);
        houseNumberTextInputEditText = findViewById(R.id.houseNumberTextInputEditText);
        zoneTextInputEditText = findViewById(R.id.zoneTextInputEditText);
        streetTextInputEditText = findViewById(R.id.streetTextInputEditText);

        birthdateTextInputLayout = findViewById(R.id.birthDateTextInputLayout);
        firstNameTextInputLayout = findViewById(R.id.firstNameTextInputLayout);
        lastNameTextInputLayout = findViewById(R.id.lastNameTextInputLayout);
        usernameTextInputLayout = findViewById(R.id.usernameTextInputLayout);
        passwordTextInputLayout = findViewById(R.id.passwordTextInputLayout);
        confirmPasswordTextInputLayout = findViewById(R.id.confirmPasswordTextInputLayout);
        ageTextInputLayout = findViewById(R.id.ageTextInputLayout);
        houseNumberTextInputLayout = findViewById(R.id.houseNumberTextInputLayout);
        zoneTextInputLayout = findViewById(R.id.zoneTextInputLayout);
        streetTextInputLayout = findViewById(R.id.streetTextInputLayout);

        removeTextInputLayoutAnimation();

    }

    private void removeTextInputLayoutAnimation() {

        firstNameTextInputLayout.setHintAnimationEnabled(false);
        firstNameTextInputLayout.setHintEnabled(false);

        lastNameTextInputLayout.setHintAnimationEnabled(false);
        lastNameTextInputLayout.setHintEnabled(false);

        usernameTextInputLayout.setHintAnimationEnabled(false);
        usernameTextInputLayout.setHintEnabled(false);

        birthdateTextInputLayout.setHintAnimationEnabled(false);
        birthdateTextInputLayout.setHintEnabled(false);

        passwordTextInputLayout.setHintAnimationEnabled(false);
        passwordTextInputLayout.setHintEnabled(false);

        confirmPasswordTextInputLayout.setHintAnimationEnabled(false);
        confirmPasswordTextInputLayout.setHintEnabled(false);

        ageTextInputLayout.setHintAnimationEnabled(false);
        ageTextInputLayout.setHintEnabled(false);

        houseNumberTextInputLayout.setHintAnimationEnabled(false);
        houseNumberTextInputLayout.setHintEnabled(false);

        zoneTextInputLayout.setHintAnimationEnabled(false);
        zoneTextInputLayout.setHintEnabled(false);

        streetTextInputLayout.setHintAnimationEnabled(false);
        streetTextInputLayout.setHintEnabled(false);

    }

}