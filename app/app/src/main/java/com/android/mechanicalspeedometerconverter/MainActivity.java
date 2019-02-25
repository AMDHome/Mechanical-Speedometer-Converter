package com.android.mechanicalspeedometerconverter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.Set;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import java.util.ArrayList;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ListView;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletionService;

import android.os.Handler;
import android.widget.ProgressBar;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.CoordinatorLayout;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private Button btButton;
    private Button increButton;
    private Button decreButton;
    private TextView mValue;
    private ProgressBar toolbarProgressCircle;
    private Toolbar mToolbar;
    private ListView btList;
    int count = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private BluetoothDevicesAdapter btDevicesAdapter;
    private StringBuilder recDataString = new StringBuilder();
    private CoordinatorLayout mCoordinatorLayout;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice> pairedDevices;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_calibrate);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_liveData);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_settings);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = findViewById(R.id.message);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // bluetooth button
        btButton = findViewById(R.id.btButton);
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //open(view);
                if(mBluetoothAdapter.isEnabled())
                {
                    bluetoothSearch();
                } else {
                    bluetoothEnable();
                }
            }
        });

        // bluetooth device list
        btList = findViewById(R.id.btList);
        btList.setOnClickListener(new View.OnClickListener(int position) {
            @Override
            public void onClick(View view) {
                mToolbar.setSubtitle("Asking to connect");
                final BluetoothDevice device = bluetoothDevicesAdapter.getItem(position);

                new AlertDialog.Builder(MainActivity.this)
                        .setCancelable(false)
                        .setTitle("Connect")
                        .setMessage("Do you want to connect to: " + device.getName() + " - " +
                                device.getAddress())
                        .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Log.d(Constants.TAG, "Opening new Activity");
                                mBluetoothAdapter.cancelDiscovery();
                                toolbarProgressCircle.setVisibility(View.INVISIBLE);

                                Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);

                                intent.putExtra(Constants.EXTRA_DEVICE, device);

                                startActivity(intent);
                            }
                        })

                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mToolbar.setSubtitle("Cancelled connection");
                                Log.d(Constants.TAG, "Cancelled");
                            }
                        }).show();

            }
        })

        mValue = findViewById(R.id.value);

        increButton = findViewById(R.id.increButton);
        increButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count++;
                mValue.setText(Integer.toString(count));
                //mConnectedThread.write(count);
            }
        });

        decreButton = findViewById(R.id.decreButton);
        decreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count--;
                mValue.setText(Integer.toString(count));
                //mConnectedThread.write(count);
            }
        });
        //Intent homeIntent= new Intent(MainActivity.this, CalibrateActivity.class);
        //startActivity(homeIntent);
        //finish();

    }



    private void bluetoothSearch() {
        if (mBluetoothAdapter.startDiscovery()) {
            toolbarProgressCircle.setVisibility(View.VISIBLE);
            mToolbar.setSubtitle("Searching for bluetooth devices");
        } else {
            mToolbar.setSubtitle("Error");
            Snackbar.make(mCoordinatorLayout, "Failed to start searching",
                    Snackbar.LENGTH_INDEFINITE).setAction("Try Again",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            bluetoothSearch();
                        }
                    }).show();
        }
    }

    private void bluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);

        Toast.makeText(MainActivity.this, "Bluetooth already enabled",
                Toast.LENGTH_LONG).show();
    }

    BluetoothDevice ArduinoDevice;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                bluetoothSearch();
                /*Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_LONG).show();
                pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size()>0) {
                    for (BluetoothDevice device: pairedDevices) {
                        if (device.getName().equals("DSD TECH HC-05")) {
                            ArduinoDevice = device;
                            Toast.makeText(this, "Bluetooth connected", Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                }*/
                //open(View);
                //pairedDevice(); //method that will be called
            } else {
                mToolbar.setSubtitle("Error");
                Snackbar.make(mCoordinatorLayout, "Failed to enable bluetooth",
                        Snackbar.LENGTH_INDEFINITE).setAction("Try Again",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                bluetoothEnable();
                            }
                        }).show();
                //Toast.makeText(this, "User canceled", Toast.LENGTH_LONG).show();
            }
        }
    }

    /*Button bluetooth = (Button) findViewById(R.id.button);
    private void pairedDevice()
    {
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size()>0)
        {
            bluetooth.setText(pairedDevices.toString());
        }
        else
        {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        //final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        //list.setAdapter(adapter);
        //list.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }*/

    /*public void showBTDialog() {

        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.activity_main, (ViewGroup) findViewById(R.id.bt_list));
        popDialog.setTitle("Paired Bluetooth Devices");
        popDialog.setView(Viewlayout);

        // create the arrayAdapter that contains the BTDevices, and set it to a ListView
        myListView = (ListView) findViewById(R.id.BTList);
        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        myListView.setAdapter(BTArrayAdapter);

        // get paired devices
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        BluetoothDevice btDev = null;
        // put it's one to the adapter
        for(Object device : pairedDevices)
            btDev = (BluetoothDevice) device;
            BTArrayAdapter.add(btDev.getName()+ "\n" + btDev.getAddress());

        // Button OK
        popDialog.setPositiveButton("Pair",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                });

        // Create popup and show
        popDialog.create();
        popDialog.show();

    }*/
    /*private void pairedDevicesList()
    {
        BluetoothDevice bt;
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size()>0)
        {
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView av, View v, int arg2, long arg3)
        {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            // Make an intent to start next activity.
            Intent i = new Intent(DeviceList.this, ledControl.class);
            //Change the activity.
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
            startActivity(i);
        }
    };*/

    /*public void open(View view){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Allow this application to use bluetooth?");
                alertDialogBuilder.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                Toast.makeText(MainActivity.this,
                                        REQUEST_ENABLE_BT,Toast.LENGTH_LONG).show();
                            }
                        });

        alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }*/



}
