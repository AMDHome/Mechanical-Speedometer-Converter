package com.android.ecs193.meterconverter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BtConnection extends AppCompatActivity {
    private Set<BluetoothDevice> pairedDevices;
    Intent rtnIntent;
    TextView tv;
    ListView devicelist;
    Button findButton;
    Button cancelButton;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    static BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static String EXTRA_ADDRESS;
    public static final String BT_INTENT_FLAG = "BT_INTENT_FLAG";
    static String btName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rtnIntent = new Intent();
        setContentView(R.layout.activity_bt_connection);

        devicelist = findViewById(R.id.listBluetooth);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        findButton = findViewById(R.id.but_find);
        cancelButton = findViewById(R.id.but_cancel);

        checkBluetoothConnectivity();

        findButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                checkBluetoothConnectivity();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
            }
        });
    }

    void checkBluetoothConnectivity() {

        if (myBluetooth == null) {
            //Show a message that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

            //finish apk
            finish();
        } else if (!myBluetooth.isEnabled()) {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }

        pairedDevicesList();

    }

    public void pairedDevicesList() {

        // Register for broadcasts when a device is discovered.
        //IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //registerReceiver(mReceiver, filter);


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
            btName = info.substring(0, 15);
            address = info.substring(info.length() - 17);


            // Initialize a TextView for ListView each Item
            tv = v.findViewById(android.R.id.text1);

            // Set the text color of TextView (ListView Item)
            tv.setTextColor(Color.WHITE);

            new ConnectBT().execute(); //Call the class to connect
        }
    };

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(BtConnection.this, "Connecting...",
                    "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            myBluetooth = null;
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
                // change opacity of calibration layout
                //findViewById(R.id.meterSettings).setAlpha((float)1);
            }
            progress.dismiss();

            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    static public BluetoothSocket getBtConnection()
    {
        return btSocket;
    }

    static public String getBtName() { return btName; }
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }
}
