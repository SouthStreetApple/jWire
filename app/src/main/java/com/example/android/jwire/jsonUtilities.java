package com.example.android.jwire;

import android.app.Application;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class jsonUtilities {

  //String variable to hold JSON response
  String jsonNewsResponse = "";

    public jsonUtilities() {
    }

    /**
     * We're trying to return a list of news items from the selected JSON response
     */

    //public static ArrayList<item> extractNewsItems() {
    public ArrayList<item> extractNewsItems() {

        // Create an empty ArrayList that we can start adding news items to
        ArrayList<item> newsItems = new ArrayList<>();

        /**
         * We're going to try and parse the JSON response here, first let's see if the JSON reply
         * is not empty.
         */
        if (jsonNewsResponse.equalsIgnoreCase("")){
            return newsItems;
        }
        /**
         * Now we will try to parse the JSON
         */
        try {

            //Declaring a new JSON response, which we'll create from the string response we got from
            //the HTTP request
            JSONObject response;
            response = new JSONObject(jsonNewsResponse);

            //JSON Response Variables
            JSONArray jsonItems;
            JSONObject jsonItem;

            //jsonItem data variables
            String time;
            String url;
            String title;
            String section;
            String byline = "";


            //Get number (count) of news items
            jsonItem = new JSONObject(response.getString("response"));
            int count = jsonItem.getInt("pageSize");//response.getInt("pageSize");

            //newsItem variable so we can add this to the earthquakes array.
            item newsItem;

            //Get JSON Array
            jsonItems = jsonItem.getJSONArray("results");//response.getJSONArray("results");

            //If newsItem count != 0 then continue.
            if(count != 0) {
                for (int i = 0; i < count ; i++) {
                    try{
                        //initialize new newsItem object
                        newsItem = new item();

                        //grab the current item from the JSON array
                        jsonItem = jsonItems.getJSONObject(i);

                        //Grab the item details
                        //Check to see if each is available!
                        if(jsonItem.has("webPublicationDate")){
                            time = jsonItem.getString("webPublicationDate");
                        }else{
                            time = "";
                        }
                        if(jsonItem.has("webUrl")){
                            url = jsonItem.getString("webUrl");
                        }else{
                            url = "";
                        }
                        if(jsonItem.has("webTitle")){
                            title = jsonItem.getString("webTitle");
                        } else {
                            title = "";
                        }
                        if(jsonItem.has("sectionName")){
                            section = jsonItem.getString("sectionName");
                        }else {
                            section = "";
                        }
                        if(jsonItem.has("fields")){
                            jsonItem = new JSONObject(jsonItem.getString("fields"));
                            if(jsonItem.has("byline")){
                                byline = jsonItem.getString("byline");
                            } else {
                                byline = "";
                            }
                        }else {
                        }

                        //Set news details to newsItem
                        newsItem.setItem(title,section,time,url, byline);
                        //Add newsItem to newsItems Array
                        newsItems.add(newsItem);

                    } catch (JSONException e) {
                        return null;
                    }
                }
            }

        } catch (JSONException e) {
            return null;
        }

        // return news items
        return newsItems;
    }
}
