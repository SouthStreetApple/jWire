package com.example.android.jwire;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {
    /**
     * AsyncTaskLoader code adapted from sanjeev yadav's excellent tutorial
     * URL: https://medium.com/@sanjeevy133/an-idiots-guide-to-android-asynctaskloader-76f8bfb0a0c0
     */

    //This is the loaderID, a unique number to reference the loader by if we need to later
    int loaderID = 1;
    //This is the string we're going to pass to the networking code so we can download the right
    //JSON data to parse and display news.  We'll add code later to allow the user to change this,
    //so we can customize the news search.

    //Old complete URL, REMOVE LATER
    //static final String  dataURL =  "https://content.guardianapis.com/search?format=json&show-fields=byline,starRating,headline,thumbnail,short-url&api-key=dc0adc63-cbd9-4080-9f96-2a374025533e";

    //This is the base URL, we'll construct the rest of it later in constructURL
    static final String dataURL = "https://content.guardianapis.com/search?";

    //JSON data string
    String jsonData;

    //TextView Status
    public TextView textViewStatus;

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, final Bundle args) {
        //This is where the AsyncTaskLoader will be created
        return new AsyncTaskLoader<String>(this) {

            //We can hold the JSON data here for when we rotate the screen
            //URL: http://www.riptutorial.com/android/example/16217/asynctaskloader-with-cache
            private final AtomicReference<String> tempJSONData = new AtomicReference<>();

            @Override
            public String loadInBackground() {
                //This is where the start the networking stuff (so our screen doesn't freeze)
                String url = args.getString("url");
                if (url == null && "".equals(url)) {
                    //If the url is somehow null (something went wrong) return nothing.
                    return null;
                } else {
                    String dataResult = "";
                    try {
                        //Now let's try to get that JSON data!
                        dataResult = makeHttpRequest(constructURL(url));
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
            protected void onStartLoading() {
                //Now we are going to see if the jsonData variable is empty, if it is reload.
                if (tempJSONData.get() != null) {
                    //Do not reload the HTTP, just send the data back we already have.
                    deliverResult(tempJSONData.get());
                } else {
                    forceLoad();
                }
            }

            @Override
            public void deliverResult(String data) {
                tempJSONData.set(data);
                super.deliverResult(data);
            }
        };

    }

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {
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

        if (jsonData == null) {
            //The JSON response is empty, there must have been a problem with the response
            textViewStatus.setText(getApplicationContext().getString(R.string.response_no_connection));
        } else if (jsonData.equalsIgnoreCase("")) {
            //jsonData is null, which should happen when there's been no internet connection
            textViewStatus.setText(getApplicationContext().getString(R.string.response_no_data));
        } else {
            //JSON response is not empty (and not null) let's try and extract the news items.
            //and return them as an arrayList!
            newsItems = json.extractNewsItems();
            //Check to see if newsItem is null
            if (newsItems != null) {

                //Create a variable to reference the listView so we can modify it.
                ListView listView = findViewById(R.id.news_item_list);

                //Set the listView to use the info from the newsItems array
                itemAdapter ia = new itemAdapter(this, newsItems);
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
                //newsItems was null, which happens when the JSON is incorrectly formatted or empty
                //Display an error message.
                textViewStatus.setText(getApplicationContext().getString(R.string.response_json_formatting));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {
        //Now sure what we can use this for yet.
    }

    ArrayList<item> newsItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Setup variable to display the status
        textViewStatus = (TextView) findViewById(R.id.news_status);
        //Setup the loader
        getSupportLoaderManager().initLoader(loaderID, null, this);
        //Initiate the data retrieval
        makeOperationSearchQuery(dataURL);
    }

    /**
     * The below will construct the URI from the string with the base URL for The Guardian
     */
    private URL constructURL(String baseURL) {
        //We need to load the preferences into memory
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String maximumResults = sharedPrefs.getString(getString(R.string.settings_number_of_results_key), getString(R.string.settings_number_of_results_default));
        String searchTerms = sharedPrefs.getString(getString(R.string.settings_search_terms_key), getString(R.string.settings_search_terms_default));

        //Create BASE URI from the
        Uri baseUri = Uri.parse(baseURL);

        //Build the URI with new query parameters.
        Uri.Builder uriBuilder = baseUri.buildUpon();

        //Append the default query parameters
        //Format
        uriBuilder.appendQueryParameter("format", "json");
        //Editions
        uriBuilder.appendQueryParameter("edition", "us");
        //Tags to show
        uriBuilder.appendQueryParameter("show-tags", "contributor");
        //Fields to show
        uriBuilder.appendQueryParameter("show-fields", "starRating,headline,thumbnail,short-url,byline");
        //Order to show in (may be an option later..
        uriBuilder.appendQueryParameter("order-by", "newest");

        //Append Option Parameters
        uriBuilder.appendQueryParameter("page-size", maximumResults);
        uriBuilder.appendQueryParameter("q", searchTerms);

        //API KEY
        uriBuilder.appendQueryParameter("api-key", getString(R.string.api_key));

        //Return the URL!
        URL url = createUrl(uriBuilder.toString());

        return url;
    }

    /**
     * We call this subroutine to download the JSON and display it.
     */
    private void makeOperationSearchQuery(String url) {
        //To trigger the loader we create a bundle
        Bundle queryBundle = new Bundle();
        //Now we need to put the URL string into the Bundle to it gets properly passed
        queryBundle.putString("url", url);

        //We now want to store getSupportLoaderManager in a variable
        LoaderManager loaderManager = getSupportLoaderManager();
        //Now we want to use the loader we made
        Loader<String> loader = loaderManager.getLoader(loaderID);

        //Now we need to check if the loader is null, if it is initiate it if otherwise, restart
        if (loader == null) {
            loaderManager.initLoader(loaderID, queryBundle, this);
        } else {
            loaderManager.restartLoader(loaderID, queryBundle, this);
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
        if (haveNetworkConnection() == true) {
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                inputStream = urlConnection.getInputStream();
                //Now where going to handle the urlConnection Response codes, and return a JSON response.
                switch (urlConnection.getResponseCode()) {
                    case 200:
                        jsonResponse = readFromStream(inputStream);
                        break;
                    default:
                        //Instead of being a default response we're using this like an 'else' response.
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
                //Error
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
            //No internet connection, JSON will be empty.
            return null;
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

    /**
     * Now we're going to add the menu code so that our custom settings menu shows up!
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
