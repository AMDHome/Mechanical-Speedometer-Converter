package com.android.ecs193.meterconverter;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import com.android.ecs193.meterconverter.MeterWizard.MeterWizardRatio;
import com.android.ecs193.meterconverter.MeterWizard.MeterWizardTireSize;
import com.android.ecs193.meterconverter.MeterWizard.MeterWizardUnit;
import com.android.ecs193.meterconverter.MeterWizard.MeterWizardDriveCheck;

public class HomeFragment extends Fragment {

    Button butWizardDrive;
    Button butWizardRatio;
    Button pairDevice;
    //TextView butUnits;
    Button butUnits;
    Button butMaxSpeed;
    Button butMagnets;
    Button butFinalDrive;
    Button butRatio;
    Button butSize;

    static TextView unitsText;
    static TextView maxSpeedText;
    static TextView magnetsText;
    static TextView finalDriveText;
    static TextView meterRatioText;
    static TextView tireSizeText;

    TextView tv;
    ListView mListView;
    static BluetoothSocket btSocket = null;

    static AlertDialog alertDialog1;

    String wheelUnit;
    static boolean driveCheck = false;
    final static int BT_INTENT_FLAG = 0;

    private RecyclerView recyclerView;

    static MeterWizardRatio mMeterWizardRatio = new MeterWizardRatio();

    static Context thisContext;

