package com.dai.timekeep;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.HashMap;

public class TimerService extends Service {

    private final IBinder binder = new TimerBinder();

    private final int NOTIFICATION_ID = 1234;
    private final int NOTIFICATION_ID2 = 1235;


    private int divisions;
    private String[] taskNames;
    private boolean[] active;
    private long[] millisRemaining;
    private int taskCount;
    private int timerIndex;

    private CountDownTimer countDownTimer;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager notificationManager;
    private boolean attached;

    private ProgressActivity activity;



    @Override
    public void onCreate() {
        super.onCreate();
    }

    public String[] getTaskNames() {
        return taskNames;
    }

    public boolean[] getActive() {
        return active;
    }

    public boolean checkActive(int position) {
        return millisRemaining[position] > 0;
    }

    public class TimerBinder extends Binder {
        TimerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TimerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //Get data from intent
        taskNames = intent.getStringArrayExtra(getString(R.string.taskOrderExtra));
        taskCount = taskNames.length;
        active = new boolean[taskCount];
        HashMap<String, Integer> map = (HashMap<String, Integer>) intent.getSerializableExtra(getString(R.string.allocationMapExtra));
        //fixme ACCOUNT FOR CALENDAR HERE INSTEAD
        int totalDuration = intent.getIntExtra(getString(R.string.taskSleepLengthExtra), 0);
        millisRemaining = new long[taskCount];
        for(int i = 0; i < taskCount; i++){
            millisRemaining[i] = (long) (totalDuration * ((float) map.get(taskNames[i]))/100);
        }

        //Set up notification
        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),0, myIntent, 0);
        mBuilder = new NotificationCompat.Builder(getApplicationContext(), "1")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.timer_icon)
                .setContentTitle("Tasks: ")
                .setContentText("Initializing...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notificationManager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1", "Timer", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            startForeground(NOTIFICATION_ID, mBuilder.build());
        }
        else{
            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }

        return START_STICKY;
    }

    private void finishedTimer() {
        millisRemaining[timerIndex] = 0;
        active[timerIndex] = false;
        if(activity != null){
            activity.changeAt(timerIndex);
            activity.timerTexts[timerIndex].setText("Complete");
        }
        int activeCount = 0;
        int firstAlive = -1;
        for(int i = 0; i < taskCount; i++){
            if(active[i]){
                activeCount++;
            }
            if(firstAlive == -1 && millisRemaining[i] > 0){
                firstAlive = i;
            }
        }
        if(activeCount > 0){
            startTimer();
        }
        else if(firstAlive != -1){
            active[firstAlive] = true;
            if(activity != null){
                activity.changeAt(firstAlive);
            }
            startTimer();
        }
        else{
            //Turn cycle off
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.cycleOnKey), false);
            editor.commit();

            //Notify over
            mBuilder.setOngoing(false)
                    .setChannelId("2")
                    .setAutoCancel(true)
                    .setContentTitle("All tasks complete!")
                    .setContentText("Great job!")
                    .setOnlyAlertOnce(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("2", "Finish", NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }
            notificationManager.notify(NOTIFICATION_ID2, mBuilder.build());

            //Kill service
            notificationManager.cancel(NOTIFICATION_ID);
            stopSelf();
        }
    }

    public void startTimer(){
        if(countDownTimer != null){
            countDownTimer.cancel();
        }
        divisions = 1; //fixme
        timerIndex = -1;
        for(int i = 0; i < taskCount; i++){
            if(active[i]){
                //divisions++;
                if(timerIndex == -1 || millisRemaining[timerIndex] < millisRemaining[i]){
                    timerIndex = i;
                }
            }
        }
        countDownTimer = new CountDownTimer((int) millisRemaining[timerIndex] * divisions, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String notificationText = "";
                String timeLeft;
                int hours, minutes, seconds;
                for(int i = 0; i < taskCount; i++){
                    if(active[i]){
                        millisRemaining[i] -= 1000/divisions;
                        seconds = (int) (millisRemaining[i] / 1000) % 60 ;
                        minutes = (int) ((millisRemaining[i] / (1000*60)) % 60);
                        hours   = (int) (millisRemaining[i] / (1000*60*60));
                        timeLeft = hours + ":" + String.format("%1$02d" , minutes) + ":" + String.format("%1$02d" , seconds);
                        notificationText += taskNames[i] + ": " + timeLeft + System.lineSeparator();
                        if(activity != null){
                            activity.timerTexts[i].setText(timeLeft);
                        }
                    }
                    else if(activity != null){
                        if(millisRemaining[i] > 0){
                            seconds = (int) (millisRemaining[i] / 1000) % 60 ;
                            minutes = (int) ((millisRemaining[i] / (1000*60)) % 60);
                            hours   = (int) (millisRemaining[i] / (1000*60*60));
                            timeLeft = hours + ":" + String.format("%1$02d" , minutes) + ":" + String.format("%1$02d" , seconds);
                            activity.timerTexts[i].setText(timeLeft);
                        }
                        else{
                            activity.timerTexts[i].setText("Complete");
                        }
                    }
                }
                mBuilder.setContentText(notificationText);
                notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            }

            @Override
            public void onFinish() {
                finishedTimer();
            }
        };
        countDownTimer.start();
    }

    //Returns index of next timer
    private int getHighestPriority(){
        for(int i = 0; i < taskCount; i++){
            if(millisRemaining[i] != 0){
                return i;
            }
        }
        return -1;
    }

    public void attach(ProgressActivity act, boolean[] activeArr){
        activity = act;
        if(activeArr != null){
            active = activeArr;
        }
        attached = true;
    }

    public boolean beenAttached() {
        return attached;
    }

    public void detach(){
        activity = null;
    }

    public void reset(){
        detach();
        countDownTimer.cancel();
        stopSelf();
    }
}
