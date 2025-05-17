package com.example.ecopadal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class Bluetoothcon extends AppCompatActivity {

    private static final String TAG = "Bluetoothcon";
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;
    private OutputStream outputStream = null;
    private BluetoothDevice selectedDevice = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Button lockButton;
    private Button unlockButton;
    private Button btnStartTiming;
    private DatabaseReference databaseReference;
    private double distance = 0.0; // To store the distance retrieved from Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetoothcon);

        checkBluetoothPermissions();

        // Initialize Bluetooth Adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
            finish(); // Exit if no Bluetooth support
            return;
        }

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("DistanceData");

        // Retrieve data from Firebase
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Distance.DistanceData distanceData = dataSnapshot.getValue(Distance.DistanceData.class);
                    if (distanceData != null) {
                        distance = distanceData.distance; // Retrieve the distance
                        Log.d(TAG, "Distance: " + distance);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to retrieve data", error.toException());
            }
        });

        // UI Components
        lockButton = findViewById(R.id.lockButton);
        unlockButton = findViewById(R.id.UnlockButton);
        Button selectDeviceButton = findViewById(R.id.selectDeviceButton);
        btnStartTiming = findViewById(R.id.Starttimming);

        btnStartTiming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (distance > 0) {
                    // Calculate the time based on the distance (100m = 10min)
                    long timeInMinutes = (long) ((distance *10) * 10*60);

                    // Intent to start a new activity
                    Intent intent = new Intent(Bluetoothcon.this, Endjourney.class);

                    // Pass the calculated time to the new activity
                    intent.putExtra("CALCULATED_TIME", timeInMinutes);
                    intent.putExtra("Distance", distance);
                    startActivity(intent);
                    finish(); // Finish current activity if needed
                } else {
                    Toast.makeText(Bluetoothcon.this, "Distance data not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Select a Bluetooth device
        selectDeviceButton.setOnClickListener(v -> showBluetoothDevicesDialog());

        // Unlock functionality
        unlockButton.setOnClickListener(v -> {
            if (selectedDevice == null) {
                Toast.makeText(getApplicationContext(), "No device selected", Toast.LENGTH_SHORT).show();
                return;
            }

            if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                connectToDevice(selectedDevice);
            }

            try {
                if (outputStream != null) {
                    outputStream.write('U'); // send 'U' to unlock
                    Toast.makeText(getApplicationContext(), "Unlocked", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Output stream is null", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                handleBluetoothError(e);
            }
        });

        // Lock functionality
        lockButton.setOnClickListener(v -> {
            if (selectedDevice == null) {
                Toast.makeText(getApplicationContext(), "No device selected", Toast.LENGTH_SHORT).show();
                return;
            }

            if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                connectToDevice(selectedDevice);
            }

            try {
                if (outputStream != null) {
                    outputStream.write('L'); // send 'L' to lock
                    Toast.makeText(getApplicationContext(), "Locked", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Output stream is null", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                handleBluetoothError(e);
            }
        });
    }

    private void checkBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
    }

    private void showBluetoothDevicesDialog() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> pairedDevicesSet = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> pairedDevices = new ArrayList<>(pairedDevicesSet);

        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDeviceAdapter adapter = new BluetoothDeviceAdapter(this, pairedDevices);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select your device");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_bluetooth_devices, null);
        ListView listView = dialogView.findViewById(R.id.bluetooth_device_list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = pairedDevices.get(position);
            Toast.makeText(getApplicationContext(), "Selected device: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
            connectToDevice(selectedDevice);
        });

        builder.setView(dialogView);
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }

        try {
            Log.d(TAG, "Attempting to connect to " + device.getName() + " at " + device.getAddress());
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            handleBluetoothError(e);
        }
    }

    private void handleBluetoothError(Exception e) {
        Log.e(TAG, "Bluetooth error: " + e.getMessage(), e);
        Toast.makeText(this, "Bluetooth error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        closeConnection();
    }

    private void closeConnection() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to close connection: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnection();
    }

    private class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
        private final LayoutInflater inflater;
        private final List<BluetoothDevice> devices;

        BluetoothDeviceAdapter(Context context, List<BluetoothDevice> devices) {
            super(context, 0, devices);
            this.inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            BluetoothDevice device = devices.get(position);

            TextView deviceName = convertView.findViewById(android.R.id.text1);
            TextView deviceAddress = convertView.findViewById(android.R.id.text2);

            if (ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return convertView;
            }
            deviceName.setText(device.getName());
            deviceAddress.setText(device.getAddress());

            return convertView;
        }
    }
}
