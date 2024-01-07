package com.example.googleapitest;

import android.Manifest.permission;
import android.annotation.SuppressLint;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.PolyUtil;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private boolean permissionDenied = false;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    GoogleMap Map;
    private LatLng originLatLng;
    private LatLng destinationLatLng;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Map = googleMap;

        enableMyLocation();
        getLocation();

        Map.setOnMyLocationButtonClickListener(this);
        Map.setOnMyLocationClickListener(this);

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                googleMap.clear();

                // Handle the click event and get the coordinates
                double latitude = latLng.latitude;
                double longitude = latLng.longitude;

                destinationLatLng = new LatLng(latitude, longitude);

                // Log or display the coordinates as needed
                Log.d("MapClick", "Latitude: " + latitude + ", Longitude: " + longitude);

                // Add a marker at the clicked position
                googleMap.addMarker(new MarkerOptions().position(latLng).title("Latitude: " + latitude + ", Longitude: " + longitude));
            }
        });

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getLocation();
                Log.d("BUTTONS", "User tapped the button");
                getDirections(originLatLng,destinationLatLng, "walking");
            }
        });

        Map.setOnMyLocationButtonClickListener(this);
        Map.setOnMyLocationClickListener(this);


    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {

        // Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Map.setMyLocationEnabled(true);
            return;
        }

        // request location permissions from the user.
        PermissionUtils.requestLocationPermissions(this, LOCATION_PERMISSION_REQUEST_CODE, true);

    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION) || PermissionUtils
                .isPermissionGranted(permissions, grantResults,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message

            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true;

        }
    }


    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            permissionDenied = false;
        }
    }


    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    private void getLocation() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            // Use the latitude and longitude as needed
                            Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);

                            // Add a marker at the user's current location
                            originLatLng = new LatLng(latitude, longitude);


                            // Move the camera to the user's location
                            Map.moveCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 15f));

                        } else {
                            // Handle the case where the last known location is not available
                            Log.d("Location", "Last known location is not available");
                        }
                    }
                });
    }

    private void getDirections(LatLng origin, LatLng destination, String mode) {
        String apiKey = "AIzaSyAOMK7IzBZSX0Q6t7rs7J2sMgQ6ocxFOH8";
        String originString = origin.latitude + "," + origin.longitude;
        String destinationString = destination.latitude + "," + destination.longitude;

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/directions/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Create API service
        DirectionsApiService apiService = retrofit.create(DirectionsApiService.class);

        // Make API request
        Call<DirectionsResponse> call = apiService.getDirections(originString, destinationString, mode, apiKey);
        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Parse the response and draw the route on the map
                    drawDirections(response.body());
                } else {
                    // Handle API error
                    Log.e("DirectionsAPI", "Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                // Handle network or other errors
                Log.e("DirectionsAPI", "Error: " + t.getMessage());
            }
        });
    }

    private void drawDirections(DirectionsResponse directionsResponse) {
        // Parse the directions response and draw the route on the map
        String polyline = directionsResponse.routes.get(0).overviewPolyline.points;

        // Decode the polyline and draw it on the map
        List<LatLng> decodedPolyline = PolyUtil.decode(polyline);

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(decodedPolyline)
                .color(Color.BLUE)  // Set the color to blue
                .width(10);  // Set the width of the polyline

        Map.addPolyline(polylineOptions);
    }

}