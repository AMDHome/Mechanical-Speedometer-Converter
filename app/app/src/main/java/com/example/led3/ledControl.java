package com.example.led3;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;



public class ledControl extends AppCompatActivity {



    Button btnOn, btnOff, btnDis;
    EditText txtField,txtField2,txtField3,txtField4,txtField5;
    TextView lumn;
    ToggleButton tog;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);

        //call the widgtes
        btnOn = (Button)findViewById(R.id.button2);
        btnOff = (Button)findViewById(R.id.button3);
        btnDis = (Button)findViewById(R.id.button4);
        txtField = (EditText)findViewById(R.id.editText);
        txtField2 = (EditText)findViewById(R.id.editText2);
        txtField3 = (EditText)findViewById(R.id.editText3);
        txtField4 = (EditText)findViewById(R.id.editText4);
        txtField5 = (EditText)findViewById(R.id.editText5);
        tog = (ToggleButton)findViewById(R.id.toggleButton);


        new ConnectBT().execute(); //Call the class to connect

        tog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { //TOGGLE
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    if (btSocket!=null) {
                        try
                        {
                            String str="U:";
                            str=str+"0"+'\0';

                            System.out.println(str);

                            btSocket.getOutputStream().write(str.toString().getBytes());
                        }
                        catch (IOException e)
                        {
                            msg("Error");
                        }
                    }

                } else {
                    // The toggle is disabled
                    if (btSocket!=null) {
                        try
                        {
                            String str="U:";
                            str=str+"1"+'\0';
                            System.out.println(str);

                            btSocket.getOutputStream().write(str.toString().getBytes());
                        }
                        catch (IOException e)
                        {
                            msg("Error");
                        }
                    }
                }
            }
        });

        txtField.setOnEditorActionListener(new TextView.OnEditorActionListener() { //TXTFIELD
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    if (btSocket!=null) {
                        try
                        {
                            //String str = getText();
                            String str = txtField.getText().toString();
                            str="M:" + str + '\0';
                            System.out.println(str);

                            btSocket.getOutputStream().write(str.toString().getBytes());
                        }
                        catch (IOException e)
                        {
                            msg("Error");
                        }
                    }

                    handled = true;
                }
                return handled;
            }
        });

        txtField2.setOnEditorActionListener(new TextView.OnEditorActionListener() { //TXTFIELD 2
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    if (btSocket!=null) {
                        try
                        {
                            //String str = getText();
                            String str = txtField2.getText().toString();
                            str="N:" + str + '\0';
                            System.out.println(str);

                            btSocket.getOutputStream().write(str.toString().getBytes());
                        }
                        catch (IOException e)
                        {
                            msg("Error");
                        }
                    }

                    handled = true;
                }
                return handled;
            }
        });

        txtField3.setOnEditorActionListener(new TextView.OnEditorActionListener() { //TXTFIELD 3
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    if (btSocket!=null) {
                        try
                        {
                            //String str = getText();
                            String str = txtField3.getText().toString();
                            int result = Integer.parseInt(str);
                            result=result*1000000;
                            str=new Integer(result).toString();
                            str="F:" + str + '\0';
                            System.out.println(str);
                            btSocket.getOutputStream().write(str.toString().getBytes());
                        }
                        catch (IOException e)
                        {
                            msg("Error");
                        }
                    }

                    handled = true;
                }
                return handled;
            }
        });

        txtField4.setOnEditorActionListener(new TextView.OnEditorActionListener() { //TXTFIELD 4
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    if (btSocket!=null) {
                        try
                        {
                            //String str = getText();
                            //String str = txtField.getText().toString();
                            String str = txtField4.getText().toString();
                            int result = Integer.parseInt(str);
                            result=result*1000000;
                            str=new Integer(result).toString();
                            str="S:" + str + '\0';
                            System.out.println(str);

                            btSocket.getOutputStream().write(str.toString().getBytes());

                        }
                        catch (IOException e)
                        {
                            msg("Error");
                        }
                    }

                    handled = true;
                }
                return handled;
            }
        });

        txtField5.setOnEditorActionListener(new TextView.OnEditorActionListener() { //TXTFIELD 5
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    if (btSocket!=null) {
                        try
                        {
                            //String str = getText();
                            String str = txtField5.getText().toString();
                            int result = Integer.parseInt(str);
                            result=result*100000000;
                            result=result / 63360;
                            str=new Integer(result).toString();
                            str="W:" + str + '\0';
                            System.out.println(str);

                            btSocket.getOutputStream().write(str.toString().getBytes());
                        }
                        catch (IOException e)
                        {
                            msg("Error");
                        }
                    }

                    handled = true;
                }
                return handled;
            }
        });

        btnOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                turnOnLed();      //method to turn on
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                turnOffLed();   //method to turn off
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });



    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    private void turnOffLed()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("ab".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void turnOnLed()
    {
        if (btSocket!=null)
        {
            try
            {
                InputStream a=null;
                btSocket.getOutputStream().write("m1 on".toString().getBytes());

                a = btSocket.getInputStream();
                if (a!=null) {
                    msg("InputStream a is not null");
                    int str=a.read();
                    System.out.println(str);

                }
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }


    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
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
            }
            progress.dismiss();
        }
    }

}
