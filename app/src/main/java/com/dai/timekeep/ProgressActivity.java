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
        String[] taskNamesTemp = old.getStringArrayExtra(getString(R.string.taskOrderExtra));
        taskNames = new String[taskNamesTemp.length + 1];
        taskNames[0] = getString(R.string.schedule);
        System.arraycopy(taskNamesTemp, 0, taskNames, 1, taskNamesTemp.length);
        active = new boolean[taskNames.length];
        active[1] = true;

        recyclerSetup();

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
            //get the info needed - service has been running
            sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
            taskNames = service.getTaskNames();
            active = service.getActive();
            recyclerSetup();
            textViewSetup();
            service.attach(this, null);
        }
        else{
            //give info needed - service just started
            service.attach(this, active);
            List<SchedulePair> schedule = (List<SchedulePair>) getIntent().getSerializableExtra(getString(R.string.scheduleExtra));
            service.loadSchedule(schedule);
            service.startTimer();
        }
    }

    @Override
    public void OnProgressClick(int position) {
        if(position == 0 || active[0] == true || !service.checkActive(position)){
            return;
        }
        for(int i = 0; i < active.length; i++){
           active[i] = false;
        }
        active[position] = true;
        mAdapter.notifyDataSetChanged();
        service.startTimer();
    }

    @Override
    public void multiRun(int position){
        if(position == 0 || active[0] == true){
            return;
        }
        if(active[position]){
            //Minus
            active[position] = false;
        }
        else if(service.checkActive(position)){
            //Plus
            active[position] = true;
        }

        mAdapter.notifyDataSetChanged();
        service.startTimer();
    }

    public void changeAt(int timerIndex) {
        mAdapter.notifyItemChanged(timerIndex);
    }
}
