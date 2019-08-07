package com.example.agriculturalcompany;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private List<String> values = new ArrayList<String>();
    private String moisture_val;
    private String temperature_val;
    private String smoke_val;
    private Button clickButton;
    private TextView current_moisture;
    private TextView current_temperature;
    private TextView current_smoke;
    private static final int threshold_moisture = 60;
    private static final int threshold_temp = 32;
    private static final int threshold_smoke = 100;
    private static final int num_0f_graphs = 3;
    private static final int limit = 100;
    private Mail m;
    //for choosing threshold and send email
    private boolean flag = false;
    private int timer;
    private int counter=0;
    //check internet connection
    boolean connected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check internet connection
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
        }
        else {
            connected = false;
            Toast.makeText(MainActivity.this, "NO INTERNET CONNECTION", Toast.LENGTH_LONG).show();
        }


        if(connected) {
            new JsonTask().execute();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    new JsonTask().execute();
                    handler.postDelayed(this, 15000); //now is every 10 secs
                    //Log.d("Time", "timer: 10");
                }
            }, 15000); //Every 10 secs

            if (flag) {
                Toast.makeText(MainActivity.this, "Email was sent successfully.", Toast.LENGTH_LONG).show();
                flag = false;
            }

            clickButton = (Button) findViewById(R.id.button);
            current_moisture = (TextView) findViewById(R.id.current_moisture);
            current_temperature = (TextView) findViewById(R.id.current_temperature);
            current_smoke = (TextView) findViewById(R.id.current_smoke_levels);
            clickButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    current_moisture.setText("Current Moisture: " + moisture_val);
                    current_temperature.setText("Current Temperature: " + temperature_val);
                    current_smoke.setText("Current Smoke Levels: " + smoke_val);
                }
            });
        }

    }

    public class JsonTask extends AsyncTask<Void, Void, DataPoint[]> {

        @Override
        protected DataPoint[] doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("https://api.thingspeak.com/channels/802148/feeds.json?api_key=IHQG1J44DR0CHILC&results="+limit)
                    .build();

            Response response = null;

            flag = false;

            try{
                values.clear();
                response = client.newCall(request).execute();
                JSONObject j = new JSONObject(response.body().string().toString());
                JSONArray a = j.getJSONArray("feeds");
                //GET MOISTURE
                for (int i=0;i<limit;i++) {
                    j = a.getJSONObject(i);
                    moisture_val = j.getString("field3");
                    values.add(moisture_val);
                }
                //check last value if moisture very low
                if(Integer.parseInt(moisture_val)<threshold_moisture){
                    flag = true;
                }
                //GET TEMPERATURE
                for (int i=0;i<limit;i++) {
                    j = a.getJSONObject(i);
                    temperature_val = j.getString("field2");
                    values.add(temperature_val);
                }
                //check last value
                if(Integer.parseInt(temperature_val)>threshold_temp){
                    flag = true;
                }
                //GET Smoke Levels
                for (int i=0;i<limit;i++) {
                    j = a.getJSONObject(i);
                    smoke_val = j.getString("field1");
                    values.add(smoke_val);
                }
                //check last value
                if(Integer.parseInt(smoke_val)>threshold_smoke){
                    flag = true;
                }

                //Log.d("this is my array", "arr1: " + values);

                DataPoint[] items = new DataPoint[limit*num_0f_graphs];
                for(int i=0;i<limit*num_0f_graphs;i++){
                    //Log.d("this is my value", "moisture_val: " + values.get(i));
                    DataPoint v = new DataPoint(i, Integer.parseInt(values.get(i)));
                    //Log.d("this is my value", "temperature_val: " + values.get(i).toString());
                    items[i] = v;
                    //Log.d("this is my array", "arr2: " + items[299]);

                }
                if(flag){
                    counter++;
                    //if is the first email send it
                    if (counter==1) {
                        timer = new Time(System.currentTimeMillis()).getMinutes();
                        Log.d("this is my timer", "timer: " + timer);
                    }
                    //if you send email before <5 min dont send again
                    if((new Time(System.currentTimeMillis()).getMinutes()) - timer >= 5 || counter==1) {
                        m = new Mail("greendustry4.0@gmail.com", "SpanosDasyras12");
                        String[] toArr = {"anastasisdasy@gmail.com", "greendustry4.0@gmail.com"};
                        m.setTo(toArr);
                        m.setFrom("greendustry4.0@gmail.com");
                        m.setSubject("WAREHOUSE ALERT");
                        m.setBody("Something goes wrong in our warehouse, check our app!");
                        timer = new Time(System.currentTimeMillis()).getMinutes();
                        try {
                            m.send();
                        } catch (Exception e) {
                            Log.e("MailApp", "Could not seßnd email", e);
                        }

                    }

                }

                return items;


            }catch (IOException e){
                e.printStackTrace();
            }catch (JSONException e){
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(DataPoint[] items) {
            super.onPostExecute(items);
            DataPoint[] moisture_val = new DataPoint[limit];
            DataPoint[] temperature_val = new DataPoint[limit];
            DataPoint[] smoke_val = new DataPoint[limit];
            for(int i=0;i<limit;i++){
                //items list has the total datapoints for the three graphs, so seprate them
                moisture_val[i] = new DataPoint(i, items[i].getY());
                temperature_val[i] = new DataPoint(i, items[i+limit].getY());
                smoke_val[i] = new DataPoint(i, items[i+(limit*2)].getY());

                //find max values to dynamically change
            }


            //Log.d("this is my array", "arr: " + Arrays.toString(smoke_val));
            //MOISTURE GRAPH
            GraphView moisture_graph = (GraphView)findViewById(R.id.moisture);
            LineGraphSeries<DataPoint> moisture_series = new LineGraphSeries<DataPoint>(moisture_val);
            moisture_graph.removeAllSeries();
            moisture_graph.addSeries(moisture_series);
            moisture_graph.getViewport().setYAxisBoundsManual(true);
            moisture_graph.getViewport().setXAxisBoundsManual(true);
            moisture_graph.getViewport().setMaxY(100);
            moisture_graph.getViewport().setMinX(10);
            moisture_graph.getViewport().setMinY(10);

            //TEMPERATURE GRAPH
            GraphView temperature_graph = (GraphView)findViewById(R.id.temperature);
            LineGraphSeries<DataPoint> temperature_series = new LineGraphSeries<DataPoint>(temperature_val); //change val
            temperature_graph.removeAllSeries();
            temperature_graph.addSeries(temperature_series);
            temperature_graph.getViewport().setYAxisBoundsManual(true);
            temperature_graph.getViewport().setXAxisBoundsManual(true);
            temperature_graph.getViewport().setMaxY(100);
            temperature_graph.getViewport().setMinX(10);
            temperature_graph.getViewport().setMinY(10);

            //SMOKE GRAPH
            GraphView smoke_graph = (GraphView)findViewById(R.id.smoke);
            LineGraphSeries<DataPoint> smoke_seriesies = new LineGraphSeries<DataPoint>(smoke_val); //change val
            smoke_graph.removeAllSeries();
            smoke_graph.addSeries(smoke_seriesies);
            smoke_graph.getViewport().setYAxisBoundsManual(true);
            smoke_graph.getViewport().setXAxisBoundsManual(true);
            smoke_graph.getViewport().setMaxY(200);
            smoke_graph.getViewport().setMinX(10);
            smoke_graph.getViewport().setMinY(10);
        }
    }





}
