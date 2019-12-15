package com.dai.timekeep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;

import java.security.Permission;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
        if(sharedPreferences.getBoolean( getString(R.string.cycleOnKey), false)){
            Intent i1 = new Intent(this, ProgressActivity.class);
            startActivity(i1);
        }
        else{
            setContentView(R.layout.activity_main);
        }


    }

    public void toSleep(View view) {
        Intent i1 = new Intent(this, SleepActivity.class);
        startActivity(i1);
    }

    public void toTasks(View view) {
        Intent i1 = new Intent(this, TaskActivity.class);
        i1.putExtra(getString(R.string.taskBooleanExtra), false);
        startActivity(i1);
    }

    public void toCalendar(View view) {
        Intent i1 = new Intent(this, CalendarActivity.class);
        startActivity(i1);
    }


}
