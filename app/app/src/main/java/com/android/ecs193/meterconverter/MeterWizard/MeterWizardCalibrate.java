package com.android.ecs193.meterconverter.MeterWizard;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.ecs193.meterconverter.HomeFragment;
import com.android.ecs193.meterconverter.R;

public class MeterWizardCalibrate extends AppCompatActivity {

    // Radius of the Earth in meters, used in calcHaversineDist
    public static final double EARTH_RADIUS = 6371071.0;
    // Creates a reference to the system Location Manager
    LocationManager locationManager;
    // Creates a listener that responds to location updates
    LocationListener locationListener;
    // To hold previous location
    public static Location oldLocation = null;
    // Constant for fine location, ACCESS_FINE_LOCATION is for GPS_Provider
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    TextView text_target;
    TextView text_current;
    Button but_cancel;
    Button but_finish;

    HomeFragment mHomeFragment = new HomeFragment();
    MeterWizardRPM mMeterWizardRPM = new MeterWizardRPM();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_meter_calibrate);

        text_target = findViewById(R.id.text_target_speed);
        text_target.setText(String.valueOf(mMeterWizardRPM.getTargetSpeed()));



        // GPS for current speed
        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new speedCalc();

        // This part should "register" the listener with the Location Manager to receive updates after checking permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // minTime: 0 and minDistance: 0 indicates that the provider should make updates as fast as possible. This seems to be about once per second.
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }

        but_finish = findViewById(R.id.but_finish);
        but_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    finish();
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent wizIntent = new Intent(MeterWizardCalibrate.this, MeterWizardTireSize.class);
                finish();
                // Slide from right to left
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                startActivity(wizIntent);


            }
        });

    }

    public void useNewLocation(Location location) {
        // If on first call, set oldLocation and return
        if (oldLocation == null) {
            oldLocation = location;
            return;
        }
        double newLat = location.getLatitude(); // In degrees
        double newLon = location.getLongitude(); // In degrees
        long currTime = location.getTime(); // Milliseconds since 1/1/70
        double distance = calcHaversineDist(oldLocation.getLatitude(), oldLocation.getLongitude(), newLat, newLon);
        int mph = (int) ((distance/1609.0)/(((double)(currTime - oldLocation.getTime())) / 3600000.0)); // Convert meters to miles and ms to hours
        text_current = findViewById(R.id.text_curr_speed);
        text_current.setText(mph);
        oldLocation = location;
    }

    // Haversine Formula for distance b/t pts on sphere given lat. & lon.; see https://en.wikipedia.org/wiki/Haversine_formula
    public static double calcHaversineDist(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2.0)
                * Math.sin(dLon / 2.0);
        double c = 2 * Math.asin(Math.sqrt(a));
        double distInMeters = EARTH_RADIUS * c;
        return distInMeters;
    }

    // Defines a listener that responds to location updates
    class speedCalc implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {

            // Called when a new location is found by the GPS location provider.
            if (location != null) {
                useNewLocation(location);
            }
        }
        // Following functions not used but required in class which implements LocationListener
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    }
}


