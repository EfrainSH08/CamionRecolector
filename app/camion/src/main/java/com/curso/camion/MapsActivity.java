package com.curso.camion;

import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.curso.camion.models.Camion;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedClient;
    private static final int REQUEST_CODE = 101;
    private FirebaseFirestore db;
    private Marker currentTruckMarker;
    private List<LatLng> currentRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        getLocationUpdates();

        currentRoute = new ArrayList<>();
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
            }
        }
    };

    private void updateMap(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (currentTruckMarker == null) {
            currentTruckMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Ubicación actual del camión recolector"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        } else {
            currentTruckMarker.setPosition(latLng);
        }
        currentRoute.add(latLng);
    }

    private void saveLocationToFirestore(Location location) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String camionId = currentUser.getUid();
            String camionEmail = currentUser.getEmail();
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Camion camion = new Camion(camionId, currentUser.getDisplayName(), camionEmail, null);
            TruckLocation truckLocation = new TruckLocation(new GeoPoint(location.getLatitude(), location.getLongitude()), System.currentTimeMillis(), camion);

            // Verificar si ya existe un documento para la fecha actual
            db.collection("truck_routes")
                    .document(currentDate)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Si el documento existe, actualizar la ruta del camión
                            addRoutePoint(currentDate, camionId, location);
                        } else {
                            // Si el documento no existe, crearlo con la ruta del camión
                            updateLocations(currentDate, truckLocation);
                        }
                    });
        }
    }

    private void addRoutePoint(String currentDate, String camionId, Location location) {
        db.collection("truck_routes")
                .document(currentDate)
                .update("camiones." + camionId + ".route", FieldValue.arrayUnion(new GeoPoint(location.getLatitude(), location.getLongitude())),
                        "camiones." + camionId + ".geo_point", new GeoPoint(location.getLatitude(), location.getLongitude()),
                        "camiones." + camionId + ".timestamp", System.currentTimeMillis());
    }

    private void updateLocations(String currentDate, TruckLocation truckLocation) {
        Map<String, Object> data = new HashMap<>();

        // Obtener la información actual de los camiones
        DocumentReference docRef = db.collection("truck_routes").document(currentDate);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            Map<String, Object> camionesMap = (Map<String, Object>) documentSnapshot.get("camiones");
            if (camionesMap == null) {
                camionesMap = new HashMap<>();
            }

            // Crear un mapa con geo_point, timestamp, email, truckId, y route
            Map<String, Object> truckData = new HashMap<>();
            truckData.put("geo_point", truckLocation.getLocation());
            truckData.put("timestamp", truckLocation.getTimestamp());
            truckData.put("email", truckLocation.getCamion().getEmail());
            truckData.put("truckId", truckLocation.getCamion().getTruckId());
            truckData.put("route", Collections.singletonList(truckLocation.getLocation()));

            // Agregar el nuevo camión al mapa de camiones
            camionesMap.put(truckLocation.getCamion().getTruckId(), truckData);

            // Actualizar el campo "camiones" en Firestore
            data.put("camiones", camionesMap);
            docRef.set(data, SetOptions.merge());
        });
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);


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
