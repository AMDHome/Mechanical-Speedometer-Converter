package com.ecs193.speedometerconverter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import java.util.ArrayList;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 */
public class BottomBarFragment extends Fragment{

    //private BluetoothActivity mBluetoothActivity;

    public static String EXTRA_ADDRESS = "device_address";

    Button btnPaired;
    ListView devicelist;

    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;

    public static final String ARG_TITLE = "arg_title";

    private TextView textView;

    //public BottomBarFragment() {
    //    // Required empty public constructor
    //}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_bottom_bar, container, false);


        //mBluetoothActivity.pairedDevicesList();

        // Setup any handles to view objects here
        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);
        /*btnPaired = rootView.findViewById(R.id.button);
        devicelist = rootView.findViewById(R.id.ListView);

        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth == null) {
            //Show a message that the device has no bluetooth adapter
            Toast.makeText(getActivity().getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

            //finish apk
            getActivity().finish();
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
        });*/
        //textView = (TextView) rootView.findViewById(R.id.fragment_bottom_bar_text_activetab);

        String title = getArguments().getString(ARG_TITLE, "");
        if (title == "Bluetooth") {
            Toast.makeText(getActivity().getApplicationContext(), "Bluetooth", Toast.LENGTH_LONG).show();
            //Intent intent = new Intent(getActivity(), BluetoothActivity.class);
            //startActivity(intent);
        }
        //textView.setText(title);

        return rootView;
    }



    /*@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        Intent intent = new Intent(getActivity(), BluetoothActivity.class);
        startActivity(intent);
    }*/
        //Button newBlockButton = (Button) getActivity().findViewById(
        //        R.id.new_block_button);
        //newBlockButton.setOnClickListener((OnClickListener) this);

        /*btnPaired = getActivity().findViewById(R.id.button);
        devicelist = getActivity().findViewById(R.id.ListView);

        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth == null) {
            //Show a message that the device has no bluetooth adapter
            Toast.makeText(getActivity().getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

            //finish apk
            getActivity().finish();
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
                Intent intent = new Intent(getActivity(), BluetoothActivity.class);
                startActivity(intent);
            }

        });

    }*/

    /*@Override
    public void onClick(View v) {
        Intent intent = new Intent(getActivity(), BluetoothActivity.class);
        startActivity(intent);
    }*/
    /*@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {


        // Setup any handles to view objects here
        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);
        btnPaired = view.findViewById(R.id.button);
        devicelist = view.findViewById(R.id.ListView);

        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth == null) {
            //Show a message that the device has no bluetooth adapter
            Toast.makeText(getActivity().getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

            //finish apk
            getActivity().finish();
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
    }*/

    public void pairedDevicesList() {
        Toast.makeText(getActivity().getApplicationContext(), "paired", Toast.LENGTH_LONG).show();
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
        } else {
            Toast.makeText(getActivity().getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        //final ArrayAdapter adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, list);
        //devicelist.setAdapter(adapter);
        //devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            // Make an intent to start next activity.
            Intent i = new Intent(getActivity(), ledControl.class);

            //Change the activity.
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
            startActivity(i);
        }
    };


}
