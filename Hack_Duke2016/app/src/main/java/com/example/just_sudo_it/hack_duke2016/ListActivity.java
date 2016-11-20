package com.example.just_sudo_it.hack_duke2016;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ListActivity extends AppCompatActivity {

    List<String> appData;
    UsageStatsManager mUsageStatsManager;
    SharedPreferences preferences;

    private static final int SECOND = 1000;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//            }
//        });

        setContentView(R.layout.activity_list);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        mUsageStatsManager = (UsageStatsManager) getSystemService("usagestats");

        if(!doIHavePermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }

        updateAppData();
        final ArrayAdapter adapter = new ArrayAdapter<>(this,
                R.layout.activity_view, appData);

        ListView listView = (ListView) findViewById(R.id.app_list);
        listView.setAdapter(adapter);

        Button updateButton = (Button) findViewById(R.id.update);

        updateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                updateAppData();
                adapter.clear();
                adapter.addAll(appData);
                adapter.notifyDataSetChanged();
            }

        });

        Button resetButton = (Button) findViewById(R.id.reset);

        resetButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong("StartTime", System.currentTimeMillis());
                editor.commit();
                updateAppData();
                adapter.clear();
                adapter.addAll(appData);
                adapter.notifyDataSetChanged();
            }

        });
    }

    public void updateAppData() {

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String PackageName;

        long currTime = System.currentTimeMillis();
        long startTime = preferences.getLong("StartTime", 0);
        Log.d("currTime", String.valueOf(currTime));
        Log.d("StartTime", String.valueOf(startTime));

        if(startTime < currTime - 500) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong("StartTime", startTime);
            editor.commit();
        }

        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, currTime);
        Map<String,Long> apps = new HashMap<>();
        ValueComparator bvc = new ValueComparator(apps);
        TreeMap<String, Long> sortedMap = new TreeMap<String, Long>(bvc);

        if (stats != null) {;
            for (int i = 0; i<stats.size();i++) {
                PackageName = stats.get(i).getPackageName();
                long totalTimeInMilli = stats.get(i).getTotalTimeInForeground() - startTime;
                if(totalTimeInMilli > (long)1000) {
                    apps.put(PackageName , totalTimeInMilli);
                }
            }
        }
        sortedMap.putAll(apps);
        List<String> data = new ArrayList<>();
        for(Map.Entry<String, Long> e : sortedMap.entrySet()){
            data.add(e.getKey() + "    " + convertMillitoString(e.getValue()));
            Log.d("hi",e.getKey() + "    " + convertMillitoString(e.getValue()));
        }
        appData = data;
    }

    String convertMillitoString(long ms){
        StringBuilder text = new StringBuilder("");
        if (ms > DAY) {
            text.append(ms / DAY).append(" days ");
            ms %= DAY;
        }
        if (ms > HOUR) {
            text.append(ms / HOUR).append(" hours ");
            ms %= HOUR;
        }
        if (ms > MINUTE) {
            text.append(ms / MINUTE).append(" minutes ");
            ms %= MINUTE;
        }
        if (ms > SECOND) {
            text.append(ms / SECOND).append(" seconds ");
            ms %= SECOND;
        }
        text.append(ms + " ms");
        return text.toString();
    }

    class ValueComparator implements Comparator<String> {
        Map<String, Long> base;

        public ValueComparator(Map<String, Long> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with
        // equals.
        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }

    public boolean doIHavePermission(){

        return !(mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, 0, System.currentTimeMillis()).size() == 0);

    }

}
