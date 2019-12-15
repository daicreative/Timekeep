package com.dai.timekeep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProgressActivity extends AppCompatActivity implements ProgressAdapater.OnProgressListener {

    public TextView[] timerTexts;
    private boolean[] active;

    private String[] taskNames;

    private RecyclerView recyclerView;
    private ProgressAdapater mAdapter;

    private SharedPreferences sharedPreferences;

    private TimerService service = null;
    private boolean mBound = false;


    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        Intent intent = new Intent(this, TimerService.class);
        bindService(intent, connection, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mBound){
            service.detach();
        }
        unbindService(connection);
        mBound = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        //Check if service is running
        sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
        if(sharedPreferences.getBoolean( getString(R.string.cycleOnKey), false)){
            return;
        }

        //Set cycle on because it wasn't yet
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.cycleOnKey), true);
        editor.commit();

        //Get data from intent
        Intent old = getIntent();
        taskNames = old.getStringArrayExtra(getString(R.string.taskOrderExtra));
        active = new boolean[taskNames.length];
        active[0] = true;

        recyclerSetup();;

        textViewSetup();

        //Set up service
        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.putExtras(old);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }
        else{
            startService(serviceIntent);
        }
    }

    public void reset(View view) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.cycleOnKey), false);
        editor.commit();
        if(mBound){
            service.reset();
        }
        Intent i1 = new Intent(this, MainActivity.class);
        i1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i1);
    }

    private void recyclerSetup(){
        //Set up recycler
        recyclerView = findViewById(R.id.orderList);
        recyclerView.setHasFixedSize(true);
        mAdapter = new ProgressAdapater(this, taskNames, active,this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
    }

    public void textViewSetup(){
        //Set up TextViews
        timerTexts = mAdapter.getTextViews();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder iBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            service = ((TimerService.TimerBinder) iBinder).getService();
            bindToService(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void bindToService(TimerService service) {
        if(service.beenAttached()){
            //get the info needed
            sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
            taskNames = service.getTaskNames();
            active = service.getActive();
            recyclerSetup();
            textViewSetup();
            service.attach(this, null);
        }
        else{
            //give it the info it needed
            service.attach(this, active);
            service.startTimer();
        }
    }

    @Override
    public void OnProgressClick(int position) {
        int activePos = -1;
        for(int i = 0; i < active.length; i++){
            if(active[i]){
                activePos = i;
            }
        }
        if(position == activePos){
            return;
        }
        if(service.checkActive(position)){
            active[position] = true;
            active[activePos] = false;
        }
        else{
            return;
        }
//        if(active[position]){
//            int activeCount = 0;
//            for(int i = 0; i < active.length; i++){
//                if(active[i]){
//                    activeCount++;
//                }
//            }
//            if(activeCount == 1){
//                return;
//            }
//        }
//        active[position] = !active[position];
        mAdapter.notifyDataSetChanged();
        service.startTimer();
    }

    public void changeAt(int timerIndex) {
        mAdapter.notifyItemChanged(timerIndex);
    }
}
