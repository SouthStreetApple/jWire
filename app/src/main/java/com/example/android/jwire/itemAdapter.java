package com.example.android.jwire;


import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;

import java.util.TimeZone;

public class itemAdapter extends ArrayAdapter<item> {
    public itemAdapter(Activity context, ArrayList<item> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        /**
         * This is where we're going to setup the listItemView
         */
        //Get current item
        item currentItem = getItem(position);

        //Check to see if the current item view is null, if it is create it
        View listItemView = convertView;
        if (listItemView == null) {
            //the view is currently null, create it
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.item_layout, parent, false);
        }

        /**
         * Here is where we update the list view.
         */

        //Update the title of the news item
        TextView itemTitle = (TextView) listItemView.findViewById(R.id.item_title);
        itemTitle.setText(currentItem.itemTitle);

        //Update the author of the news item, if there is no author, hide the textview
        if (currentItem.itemAuthor.equalsIgnoreCase("")) {
            TextView itemAuthor = (TextView) listItemView.findViewById(R.id.item_author);
            itemAuthor.setVisibility(View.GONE);
        } else {
            TextView itemAuthor = (TextView) listItemView.findViewById(R.id.item_author);
            itemAuthor.setText(currentItem.itemAuthor);
        }


        //Update the section name
        TextView itemSection = (TextView) listItemView.findViewById(R.id.item_section);
        itemSection.setText(currentItem.itemSection);

        //Parse the time string to create date and time, then display?
        TextView itemDate = (TextView) listItemView.findViewById(R.id.item_date_time);
        itemDate.setText(getDateTime(currentItem.itemDateTime));


        //Return the listViewItem!
        return listItemView;
    }

    public String getDateTime(String dateTime) {
        //Date/Time code adapted from a couple different resources
        //URL: https://stackoverflow.com/a/29703451/9849310
        //URL: https://stackoverflow.com/a/10725303/9849310
        String stringDate;
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a");
        inputFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = null;
        try {
            //Try to convert the date and show it if possible.
            //We remove the T, as it's not needed, and replace the Z with +0000 to make the
            //formatting work correctly.
            date = inputFormat.parse(dateTime.replace("T", " ").replace("Z", " +0000"));
            outputFormat.setTimeZone(TimeZone.getDefault());
            stringDate = outputFormat.format(date.getTime());

            return stringDate;

        } catch (ParseException e) {
            e.printStackTrace();
        }

        //We couldn't parse this for some reason, display the string the Guardian gave us.
        return dateTime;
    }
}