    static EditText tireSize1, tireSize2, tireSize3, tireSize4, tireSize5, tireSize6, tireSize7;


    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.frag_settings, container, false);
        thisContext = view.getContext();

        view.findViewById(R.id.myCarSettings).setAlpha((float)0.3);
        view.findViewById(R.id.meterSettings).setAlpha((float)0.3);
        view.findViewById(R.id.sensorSettings).setAlpha((float)0.3);

        pairDevice = view.findViewById(R.id.deviceArrow);
        pairDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent btIntent = new Intent(getActivity(), com.android.ecs193.meterconverter.BtConnection.class);
                startActivityForResult(btIntent, BT_INTENT_FLAG);
            }
        });

        butUnits = view.findViewById(R.id.but_units);
        unitsText = view.findViewById(R.id.unitsText);
        butUnits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getButtonDialog("Choose Units", R.array.units_array, unitsText);
            }
        });



        butMagnets= view.findViewById(R.id.but_magnets);
        magnetsText = view.findViewById(R.id.magnetsText);
        butMagnets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getButtonDialog("Choose Number of Magnets", R.array.magnet_array, magnetsText);
            }
        });

        butFinalDrive= view.findViewById(R.id.but_finalDrive);
        finalDriveText = view.findViewById(R.id.finalDriveText);
        butFinalDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextBoxDialog("Enter Final Drive", finalDriveText, "F:");
            }
        });

        butWizardDrive = view.findViewById(R.id.find_drive);
        butWizardDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            if (btSocket != null) {
                try {
                    String str = "P:1" + '\0';
                    // Write to Arduino to tell it to enter speedometer ratio calibration mode
                    btSocket.getOutputStream().write(str.getBytes());
                    msg(str);
                    Intent wizIntent = new Intent(getActivity(), MeterWizardDriveCheck.class);
                    startActivity(wizIntent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            }
        });

        butMaxSpeed= view.findViewById(R.id.but_maxSpeed);
        maxSpeedText = view.findViewById(R.id.maxSpeedText);
        butMaxSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextBoxDialog("Enter Max Speed", maxSpeedText, "M:");
            }
        });

        butRatio= view.findViewById(R.id.but_ratio);
        meterRatioText = view.findViewById(R.id.meterRatioText);
        butRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextBoxDialog("Enter Speedometer Ratio", meterRatioText, "S:");
            }
        });

        butWizardRatio = view.findViewById(R.id.find_ratio);
        butWizardRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (btSocket != null) {
                    try {
                        String str = "P:1" + '\0';
                        // Write to Arduino to tell it to enter speedometer ratio calibration mode
                        btSocket.getOutputStream().write(str.getBytes());
                        msg(str);
                        Intent wizIntent = new Intent(getActivity(), MeterWizardUnit.class);
                        startActivity(wizIntent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        butSize = view.findViewById(R.id.but_size);
        tireSizeText = view.findViewById(R.id.tireSizeText);
        butSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getTextBoxDialog("Enter tire size", tireSizeText, "W:");
                getTireSizeDialog();
            }
        });
        return view;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to
        if (requestCode == BT_INTENT_FLAG) {

            // Make sure the request was successful
            if (resultCode == getActivity().RESULT_OK) {

                btSocket = BtConnection.getBtConnection();

                getView().findViewById(R.id.myCarSettings).setAlpha((float)1);
                getView().findViewById(R.id.meterSettings).setAlpha((float)1);
                getView().findViewById(R.id.sensorSettings).setAlpha((float)1);

                TextView deviceText = getView().findViewById(R.id.device_text);
                deviceText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                deviceText.setText(BtConnection.getBtName());
                getMeterSettings();
            }
        }
    }

    void getButtonDialog(final String title, int arrID, final TextView textBox) {

        final AlertDialog closeDialog;

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setIcon(R.drawable.ic_baseline_create_24px);
        builder.setSingleChoiceItems(getResources().getStringArray(arrID),
                -1, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int item) {

                boolean dismiss = false;

                if (title == "Choose Units") {
                    switch(item) {
                        case 0:
                            textBox.setText("mph");
                            break;
                        case 1:
                            textBox.setText("kph");
                            break;
                    }

                    if (item == 0) {
                        sendUnits(Integer.toString(0));
                        dismiss = true;
                    } else if (item == 1) {
                        sendUnits(Integer.toString(1));
                        dismiss = true;
                    }

                } else if (title == "Choose Number of Magnets") {
                    switch(item) {
                        case 0:
                            textBox.setText("1");
                            break;
                        case 1:
                            textBox.setText("2");
                            break;
                        case 2:
                            textBox.setText("4");
                            break;
                    }

                    if (item == 0) {
                        sendInfo(Integer.toString(1), "N:");
                        dismiss = true;
                    } else if (item == 1) {
                        sendInfo(Integer.toString(2), "N:");
                        dismiss = true;
                    } else if (item == 2) {
                        sendInfo(Integer.toString(4), "N:");
                        dismiss = true;
                    }
                }

                if (dismiss) {
                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        public void run() {
                            dialog.dismiss();
                            timer.cancel();
                        }
                    }, 500);
                }
            }
        });
        builder.setCancelable(true);
        builder.setNegativeButton("Cancel", null);
        builder.show();

        /*if (title == "Choose Units") {

            if (textBox.getText().toString().equalsIgnoreCase("mph")) {
                sendUnits(Integer.toString(0));
                closeDialog.dismiss();
            } else if (textBox.getText().toString().equalsIgnoreCase("kph")) {
                sendUnits(Integer.toString(1));
                closeDialog.dismiss();
            }

        } else if (title == "Choose Number of Magnets") {

            if (textBox.getText().toString().equalsIgnoreCase("1")) {
                sendInfo(Integer.toString(1), "N:");
                closeDialog.dismiss();
            } else if (textBox.getText().toString().equalsIgnoreCase("2")) {
                sendInfo(Integer.toString(2), "N:");
                closeDialog.dismiss();
            } else if (textBox.getText().toString().equalsIgnoreCase("4")) {
                sendInfo(Integer.toString(4), "N:");
                closeDialog.dismiss();
            }

        }*/

    }

    static void getTireSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(thisContext);

        // get the layout inflater
        LayoutInflater inflater = (LayoutInflater)thisContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // inflate and set the layout for the dialog
        // pass null as the parent view because its going in the dialog layout
        View viewInflate = inflater.inflate(R.layout.dialog_tire_size, null);
        builder.setView(viewInflate);

        tireSize1 = viewInflate.findViewById(R.id.text_tire1);
        tireSize2 = viewInflate.findViewById(R.id.text_tire2);
        tireSize3 = viewInflate.findViewById(R.id.text_tire3);
        tireSize4 = viewInflate.findViewById(R.id.text_tire4);
        tireSize5 = viewInflate.findViewById(R.id.text_tire5);
        tireSize6 = viewInflate.findViewById(R.id.text_tire6);
        tireSize7 = viewInflate.findViewById(R.id.text_tire7);

        tireSize1.requestFocus();
        tireSize1.setCursorVisible(true);
        nextText(tireSize1, tireSize2);
        nextText(tireSize2, tireSize3);
        nextText(tireSize3, tireSize4);
        nextText(tireSize4, tireSize5);
        nextText(tireSize5, tireSize6);
        nextText(tireSize6, tireSize7);
        nextText(tireSize7, tireSize7);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // remove the dialog from the screen
            }
        });

        final AlertDialog alert = builder.create();
        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alert.show();

        tireSize7.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {
                    alert.dismiss();
                    String input = 'P' + tireSize1.getText().toString()
                            + tireSize2.getText().toString() + tireSize3.getText().toString()
                            + '/' + tireSize4.getText().toString() + tireSize5.getText().toString()
                            + 'R' + tireSize6.getText().toString() + tireSize7.getText().toString();
                    if (Pattern.matches("P[0-9][0-9][0-9]\\/[0-9][0-9]R[0-9][0-9]", input)) {
                        tireSizeText.setText(input);
                        sendTireSizeCalc(input);
                    } else {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(thisContext);
                        builder.setTitle("Error");
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(
                                "Please enter only numbers");
                        builder.setCancelable(true);

                        final AlertDialog closeDialog = builder.create();
                        closeDialog.show();

                        // Display dialog box for 2 seconds
                        final Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            public void run() {
                                closeDialog.dismiss();
                                timer.cancel();
                            }
                        }, 2000);
                        // show prompt again after 2 seconds
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                alert.show();
                            }
                        }, 2000);
                    }
                }
                return false;
            }
        });
    }

    static protected void nextText(final EditText text1, final EditText text2) {

        final StringBuilder sb = new StringBuilder();

        text1.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (sb.length() == 0 & text1.length() == 1) {
                    sb.append(s);
                    text1.clearFocus();
                    text2.requestFocus();
                    text2.setCursorVisible(true);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (sb.length() == 1) {
                    sb.deleteCharAt(0);
                }
            }

            public void afterTextChanged(Editable s) {
                if (sb.length() == 0) {
                    text1.requestFocus();
                }

            }
        });
    }

    static void getTextBoxDialog(final String title, final TextView textBox, final String arduinoStr) {

        // Set text box input as int only
        final EditText input = new EditText(thisContext);
        if (arduinoStr == "M:") { // magnet
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else if ((arduinoStr == "F:") || (arduinoStr == "S:")) {
            input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(thisContext)
                .setTitle(title)
                .setIcon(R.drawable.ic_baseline_create_24px)
                .setView(input)
                .setNegativeButton("Cancel", null);

        final AlertDialog alert = builder.create();
        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alert.show();

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {
                    alert.dismiss();
                    if (input.getText().toString().length() != 0) {
                        if (arduinoStr == "M:") {
                            if (testInt(input)) {
                                textBox.setText(input.getText().toString());
                                sendInfo(input.getText().toString(), arduinoStr);
                                mMeterWizardRatio.setspeedometerHalf(Integer.parseInt(input.getText().toString()));
                            } else {
                                alert.show();
                            }
                        } else if ((arduinoStr == "F:") || (arduinoStr == "S:")) {
                            if (testInt(input)) {
                                textBox.setText(input.getText().toString());
                                sendCalc(input.getText().toString(), arduinoStr);
                            } else {

                                // show prompt again after 2 seconds
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    public void run() {
                                        alert.show();
                                    }
                                }, 2000);

                            }
                        }
                    }
                }
                return false;
            }
        });
    }

    void getMeterSettings() {

        if (btSocket != null) {
            try {

                String rtnStr = null;
                String[] splitArr;

                btSocket.getOutputStream().write("L:1\0".getBytes());

                for (int rtnBuff = 0; ((rtnBuff = btSocket.getInputStream().read()) >= 0) && rtnBuff != 13;) {
                    if (rtnStr == null) {
                        rtnStr = Character.toString((char) rtnBuff);
                    } else {
                        rtnStr += Character.toString((char) rtnBuff);
                    }
                }

                if (!rtnStr.startsWith("D:")) {
                    msg("error reading in values");
                    return;
                } else {

                    splitArr = rtnStr.split(":");

                    if (Integer.valueOf(splitArr[1]) == 0) { // units = MPH
                        unitsText.setText("mph");
                    } else if (Integer.valueOf(splitArr[1]) == 1) { // units = KPH
                        unitsText.setText("kph");
                    } else {
                        msg("error reading units");
                        return;
                    }

                    maxSpeedText.setText(splitArr[2]);
                    mMeterWizardRatio.setspeedometerHalf(Integer.parseInt(splitArr[2]));
                    magnetsText.setText(splitArr[3]);

                    getOrigData(splitArr[4], finalDriveText, 1000000, false);
                    getOrigData(splitArr[5], meterRatioText, 1000000, false);
                    getOrigData(splitArr[6], tireSizeText, 0, true);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void getOrigData(String src, TextView dest, int divider, boolean tireSize) {

        if (src != null) {

            if (tireSize) {
                dest.setText(src);
            } else {
                dest.setText(String.valueOf((Double.valueOf(src)) / divider));
            }
        }
    }

    static void sendUnits(String indexStr) {

        if (btSocket != null) {
            try {
                String str = "U:";
                str = str + indexStr + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

                // send tire size too as units have changed
                sendTireSizeCalc(tireSizeText.getText().toString());

            } catch (IOException e) {
                msg("Error");
            }
        }

    }

    static void sendInfo(String textStr, String extraStr) {

        if (btSocket != null) {
            try {
                String str = extraStr + textStr + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    static void sendCalc(String textStr, String extraStr) {

        if (btSocket!=null) {
            try {
                String str = textStr;
                float result = Float.parseFloat(str);
                result=result*1000000;
                str = extraStr + Integer.toString((int) result) + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    static void sendTireSizeCalc(String tireSize) {

        String withoutP = tireSize.split("P")[1];
        String size1 = withoutP.split("/")[0];
        String size2 = withoutP.split("/")[1].split("R")[0];
        String size3 = withoutP.split("/")[1].split("R")[1];


        if (btSocket != null) {
            try {

                double result1 = Integer.parseInt(size1);
                double result2 = Integer.parseInt(size2);
                double result3 = Integer.parseInt(size3);

                double circ;
                double result;

                circ = ((result1 * result2 * 2.00 / 2540.00) + result3) * Math.PI;

                if (unitsText.getText().toString() == "kph") {
                    result = circ * 2.54/ 100.00;
                } else { // mph
                    result = circ * 1000.00 / 63360.00;
                }
                result = result * 1000000.00;
                String str = "W:" + String.valueOf((int)result) + ":" + tireSize + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    static public boolean testInt(TextView input) {
        try {

            // raise error if input is not numeric
            Double.parseDouble(input.getText().toString());
        }
        catch (NumberFormatException e){
            AlertDialog.Builder builder = new AlertDialog.Builder(thisContext);
            builder.setTitle("Error");
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage("Please enter a number");
            builder.setCancelable(true);

            final AlertDialog closeDialog = builder.create();
            closeDialog.show();

            // Display dialog box for 2 seconds
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    closeDialog.dismiss();
                    timer.cancel();
                }
            }, 2000);

            return false;
        }

        return true;
    }

    static public void setDriveCheck() {
        driveCheck = true;
    }

    static public boolean getDriveCheck() {
        return driveCheck;
    }

    static public String getUnits() {
        return unitsText.getText().toString();
    }
    static public void setUnits(String value) {
        if (value.equalsIgnoreCase("mph")) {
            // send to arduino if not the same as what is being stored
            if (unitsText.getText().toString().equalsIgnoreCase("kph")) {
                sendUnits(Integer.toString(0));
                unitsText.setText(value);
            }
        } else if (value.equalsIgnoreCase("kph")) {
            // send to arduino if not the same as what is being stored
            if (unitsText.getText().toString().equalsIgnoreCase("mph")) {
                sendUnits(Integer.toString(1));
                unitsText.setText(value);
            }
        }
    }

    static public String getMaxSpeed() {

        return maxSpeedText.getText().toString();
    }

    static public void setMaxSpeed(String value) {
        if (!value.equals(maxSpeedText.getText().toString())) {
            sendInfo(value, "M:");
            maxSpeedText.setText(value);
        }
    }

    static public void setMeterRatioText(String value) {
        if (!value.equals(meterRatioText.getText().toString())) {
            meterRatioText.setText(value);
            sendCalc(value, "S:");
        }
    }

    static public void setMagnetText(int value) {
        if (!(String.valueOf(value).equals(magnetsText.getText().toString()))) {
            magnetsText.setText(String.valueOf(value));
            sendInfo(Integer.toString(value), "N:");
        }
    }

    static public void setTireSize(String value) {
        if (!(String.valueOf(value).equals(tireSizeText.getText().toString()))) {
            tireSizeText.setText(value);
            sendTireSizeCalc(tireSizeText.getText().toString());
        }
    }

    static public void startCalibration() {
        if (btSocket != null) {
            try {
                String str = "D:S" + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    static public void endCalibration() {
        if (btSocket != null) {
            try {
                String str = "D:0" + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    static public boolean setFinalDrive(String avgSpeed) {

        if (btSocket != null) {
            try {

                String rtnStr = null;
                String[] splitArr;
                String str = "D:" + avgSpeed + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                for (int rtnBuff = 0; ((rtnBuff = btSocket.getInputStream().read()) >= 0) && rtnBuff != 13; ) {
                    if (rtnStr == null) {
                        rtnStr = Character.toString((char) rtnBuff);
                    } else {
                        rtnStr += Character.toString((char) rtnBuff);
                    }
                }

                msg(rtnStr);
                if (!rtnStr.startsWith("F:")) {
                    msg("error reading in values");
                    return false;
                } else {

                    splitArr = rtnStr.split(":");

                    Integer intFinalDrive = Integer.getInteger(splitArr[1]);
                    Double doubleFinalDrive = intFinalDrive/1000000.00;
                    finalDriveText.setText(String.valueOf(doubleFinalDrive));
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;

    }

    static private void msg(String s)
    {
        Toast.makeText(thisContext.getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }
}

