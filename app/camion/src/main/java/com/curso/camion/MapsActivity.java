package com.curso.camion;

import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.curso.camion.models.TruckLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedClient;
    private static final int REQUEST_CODE = 101;
    private Map<String, Marker> markerIdentifierMap = new HashMap<>();
    private FirebaseFirestore db;
    private Marker currentTruckMarker;
    private String currentTruckId;
    private boolean isZoomSet = false;
    private Polyline currentRoute;
    private List<LatLng> routePoints = new ArrayList<>();

    private ImageView btnDashboard, btnMap, btnSettings;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.curso.camion.R.layout.activity_maps);

        db = FirebaseFirestore.getInstance();
        currentTruckId = getCurrentTruckId();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(com.curso.camion.R.id.map);
        mapFragment.getMapAsync(this);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        getLocationUpdates();

        btnDashboard = findViewById(com.curso.camion.R.id.btnDashboard);
        btnMap = findViewById(com.curso.camion.R.id.btnMap);
        btnSettings = findViewById(com.curso.camion.R.id.btnSettings);

        btnDashboard.setOnClickListener(v -> goToDashboard());
        btnMap.setOnClickListener(v -> goToMap());
        btnSettings.setOnClickListener(v -> goToSettings());
    }


    private void goToDashboard() {
        Intent intent = new Intent(com.curso.camion.MapsActivity.this, AdminConfiguracion.class);
        startActivity(intent);
    }

    private void goToMap() {
        Intent intent = new Intent(com.curso.camion.MapsActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    private void goToSettings() {
        Intent intent = new Intent(com.curso.camion.MapsActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private String getCurrentTruckId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid(); // Asumiendo que el ID del usuario se usa como ID del camión
        } else {
            // Manejar el caso cuando no hay un usuario autenticado
            return "unknown_truck";
        }
    }

    private void getLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                updateMap(location);
                saveLocationToFirestore(location);
                updateRoute(location);
            }
        }
    };

    private void updateMap(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (currentTruckMarker != null) {
            currentTruckMarker.setPosition(latLng);
        } else {
            currentTruckMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Mi ubicación actual"));
        }

        if (!isZoomSet) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            isZoomSet = true;
        }
    }

    private void updateRoute(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        routePoints.add(latLng);
        if (currentRoute != null) {
            currentRoute.setPoints(routePoints);
        } else {
            currentRoute = mMap.addPolyline(new PolylineOptions().addAll(routePoints).width(5).color(R.color.teal_200));
        }
    }

    private void saveLocationToFirestore(Location location) {
        TruckLocation truckLocation = new TruckLocation(location.getLatitude(), location.getLongitude(), currentTruckId);
        db.collection("truck_locations").document(currentTruckId).set(truckLocation)
                .addOnSuccessListener(aVoid -> {
                    // Éxito al guardar la ubicación
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(com.curso.camion.MapsActivity.this, "Error al guardar la ubicación", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchOtherTrucksLocations() {
        db.collection("truck_locations").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(com.curso.camion.MapsActivity.this, "Error al obtener ubicaciones de camiones", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshots != null) {
                for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
                    TruckLocation truckLocation = snapshot.toObject(TruckLocation.class);
                    if (truckLocation != null) {
                        String truckId = truckLocation.getTruckId();
                        if (truckId != null && !truckId.equals(currentTruckId)) {
                            Double latitude = truckLocation.getLatitude();
                            Double longitude = truckLocation.getLongitude();
                            if (latitude != null && longitude != null) {
                                LatLng latLng = new LatLng(latitude, longitude);
                                if (markerIdentifierMap.containsKey(truckId)) {
                                    Marker marker = markerIdentifierMap.get(truckId);
                                    marker.setPosition(latLng);
                                } else {
                                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Camión: " + truckId);
                                    Marker marker = mMap.addMarker(markerOptions);
                                    markerIdentifierMap.put(truckId, marker);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        fetchOtherTrucksLocations();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationUpdates();
            }
        }
    }
}