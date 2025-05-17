package com.example.ecopadal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class Mainmenue extends AppCompatActivity {
    private TextView greetingText;
    private ImageView profileImage;
    private Button buttonOrder;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmenue);


        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        // Initialize views
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_view);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Ensure ActionBar is not null
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            // Handle the case where ActionBar is null
            Toast.makeText(this, "ActionBar not found", Toast.LENGTH_SHORT).show();
        }

        greetingText = findViewById(R.id.greetingText);
        profileImage = findViewById(R.id.profileImage);
        buttonOrder = findViewById(R.id.orderButton);

        // Set up navigation view listener
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                Intent homeIntent = new Intent(Mainmenue.this, Mainmenue.class);
                startActivity(homeIntent);
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (itemId == R.id.payment) {
                Toast.makeText(Mainmenue.this, "Payment", Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (itemId == R.id.settings) {
                Toast.makeText(Mainmenue.this, "Settings", Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (itemId == R.id.logout) {
                FirebaseAuth.getInstance().signOut();
                Intent logoutIntent = new Intent(Mainmenue.this, SignInActivity.class);
                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(logoutIntent);
                finish();
            }

            return true;  // Return true to update the selected item visually
        });

        // Get the currently logged-in user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                greetingText.setText("Hi, " + name);
            } else {
                greetingText.setText("Hi, User");
            }
        } else {
            // Handle the case when the user is not logged in
            greetingText.setText("Hi, Guest");
        }

        // Set a click listener on the order button
        buttonOrder.setOnClickListener(v -> {
            Intent intent = new Intent(Mainmenue.this, Distance.class);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
