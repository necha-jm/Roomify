package com.app.roomify;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class LocationMap extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_FINE_CODE = 100;
    private BottomSheetBehavior<CardView> bottomSheetBehavior;


    private GoogleMap myMap;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_map);

        CardView bottomSheet = findViewById(R.id.floating_layout);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Set initial state (collapsed)
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Optional: set peek height
        bottomSheetBehavior.setPeekHeight(200); // adjust as you like

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermission();
    }

    // 🔐 Ask permission immediately
    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_FINE_CODE
            );
        } else {
            getLastLocation();
        }
    }

    // 📍 Get last known location
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location != null) {
                        currentLocation = location;

                        SupportMapFragment mapFragment =
                                (SupportMapFragment) getSupportFragmentManager()
                                        .findFragmentById(R.id.map);

                        if (mapFragment != null) {
                            mapFragment.getMapAsync(this);
                        }

                    } else {
                        Toast.makeText(
                                this,
                                "Turn on GPS to get location",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // 🗺️ Map ready
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        if (currentLocation == null) return;

        LatLng myLocation = new LatLng(
                currentLocation.getLatitude(),
                currentLocation.getLongitude()
        );

        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));

        myMap.addMarker(new MarkerOptions()
                .position(myLocation)
                .title("My Location")
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
    }

    // 🔄 Permission result
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_FINE_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getLastLocation();

            } else {
                Toast.makeText(
                        this,
                        "Location permission denied",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}
