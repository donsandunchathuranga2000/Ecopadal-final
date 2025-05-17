package com.example.ecopadal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Distance extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView textTime, textCost;
    private Button btnEnd;
    private Handler handler = new Handler();
    private Marker marker1, marker2;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private FloatingActionButton fabClearEndLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private Polyline polyline;
    private List<LatLng> pathPoints = new ArrayList<>();
    private float distanceInKm = 0.0f;
    private float cost =0.0f;

    // Add Firebase Database reference
    private DatabaseReference databaseReference;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance);

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("DistanceData");

        textTime = findViewById(R.id.text_time_value);
        textCost = findViewById(R.id.text_cost_value);
        btnEnd = findViewById(R.id.btn_end);
        fabClearEndLocation = findViewById(R.id.fab_clear_end_location);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MapFragment", "MapFragment is null");
        }

        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a unique ID for the distance record
                String id = databaseReference.push().getKey();
                DistanceData distanceData = new DistanceData(distanceInKm, cost);

                if (id != null) {
                    // Upload the distance data to Firebase
                    databaseReference.child(id).setValue(distanceData)
                            .addOnSuccessListener(aVoid -> {
                                // Handle success
                                Log.d("Distance", "Distance data uploaded successfully");
                            })
                            .addOnFailureListener(e -> {
                                // Handle failure
                                Log.e("Distance", "Failed to upload distance data", e);
                            });
                }

                Intent intent1 = new Intent(Distance.this, Payment.class);
                startActivity(intent1);
                finish();
            }
        });

        fabClearEndLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearEndLocationMarker();
            }
        });
    }

    private void clearEndLocationMarker() {
        if (marker2 != null) {
            marker2.remove();
            marker2 = null;
            textTime.setText("0.00 Km");
            textCost.setText("Rs.0.00");
            btnEnd.setText("Continue");
        }

        if (polyline != null) {
            polyline.remove();
            polyline = null;
        }
        pathPoints.clear();
    }

    private void stopTracking() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                pathPoints.add(latLng);

                if (marker1 == null) {
                    marker1 = mMap.addMarker(new MarkerOptions().position(latLng).title("Start Location"));
                } else if (marker2 == null) {
                    marker2 = mMap.addMarker(new MarkerOptions().position(latLng).title("End Location"));
                    calculateDistance();
                } else {
                    // Clear the markers and polyline
                    marker1.remove();
                    marker2.remove();
                    if (polyline != null) {
                        polyline.remove();
                        polyline = null;
                    }
                    pathPoints.clear();

                    marker1 = mMap.addMarker(new MarkerOptions().position(latLng).title("Start Location"));
                    marker2 = null;
                    textTime.setText("0.00 Km");
                    textCost.setText("Rs.0.00");
                    btnEnd.setText("Continue");
                }

                // Draw polyline if there are enough points
                if (pathPoints.size() > 1) {
                    if (polyline != null) {
                        polyline.remove();
                    }
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(pathPoints)
                            .color(android.graphics.Color.BLUE)
                            .width(5);
                    polyline = mMap.addPolyline(polylineOptions);
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // Set the start marker at the current location
                    marker1 = mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));

                    // Move the camera to the current location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                }
            }
        });
    }

    private void calculateDistance() {
        if (marker1 != null && marker2 != null) {
            LatLng start = marker1.getPosition();
            LatLng end = marker2.getPosition();

            Location loc1 = new Location("");
            loc1.setLatitude(start.latitude);
            loc1.setLongitude(start.longitude);

            Location loc2 = new Location("");
            loc2.setLatitude(end.latitude);
            loc2.setLongitude(end.longitude);

            float distanceInMeters = loc1.distanceTo(loc2);
            distanceInKm = distanceInMeters / 1000;

            // Update the TextView with the calculated distance
            textTime.setText(String.format(Locale.getDefault(), "%.2f Km", distanceInKm));
            double cost = distanceInKm * 500;
            textCost.setText(String.format(Locale.getDefault(), "Rs.%.2f", cost));

            // Update the text of the button with the cost
            btnEnd.setText(String.format(Locale.getDefault(), "Pay Rs.%.2f", cost));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Log.e("Distance", "Location permission denied");
            }
        }
    }

    // Define a class to represent the distance data
    public static class DistanceData {
        public double distance;
        public double cost;

        public DistanceData() {
            // Default constructor required for calls to DataSnapshot.getValue(DistanceData.class)
        }

        public DistanceData(double distance, double cost) {
            this.distance = distance;
            this.cost = cost;
        }
    }
}
