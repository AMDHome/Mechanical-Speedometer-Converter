/* 
*  This program uses the android location API to determine the GPS-based speed of the mobile device it runs on. Specifically, 
*  it makes time, latitude, and longitude requests from android.location. The latter two are used in the haversine formula
*  to calculate distance between two locations. In turn, this distance is divided by the time difference of the locations to 
*  derive speed. While the API also features a getSpeed() function, the consensus seems to be that this function is fairly 
*  unreliable: if it cannot determine the speed, it simply returns 0, a value that must be ignored to avoid bad output. 
*  Requesting time, latitude, and longitude and then calculating the speed manually seems to be the better choice.  
*/ 

package com.example.speedfromgps;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    // Radius of the Earth in meters, used in calcHaversineDist
    public static final int EARTH_RADIUS = 6371071;
    // Creates a reference to the system Location Manager
    LocationManager locationManager;
    // Creates a listener that responds to location updates
    LocationListener locationListener;
    // To hold previous location
    public static Location oldLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new speedCalc();
        
        // This line should "register" the listener with the Location Manager to receive updates according to 
        // the online documentation, but is giving me some kind of permission problem. 
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
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
        long distance = calcHaversineDist(oldLocation.getLatitude(), oldLocation.getLongitude(), newLat, newLon);
        int mph = (int)((distance/1609)/((currTime - oldLocation.getTime())/(3600000))); // Convert meters to miles and ms to hours
        TextView textView = findViewById(R.id.txtCurrentSpeed);
        textView.setText(mph + " miles/hour");
        oldLocation = location;
    }
    // Haversine Formula for distance b/t pts on sphere given lat. & lon.; see https://en.wikipedia.org/wiki/Haversine_formula
    public static long calcHaversineDist(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        long distInMeters = Math.round(EARTH_RADIUS * c);
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
        // Following functions not used
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    }
}
