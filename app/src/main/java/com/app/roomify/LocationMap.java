package com.app.roomify;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationMap extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_FINE_CODE = 100;

    private GoogleMap myMap;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FirebaseFirestore db;
    private SearchView search;
    private ImageView addRoom, apartment;



    private BottomSheetBehavior<CardView> bottomSheetBehavior;

    // IMPORTANT DATA HOLDERS
    private final List<Room> roomsList = new ArrayList<>();
    private final Map<Marker, Room> markerRoomMap = new HashMap<>();

    private boolean shouldRefreshRooms = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map);

        db = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        addRoom = findViewById(R.id.addRoom);
        search = findViewById(R.id.search);
        apartment = findViewById(R.id.apartment);

        CardView bottomSheet = findViewById(R.id.floating_layout);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(200);


        // ✅ MODIFY HERE
        apartment.setOnClickListener(v -> {
            if (roomsList.isEmpty()) {
                Toast.makeText(this, "No rooms available", Toast.LENGTH_SHORT).show();
                return;
            }

            Room room = roomsList.get(0); // get the first available room
            Intent intent = new Intent(LocationMap.this, RoomDetailsActivity.class);
            intent.putExtra("room_id", room.getId()); // pass room_id safely
            startActivity(intent);
        });



        addRoom.setOnClickListener(v ->
                startActivity(new Intent(LocationMap.this, PostRoomActivity.class)));

        requestLocationPermission();
        setupSearch();
    }

    // SEARCH LOCATION
    private void setupSearch() {
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Geocoder geocoder = new Geocoder(LocationMap.this);
                try {
                    List<Address> addressList = geocoder.getFromLocationName(query, 1);
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(
                                address.getLatitude(),
                                address.getLongitude());

                        myMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(latLng, 12));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    // REQUEST LOCATION PERMISSION
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


    // GET USER LOCATION
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
                                "Turn on GPS",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }


    // MAP READY
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        if (currentLocation != null) {
            LatLng myLocation = new LatLng(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());

            myMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(myLocation, 14));

            // USER LOCATION MARKER
            myMap.addMarker(new MarkerOptions()
                    .position(myLocation)
                    .title("My Location")
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

        loadRoomsOnMap();


        // MARKER CLICK → ROOM DETAILS
        myMap.setOnMarkerClickListener(marker -> {
            Room room = markerRoomMap.get(marker);
            if (room == null) return false;

            Intent intent = new Intent(
                    LocationMap.this,
                    RoomDetailsActivity.class);

            intent.putExtra("room_id", room.getId());
            startActivity(intent);
            return true;
        });
    }


// load map
    private void loadRoomsOnMap() {
        db.collection("rooms")
                .whereEqualTo("available", true)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    roomsList.clear();
                    markerRoomMap.clear();
                    myMap.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {

                        Room room = doc.toObject(Room.class);
                        room.setId(doc.getId());

                        if (room.getLatitude() == 0 || room.getLongitude() == 0)
                            continue;

                        LatLng roomLocation = new LatLng(
                                room.getLatitude(),
                                room.getLongitude());

                        Marker marker = myMap.addMarker(
                                new MarkerOptions()
                                        .position(roomLocation)
                                        .title(room.getTitle())
                                        .snippet("Price: $" + room.getPrice())
                                        .icon(BitmapDescriptorFactory
                                                .defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        );

                        markerRoomMap.put(marker, room);
                    }
                });
    }


    // REFRESH MAP WHEN RETURNING
    @Override
    protected void onResume() {
        super.onResume();
        if (myMap != null && shouldRefreshRooms) {
            loadRoomsOnMap();
            shouldRefreshRooms = false;
        }
    }

    // PERMISSION RESULT
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_FINE_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {getLastLocation();
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