package com.android.mechanicalspeedometerconverter;

import android.support.v7.widget.RecyclerView;
import android.widget.ArrayAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

public class BluetoothDevicesAdapter extends ArrayAdapter<BluetoothDevice> {

    public TextView name;
    public TextView address;

    // View lookup cache
     public View viewHolder(View view) {
        //super(view);
        name = view.findViewById(R.id.device_name);
        address = view.findViewById(R.id.device_address);

        //public ViewHolder(View view) {
        //    ButterKnife.bind(this, view);
        //}
    }

    public BluetoothDevicesAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the data item for this position
        BluetoothDevice device = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        //ViewHolder viewHolder; // view lookup cache stored in tag

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_device, parent, false);
            viewHolder = new viewHolder(convertView);
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Populate the data into the template view using the data object
        viewHolder.name.setText(device.getName());
        viewHolder.address.setText(device.getAddress());

        // Return the completed to render on screen
        return convertView;
    }
}
