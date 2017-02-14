package com.example.konradbujak.closestbeaconapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Konrad.Bujak on 20.10.2016.
 */

class MySimpleArrayAdapter extends ArrayAdapter<String> {

    private final Context context;
    private static final String TAG = "XYZ";
    private final ArrayList<String> uids = new ArrayList<>();
    private final ArrayList<Double> distances = new ArrayList<>();

        private static class ViewHolder
        {
            TextView distance;
            TextView uid;
            ImageView icon;
            int position;
        }

    MySimpleArrayAdapter(Context context)
        {
            super(context, R.layout.array_adapter);
            this.context = context;
        }

    void updateList(ArrayList<Double> newDistances, ArrayList<String> newUIDs) {
        distances.clear();
        uids.clear();
        distances.addAll(newDistances);
        uids.addAll(newUIDs);
        notifyDataSetChanged();
        Log.d(TAG, "Update beacons: " +getCount());
    }

    public int getCount()
    {
        return distances.size();
    }
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("ViewHolder") View rowView = inflater.inflate(R.layout.array_adapter, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.distance = (TextView) rowView.findViewById(R.id.secondLine);
        holder.uid = (TextView) rowView.findViewById(R.id.firstLine);
        holder.icon = (ImageView) rowView.findViewById(R.id.icon);
        rowView.setTag(holder);
        if(uids.size() <=0)
          {
              holder.uid.setText("No beacons");
          }
        else
        {
            holder.distance.setText("Distance : " + distances.get(position) + " m");
            holder.uid.setText("Unique Id : " + uids.get(position));
            holder.icon.setImageResource(R.drawable.ibeacon);
        }
        return rowView;
    }

}