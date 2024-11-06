package com.example.barangayinformationsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    ImageButton menuImageButton, closeMenuImageButton;
    DrawerLayout homeDrawerLayout;
    NavigationView navigationView;
    ShapeableImageView profileMiniIconCircleImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initializeComponents();
        replaceFragment(new HomeFragment());
    }

    public void closeNavigationDrawer(View view) {
        homeDrawerLayout.closeDrawer(GravityCompat.START);
    }

    public void openNavigationDrawer(View View) {
        homeDrawerLayout.openDrawer(GravityCompat.START);
    }

    public void openProfileActivity(View view) {
        Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void initializeComponents() {

        menuImageButton = findViewById(R.id.menuImageButton);
        homeDrawerLayout = findViewById(R.id.homeDrawerLayout);
        navigationView = findViewById(R.id.navigationView);
        closeMenuImageButton = findViewById(R.id.closeMenuImageButton);

        profileMiniIconCircleImageView = findViewById(R.id.profileMiniIconCircleImageView);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull @org.jetbrains.annotations.NotNull MenuItem item) {

                int id = item.getItemId();
                item.setChecked(true);
                homeDrawerLayout.closeDrawer(GravityCompat.START);

                if(id == R.id.navHome) {
                    replaceFragment(new HomeFragment());
                } else if(id == R.id.navNews) {
                    replaceFragment(new NewsFragment());
                } else if(id == R.id.navDocumentRequest) {
                    replaceFragment(new DocumentRequestFragment());
                } else if(id == R.id.navHotlineNumbers) {
                    replaceFragment(new HotlineNumbersFragment());
                } else if(id == R.id.navAboutBarangay) {
                    replaceFragment(new AboutBarangayFragment());
                } else if(id == R.id.navBarangayLeaders) {
                    replaceFragment(new BarangayLeadersFragment());
                } else if(id == R.id.navLogOut) {
                    finish();
                }

                return true;
            }
        });

    }

    public void replaceFragment(Fragment fragment) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();

    }

}