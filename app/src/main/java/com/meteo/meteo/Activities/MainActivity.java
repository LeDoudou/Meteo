package com.meteo.meteo.Activities;

import android.app.AlarmManager;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.meteo.meteo.Activities.Adapter.ForecastAdapter;
import com.meteo.meteo.R;
import com.meteo.meteo.misc.Utility;
import com.meteo.meteo.data.WeatherContract;
import com.meteo.meteo.service.ForecastService;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static String LOG_TAG = MainActivity.class.getSimpleName();
    private ForecastAdapter mForecastAdapteur;
    private String mCurrentLocation;

    private static final int FORECAST_LOADER = 0;

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

            public static final int COL_WEATHER_ID = 0;
            public static final int COL_WEATHER_DATE = 1;
            public static final int COL_WEATHER_DESC = 2;
            public static final int COL_WEATHER_MAX_TEMP = 3;
            public static final int COL_WEATHER_MIN_TEMP = 4;
            public static final int COL_LOCATION_SETTING = 5;
            public static final int COL_WEATHER_CONDITION_ID = 6;
            public static final int COL_COORD_LAT = 7;
            public static final int COL_COORD_LONG = 8;

    //////////////////////////////////
    //                              //
    //           onCreate           //
    //                              //
    //////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setElevation(0f);

        //Cursor Loader Methode
        //-----------------------
            mForecastAdapteur = new ForecastAdapter(this, null, 0);                     //Create Adapter without Cursor

            ListView listmeteo = (ListView) this.findViewById(R.id.listview_meteo);     //Attach adapter to the listview
            listmeteo.setAdapter(mForecastAdapteur);


        //Click Listener
        //-------------
            listmeteo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                    Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                    if (cursor != null) {
                        String locationSetting = Utility.getPreferredLocation(getApplicationContext());

                        //Start intent
                            Intent intent = new Intent(   getApplicationContext(), DetailActivity.class)
                                                     .setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE)  ));
                            startActivity(intent);
                    }
                }
            });


        //Store Current Location
        //----------------------
            mCurrentLocation = Utility.getPreferredLocation(this);

        //Start Loader
        //------------
            getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    //////////////////////////////////
    //                              //
    //             Loader           //
    //                              //
    //////////////////////////////////
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        //Get Location
        String locationSetting = Utility.getPreferredLocation(this);

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());

        //Return Cursor
        return new CursorLoader(this, weatherForLocationUri,FORECAST_COLUMNS,null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mForecastAdapteur.swapCursor(cursor);

        if(ForecastService.cityName != null) {
            setTitle("  " + ForecastService.cityName);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapteur.swapCursor(null);
    }

    //////////////////////////////////
    //                              //
    //             onStart          //
    //                              //
    //////////////////////////////////
    @Override
    protected void onStart() {
        super.onStart();

        Refresh_weather();          //Update meteo on first start
    }

    //////////////////////////////////
    //                              //
    //           onResume           //
    //                              //
    //////////////////////////////////
    @Override
    protected void onResume() {     //When come back from an other screen (like DetailActivity) check if location changed
        super.onResume();

        String location = Utility.getPreferredLocation(this);

        if(location != null && !location.contentEquals(mCurrentLocation)){      //Update datas if changed
            Refresh_weather();
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
            mCurrentLocation = location;
        }
    }

    //////////////////////////////////
    //                              //
    //        Refresh_weather       //
    //                              //
    //////////////////////////////////
    public void Refresh_weather(){

        String location = Utility.getPreferredLocation(this);

        //METHODE 2
        //---------
            //FetchWeatherTask weatherTask = new FetchWeatherTask(this);
            //weatherTask.execute(location);

        //Service Methode
        //---------------
            //Intent
            //Intent serviceIntent = new Intent(this, ForecastService.class);
            //    serviceIntent.putExtra(ForecastService.LOCATION_EXTRA, location);
            //startService(serviceIntent);

        //Methode Alarm
        //-------------
            Intent alarmIntent = new Intent(this, ForecastService.DownloadAlarmReceiver.class);
                alarmIntent.putExtra(ForecastService.LOCATION_EXTRA, Utility.getPreferredLocation(this));

            //Set the AlarmManager to wake up the system.
            AlarmManager alarmManager=(AlarmManager)getSystemService(ALARM_SERVICE);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,alarmIntent,PendingIntent.FLAG_ONE_SHOT);//getBroadcast(context, 0, i, 0);
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, pendingIntent);
    }

    //////////////////////////////////
    //                              //
    //             MENU             //
    //                              //
    //////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

            case R.id.Menu_Params:
                    startActivity(new Intent(this, SettingsActivity.class));        //Launched settings
                return true;

            case R.id.Menu_Map :
                    OpenMap();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    //////////////////////////////////
    //                              //
    //            OpenMap           //
    //                              //
    //////////////////////////////////
    public void OpenMap(){
        //Get location
            String location = Utility.getPreferredLocation(this);

        //Compute Url
            Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                                                    .appendQueryParameter("q", location)
                                                    .build();
        //Intent
            Intent GeoIntent = new Intent(Intent.ACTION_VIEW);
                GeoIntent.setData(geoLocation);

            if(GeoIntent.resolveActivity(getPackageManager()) != null){     //Check if there is an App to display map
                startActivity(GeoIntent);                                   //Send Action
            }else{
                Toast.makeText(this, getString(R.string.no_map_app_msp), Toast.LENGTH_LONG).show();
                Log.v(LOG_TAG, "No Map editor !!");
            }
    }
}
