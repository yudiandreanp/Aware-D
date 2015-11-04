package com.yudiandreanp.aware_d;

/**
 * Created by yudiandrean on 11/3/2015.
 */

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class GPSManager implements LocationListener {

    private LocationManager locationManager;
    private double latitude;
    private double longitude;
    private Criteria criteria;
    private String provider;

    // fire the updateWithNewLocation method whenever a location change is detected
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status,
                                    Bundle extras) {}
    };

    public GPSManager(Context context) {
        locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        provider = locationManager.getBestProvider(criteria, true);



        try {
            Location location = locationManager.getLastKnownLocation(provider);
            updateWithNewLocation(location);
        }
        catch (SecurityException e)
        {Log.e("PERMISSION_EXCEPTION", "PERMISSION_NOT_GRANTED");}

        try {
            // updates restricted to every 2 seconds and only when movement
            // of more than 10 metres has been detected
            locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
        } catch (SecurityException e) {
            Log.e("PERMISSION_EXCEPTION", "PERMISSION_NOT_GRANTED");
        }

    }

    private void updateWithNewLocation(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }
}
