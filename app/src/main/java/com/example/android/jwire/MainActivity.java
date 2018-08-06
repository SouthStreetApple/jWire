package com.example.android.jwire;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String>{
    /**
     * AsyncTaskLoader code adapted from sanjeev yadav's excellent tutorial
     * URL: https://medium.com/@sanjeevy133/an-idiots-guide-to-android-asynctaskloader-76f8bfb0a0c0
     */

    //This is the loaderID, a unique number to reference the loader by if we need to later
    int loaderID = 1;
    //This is the string we're going to pass to the networking code so we can download the right
    //JSON data to parse and display news.  We'll add code later to allow the user to change this,
    //so we can customize the news search.
    static final String  dataURL =  "https://content.guardianapis.com/search?format=json&show-fields=byline,starRating,headline,thumbnail,short-url&api-key=dc0adc63-cbd9-4080-9f96-2a374025533e";
    //JSON data string
    String jsonData;

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, final Bundle args){
        //This is where the AsyncTaskLoader will be created
        return new AsyncTaskLoader<String>(this) {

            //We can hold the JSON data here for when we rotate the screen
            //URL: http://www.riptutorial.com/android/example/16217/asynctaskloader-with-cache
            private final AtomicReference<String> tempJSONData = new AtomicReference<>();

            @Override
            public String loadInBackground(){
                //This is where the start the networking stuff (so our screen doesn't freeze)
                String url = args.getString("url");
                if (url == null && "".equals(url)){
                    //If the url is somehow null (something went wrong) return nothing.
                    return null;
                } else {
                    String dataResult = "";
                    try {
                        //Now let's try to get that JSON data!
                        dataResult = makeHttpRequest(createUrl(url));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return dataResult;
                }
            }

            /**
             * Here is where we will see if the jsonData variable is empty, if it isn't we'll
             * just pass the data back and use it again.
             * If it is empty then we will need to forceLoad() and re-download the data!
             */
            @Override
            protected void onStartLoading(){
                //Now we are going to see if the jsonData variable is empty, if it is reload.
                if (tempJSONData.get() != null){
                    //Do not reload the HTTP, just send the data back we already have.
                    deliverResult(tempJSONData.get());
                } else {
                    forceLoad();
                }
            }

            @Override
            public void deliverResult(String data){
                tempJSONData.set(data);
                super.deliverResult(data);
            }
        };

    }

    @Override
    public void onLoadFinished(Loader<String> loader, String data){
        /**
         * The GUI may already be loaded (hopefully) so we need to refresh it to reflect the new
         * data we have collected.  This should wipe out the previous GUI and replace with GUI
         * that has all the new news items...
         *
         * Step 1: Send the data to the jsonUtilities class
         * Step 2: Set the listView to the newsItems array
         */
        jsonData = data;
        //Load the array with news items
        //We need to create a new class for jsonUtilities now, because it is no longer static
        jsonUtilities json = new jsonUtilities();
        //pass the json data to the jsonUtilities class
        json.jsonNewsResponse = jsonData;
        //now we will extract he json news items and return them as an arrayList!
        newsItems = json.extractNewsItems();

        //Check to see if newsItem is null
        if (newsItems != null){
            //Create a variable to reference the listView so we can modify it.
            ListView listView = findViewById(R.id.news_item_list);

            //Set the listView to use the info from the newsItems array
            itemAdapter ia = new itemAdapter(this,newsItems);
            listView.setAdapter(ia);

            //Set out ItemClickListener
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //Item is selected so we can open a browser with an intent
                    Intent webIntent = new Intent(Intent.ACTION_VIEW);
                    webIntent.setData(Uri.parse(newsItems.get(i).itemUrl));
                    startActivity(webIntent);
                }
            });
        } else {
            //newsItems was null, which happens when the JSON is incorrectly formatted (not empty)
            //Display an error message.
            TextView textViewStatus = (TextView) findViewById(R.id.news_status);
            textViewStatus.setText(getApplicationContext().getString(R.string.response_json_formatting));
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader){
        //Now sure what we can use this for yet.
    }

    ArrayList<item> newsItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Setup the loader
        getSupportLoaderManager().initLoader(loaderID,null,this);
        //Initiate the data retrieval
        makeOperationSearchQuery(dataURL);
    }

    /**
     * We call this subroutine to download the JSON and display it.
     */
    private void makeOperationSearchQuery(String url){
        //To trigger the loader we create a bundle
        Bundle queryBundle = new Bundle();
        //Now we need to put the URL string into the Bundle to it gets properly passed
        queryBundle.putString("url",url);

        //We now want to store getSupportLoaderManager in a variable
        LoaderManager loaderManager = getSupportLoaderManager();
        //Now we want to use the loader we made
        Loader<String> loader = loaderManager.getLoader(loaderID);

        //Now we need to check if the loader is null, if it is initiate it if otherwise, restart
        if(loader == null){
            loaderManager.initLoader(loaderID, queryBundle,this);
        } else {
            loaderManager.restartLoader(loaderID,queryBundle,this);
        }
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException exception) {
            Log.e("createURL", "Error with creating URL", exception);
            return null;
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        TextView textViewStatus = (TextView) findViewById(R.id.news_status);
        if(haveNetworkConnection()==true){
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                inputStream = urlConnection.getInputStream();
                //Now where going to handle the urlConnection Response codes, and return a JSON response.
                switch (urlConnection.getResponseCode()){
                    case 200:
                        textViewStatus.setText(getApplicationContext().getString(R.string.response_200));
                        jsonResponse = readFromStream(inputStream);
                        break;
                    case 400:
                        textViewStatus.setText(getApplicationContext().getString(R.string.response_400));
                        jsonResponse = "";
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                        if (inputStream != null) {
                            //We close the stream so that it doesn't remain open.
                            inputStream.close();
                        }
                        return jsonResponse;
                    case 403:
                        textViewStatus.setText(getApplicationContext().getString(R.string.response_403));
                        jsonResponse = "";
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                        if (inputStream != null) {
                            //We close the stream so that it doesn't remain open.
                            inputStream.close();
                        }
                        return jsonResponse;
                    case 500:
                        textViewStatus.setText(getApplicationContext().getString(R.string.response_500));
                        jsonResponse = "";
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                        if (inputStream != null) {
                            //We close the stream so that it doesn't remain open.
                            inputStream.close();
                        }
                        return jsonResponse;
                    default:
                        //Instead of being a default response we're using this like an 'else' response.
                        textViewStatus.setText(getApplicationContext().getString(R.string.response_other));
                        jsonResponse = "";
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                        if (inputStream != null) {
                            //We close the stream so that it doesn't remain open.
                            inputStream.close();
                        }
                        return jsonResponse;
                }
            } catch (IOException e) {
                //We've had an error not handled by the response codes, display the message.
                textViewStatus.setText(getApplicationContext().getString(R.string.response_error) + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    //We close the stream so that it doesn't remain open.
                    inputStream.close();
                }
            }
        } else {
            //No internet connection
            textViewStatus.setText(getApplicationContext().getString(R.string.response_no_connection));
            return jsonResponse;
        }
        //If there was no other errors or issues, but the JSON response is still empty.
        if(jsonResponse.equalsIgnoreCase("")){
            textViewStatus.setText(getApplicationContext().getString(R.string.response_no_data));
        }
        //Return JSON response
        return jsonResponse;
    }

    /**
     * Convert the inputStream into a string we can use and parse.
     */
    private String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Function to detect internet connection
     * URL: https://stackoverflow.com/a/4239410/9849310
     */
    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}
