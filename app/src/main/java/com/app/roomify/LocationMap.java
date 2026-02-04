package com.app.roomify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.SearchView;
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
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.List;

public class LocationMap extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_FINE_CODE = 100;

    private FirebaseFirestore db;

    private BottomSheetBehavior<CardView> bottomSheetBehavior;


    private GoogleMap myMap;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private SearchView search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_map);

        db = FirebaseFirestore.getInstance();


        CardView bottomSheet = findViewById(R.id.floating_layout);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Set initial state (collapsed)
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Optional: set peek height
        bottomSheetBehavior.setPeekHeight(200); // adjust as you like

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermission();

        search = findViewById(R.id.search);

        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {


                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                String  location = search.getQuery().toString();
                List<Address> addressList = null;

                if(location !=null){
                    Geocoder geocoder = new Geocoder(LocationMap.this);

                    try{
                        addressList = geocoder.getFromLocationName(location,1);
                    } catch (IOException e) {

                        e.printStackTrace();
                    }

                    Address address = addressList.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    myMap.addMarker(new MarkerOptions().position(latLng).title("My Location")
                            .icon(BitmapDescriptorFactory
                                    .defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,10));

                }


                return false;
            }
        });


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

        if (currentLocation != null) {
            LatLng myLocation = new LatLng(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude()
            );

            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14));

            myMap.addMarker(new MarkerOptions()
                    .position(myLocation)
                    .title("My Location")
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
        }

        // 🔥 LOAD ROOMS MARKERS
        loadRoomsOnMap();


        myMap.setOnMarkerClickListener(marker -> {

            // Ignore your own location marker
            if ("My Location".equals(marker.getTitle())) {
                return false;
            }

            Intent intent = new Intent(LocationMap.this, RoomDetailsActivity.class);
            intent.putExtra("room_title", marker.getTitle());
            intent.putExtra("room_info", marker.getSnippet());

            startActivity(intent);
            return false; // keep default behavior
        });
    }



    private void loadRoomsOnMap() {
        db.collection("rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        String title = doc.getString("title");
                        Double price = doc.getDouble("price");

                        if (lat == null || lng == null) continue;

                        LatLng roomLocation = new LatLng(lat, lng);

                        myMap.addMarker(new MarkerOptions()
                                .position(roomLocation)
                                .title(title)
                                .snippet("Price: " + price)
                                .icon(BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load rooms", Toast.LENGTH_SHORT).show()
                );
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