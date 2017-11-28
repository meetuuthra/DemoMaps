package com.example.meetuuthra.maps;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    LocationManager mLocManager;

    private GoogleMap mMap;
    List<LatLng> Path;
    LocationListener mLocListener;
    Boolean startTracking, trackingInProgress;

    Location currentLocation, startLocation;

    PolylineOptions pathLine;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        currentLocation = null;
        startTracking = false;
        trackingInProgress = true;
        mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Path = new ArrayList<>();
        pathLine = new PolylineOptions();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enable GPS")
                    .setMessage("Would you like to enable GPS?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            mLocListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    currentLocation = location;
                    Path.add(new LatLng(location.getLatitude(), location.getLongitude()));

                    if (trackingInProgress) {
                        startLocation = location;
                        LatLng currLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.clear();
                        pathLine = new PolylineOptions();
                        Path = new ArrayList<>();


                        mMap.addMarker(new MarkerOptions().position(currLocation).title(getAddress(location.getLatitude(), location.getLongitude()))).setSnippet("Start Location");
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currLocation, 15));
                        trackingInProgress = false;
                    }
                    pathLine.add(new LatLng(location.getLatitude(), location.getLongitude()));
                    mMap.addPolyline(pathLine);
                    setBoundaries();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }

            };
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                if (!startTracking) {

                    Toast.makeText(MapsActivity.this, "Started Tracking", Toast.LENGTH_LONG).show();

                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, mLocListener);
                    startTracking=true;
                }
                else{
                    startTracking=false;
                    trackingInProgress=true;
                    mLocManager.removeUpdates(mLocListener);
                    Toast.makeText(MapsActivity.this, "Stopped Tracking", Toast.LENGTH_LONG).show();

                    if(currentLocation!=null) {
                        LatLng loc = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(loc).title(getAddress(loc.latitude, loc.longitude))).setSnippet("End Location");
                        setBoundaries();

                    }
                }


            }
        });
    }
    public String getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);
            String add = obj.getAddressLine(0);
            add = add + "\n" + obj.getCountryName();
            add = add + "\n" + obj.getCountryCode();
            add = add + "\n" + obj.getAdminArea();
            add = add + "\n" + obj.getPostalCode();
            add = add + "\n" + obj.getSubAdminArea();
            add = add + "\n" + obj.getLocality();
            add = add + "\n" + obj.getSubThoroughfare();

            return add;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private void setBoundaries(){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(new LatLng(startLocation.getLatitude(), startLocation.getLongitude()));
        builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));

        int zoomWidth = getResources().getDisplayMetrics().widthPixels;
        int zoomHeight = getResources().getDisplayMetrics().heightPixels;
        int zoomPadding = (int) (zoomWidth * 0.15);
        LatLngBounds bounds = builder.build();
        LatLngBounds currentBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        if(!currentBounds.contains(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())))
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,zoomWidth,zoomHeight,zoomPadding));

    }
}