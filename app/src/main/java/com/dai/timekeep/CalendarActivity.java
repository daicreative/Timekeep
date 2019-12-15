package com.dai.timekeep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentUris;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarActivity extends AppCompatActivity implements CalendarAdapter.OnCalListener {

    private RecyclerView recyclerView;
    private CalendarAdapter mAdapter;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String[] calendarNames;
    private int[] calendarIds;
    private MutableInteger selectedIndex;

    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
        editor = sharedPreferences.edit();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, 1);
        }
        else{
            loadCalendars();
            setupList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i = 0; i < permissions.length; i++){
            if(permissions[i].compareTo(Manifest.permission.READ_CALENDAR) == 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED){
                loadCalendars();
                setupList();
            }
        }
    }

    private void setupList() {
        recyclerView = findViewById(R.id.calendarList);
        recyclerView.setHasFixedSize(true);
        selectedIndex = new MutableInteger(calendarNames.length - 1);
        if(sharedPreferences.contains(getString(R.string.calendarIdKey))){
            //find cal index with id
            int chosenID = sharedPreferences.getInt(getString(R.string.calendarIdKey), -1);
            for(int i = 0; i < calendarIds.length; i++){
                if(calendarIds[i] == chosenID){
                    selectedIndex.value = i;
                }
            }
        }
        mAdapter = new CalendarAdapter(this, calendarNames, selectedIndex, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
    }

    private void loadCalendars() {
        Cursor cursor;

        cursor = getContentResolver().query(Uri.parse("content://com.android.calendar/calendars"),
                new String[] { "_id", "calendar_displayName" }, null, null, null);

        // Get calendars name
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            calendarNames = new String[cursor.getCount() + 1];
            calendarNames[calendarNames.length - 1] = "None";
            // Get calendars id
            calendarIds = new int[cursor.getCount()];
            for (int i = 0; i < cursor.getCount(); i++) {
                calendarIds[i] = cursor.getInt(0);
                calendarNames[i] = cursor.getString(1);
                cursor.moveToNext();
            }
        }
    }

    @Override
    public void OnCalClick(int position) {
        if(position == selectedIndex.value){
            return;
        }
        if(position == calendarNames.length - 1){
            editor.remove(getString(R.string.calendarIdKey));
            editor.commit();
        }
        else{
            editor.putInt(getString(R.string.calendarIdKey), calendarIds[position]);
            editor.commit();
        }
        int old = selectedIndex.value;
        selectedIndex.value = position;
        mAdapter.notifyItemChanged(position);
        mAdapter.notifyItemChanged(old);
    }
}
