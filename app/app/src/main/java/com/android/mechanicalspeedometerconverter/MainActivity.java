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
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
//import java.util.Set;
import android.bluetooth.BluetoothDevice;
import android.widget.AdapterView;
import android.widget.ListView;
import android.util.Log;
import android.widget.ProgressBar;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.CoordinatorLayout;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevicesAdapter btDevicesAdapter;

    private Toolbar mToolbar;
    private ListView btList;
    private TextView emptyListView;
    private ProgressBar toolbarProgressCircle;
    private CoordinatorLayout mCoordinatorLayout;

    private TextView mTextMessage;
    private Button btButton;
    //private Button increButton;
    //private Button decreButton;
    //private TextView mValue;



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

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mToolbar = findViewById(R.id.toolbar);
        btList = findViewById(R.id.devices_list_view);
        emptyListView = findViewById(R.id.empty_list_item);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout_main);
        mTextMessage = findViewById(R.id.message);
        btButton = findViewById(R.id.btButton);
        toolbarProgressCircle = findViewById(R.id.toolbar_progress_bar);
        //mValue = findViewById(R.id.value);

        setSupportActionBar(mToolbar);
        mToolbar.setSubtitle("None");

        btDevicesAdapter = new BluetoothDevicesAdapter(this);

        btList.setAdapter(btDevicesAdapter);
        btList.setEmptyView(emptyListView);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Log.e(Constants.TAG, "Device has no bluetooth");
            new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle("No Bluetooth")
                    .setMessage("Your device has no bluetooth")
                    .setPositiveButton("Close app", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            Log.d(Constants.TAG, "App closed");
                            finish();
                        }
                    }).show();
        }


        // bluetooth button
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
        btList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //btList.setOnClickListener(new View.OnClickListener() {
            //@Override
            //public void onClick(View view) {
                mToolbar.setSubtitle("Asking to connect");
                final BluetoothDevice device = btDevicesAdapter.getItem(position);

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
        });



        /*increButton = findViewById(R.id.increButton);
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
        });*/
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
        mToolbar.setSubtitle("Enabling Bluetooth");
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
    }

    @Override protected void onStart() {
        super.onStart();

        Log.d(Constants.TAG, "Registering receiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override protected void onStop() {
        super.onStop();
        Log.d(Constants.TAG, "Receiver unregistered");
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                bluetoothSearch();
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

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                device.fetchUuidsWithSdp();

                if (btDevicesAdapter.getPosition(device) == -1) {
                    // -1 is returned when the item is not in the adapter
                    btDevicesAdapter.add(device);
                    btDevicesAdapter.notifyDataSetChanged();
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                toolbarProgressCircle.setVisibility(View.INVISIBLE);
                mToolbar.setSubtitle("None");

            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Snackbar.make(mCoordinatorLayout, "Bluetooth turned off", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Turn on", new View.OnClickListener() {
                                    @Override public void onClick(View v) {
                                        bluetoothEnable();
                                    }
                                }).show();
                        break;
                }
            }
        }
    };

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
        final View Viewlayout = inflater.inflate(R.layout.oldactivity_main, (ViewGroup) findViewById(R.id.bt_list));
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
