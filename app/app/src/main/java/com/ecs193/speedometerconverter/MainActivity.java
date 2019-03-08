package com.ecs193.speedometerconverter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.os.Handler;
import android.widget.TextView;


//import com.ecs193.speedometerconverter.BluetoothActivity;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public TextView mTextMessage;
    public static String EXTRA_ADDRESS = "device_address";

    Button btnPaired;
    ListView devicelist;

    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;


    //public void getBluetoothActivity() {}

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            // here we set the listener
            switch (item.getItemId()) {
                case R.id.bottombar_bluetooth:
                    //Intent a = new Intent(MainActivity.this,BluetoothActivity.class);
                    //startActivity(a);
                    //findViewById(R.id.bluetoothLayout).setVisibility(View.VISIBLE);

                    //BluetoothActivity cls2 = new BluetoothActivity();
                    //cls2.getBluetoothActivity();

                    BluetoothActivity();
                    return true;
                case R.id.bottombar_settings:
                    findViewById(R.id.textView).setVisibility(View.GONE);
                    findViewById(R.id.button).setVisibility(View.GONE);
                    findViewById(R.id.ListView).setVisibility(View.GONE);
                    //findViewById(R.id.bluetoothLayout).setVisibility(ConstraintLayout.GONE);
                    return true;
                case R.id.bottombar_data:
                    findViewById(R.id.textView).setVisibility(View.GONE);
                    findViewById(R.id.button).setVisibility(View.GONE);
                    findViewById(R.id.ListView).setVisibility(View.GONE);
                    //findViewById(R.id.bluetoothLayout).setVisibility(ConstraintLayout.GONE);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothActivity();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        //Integer btID = getResources().getIdentifier("@string/menu_bluetooth","layout", getPackageName());
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    }

    void BluetoothActivity() {
        findViewById(R.id.textView).setVisibility(View.VISIBLE);
        findViewById(R.id.button).setVisibility(View.VISIBLE);
        findViewById(R.id.ListView).setVisibility(View.VISIBLE);
        
        btnPaired = findViewById(R.id.button);
        devicelist = findViewById(R.id.ListView);

        myBluetooth = BluetoothAdapter.getDefaultAdapter();


        if (myBluetooth == null) {
            //Show a message that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

            //finish apk
            finish();
        } else if (!myBluetooth.isEnabled()) {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        } else {
            pairedDevicesList();
        }

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pairedDevicesList();
            }
        });
    }

    public void pairedDevicesList() {
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            // Make an intent to start next activity.
            Intent i = new Intent(MainActivity.this, ledControl.class);

            //Change the activity.
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
            startActivity(i);
        }
    };

}

