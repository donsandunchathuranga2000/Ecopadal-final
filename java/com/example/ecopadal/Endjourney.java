package com.example.ecopadal;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class Endjourney extends AppCompatActivity {

    private TextView tvTimeRemaining, tvDistanceLeft, tvDistanceTotal;
    private Button btnFinish;
    private StreetViewPanoramaView streetViewPanoramaView;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Vibrator vibrator;
    private double totalDistance; // total distance in km
    private double distanceLeft; // remaining distance in km

    private OutputStream outputStream; // Assume this is initialized elsewhere
    private CountDownTimer countDownTimer; // Reference to the CountDownTimer

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_endjourney);

        tvTimeRemaining = findViewById(R.id.tv_time_remaining);
        tvDistanceLeft = findViewById(R.id.tv_distance_left);
        tvDistanceTotal = findViewById(R.id.tv_distance_total);
        btnFinish = findViewById(R.id.btn_finish);

        // Initialize Vibrator
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize Street View
        streetViewPanoramaView = findViewById(R.id.street_view_panorama);
        streetViewPanoramaView.onCreate(savedInstanceState);

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        streetViewPanoramaView.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
            @Override
            public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
                updateStreetViewLocation(panorama);
            }
        });

        // Get the passed time from the Intent
        Intent intent = getIntent();
        long passedTimeInSeconds = intent.getLongExtra("CALCULATED_TIME", 0);
        double distance = intent.getDoubleExtra("Distance", 0);
        Log.d(TAG, "Passed Time in Seconds: " + passedTimeInSeconds);

        // Set up the countdown timer
        if (passedTimeInSeconds > 0) {
            setupCountDownTimer(passedTimeInSeconds);
        } else {
            tvTimeRemaining.setText("Invalid time"); // Handle invalid time
        }

        // Display initial values
        tvDistanceTotal.setText(String.format("%.1f km", distance));

        btnFinish.setOnClickListener(v -> {
            // Stop the countdown timer if it's running
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            // Finish the ride
            finishRide();
        });
    }

    @SuppressLint("MissingPermission")
    private void updateStreetViewLocation(StreetViewPanorama panorama) {
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        panorama.setPosition(currentLocation);
                        panorama.setStreetNamesEnabled(true);
                        panorama.setUserNavigationEnabled(true);
                        panorama.setZoomGesturesEnabled(true);
                    } else {
                        Log.e(TAG, "Location is null");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get location: ", e));
    }

    private void setupCountDownTimer(long totalTimeInSeconds) {
        countDownTimer = new CountDownTimer(totalTimeInSeconds * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                updateRemainingTime(secondsLeft);

                if (secondsLeft <= 300 && secondsLeft > 299) { // 5 minutes in seconds
                    // Vibrate the phone
                    if (vibrator != null) {
                        vibrator.vibrate(500); // Vibrate for 500 milliseconds
                    }
                    // Change text color to red
                    tvTimeRemaining.setTextColor(Color.RED);
                }
            }

            public void onFinish() {
                tvTimeRemaining.setText("00:00:00"); // Timer finished
                sendBluetoothMessage("U");
                // Send data via Bluetooth
            }
        }.start();
    }

    private void updateRemainingTime(long secondsLeft) {
        int hours = (int) (secondsLeft / 3600);
        int minutes = (int) ((secondsLeft % 3600) / 60);
        int seconds = (int) (secondsLeft % 60);

        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        tvTimeRemaining.setText(timeFormatted);
    }

    private void finishRide() {
        // Handle ride finish logic
        showThankYouPopup();
    }

    private void showThankYouPopup() {
        // Inflate the custom layout using the LayoutInflater
        View view = LayoutInflater.from(this).inflate(R.layout.card_popup, null);

        // Find the OK button in the inflated layout
        Button btnOk = view.findViewById(R.id.btn_ok);

        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false); // Prevent the user from dismissing the dialog by clicking outside

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set the onClick listener for the OK button
        btnOk.setOnClickListener(v -> {
            dialog.dismiss(); // Close the dialog
            // Return to MapsActivity
            Intent intent = new Intent(Endjourney.this, CameraActivity.class);
            startActivity(intent);
            finish(); // Finish the current activity
        });
    }

    private void sendBluetoothMessage(String message) {
        if (outputStream != null) {
            try {
                outputStream.write(message.getBytes());
                outputStream.flush();
                Log.d(TAG, "Message sent: " + message);
            } catch (IOException e) {
                Log.e(TAG, "Error sending message via Bluetooth: ", e);
            }
        } else {
            Log.e(TAG, "OutputStream is null. Cannot send message.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        streetViewPanoramaView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        streetViewPanoramaView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        streetViewPanoramaView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        streetViewPanoramaView.onLowMemory();
    }
}

