package com.dai.timekeep;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.text.Normalizer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class TimerService extends Service {

    private final IBinder binder = new TimerBinder();

    private final int NOTIFICATION_ID = 1234;
    private final int NOTIFICATION_ID2 = 1235;


    private String[] taskNames;
    private boolean[] active;
    private long[] millisRemaining;
    private int taskCount;
    private long finalEndTime;

    private CountDownTimer countDownTimer;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager notificationManager;
    private boolean attached;

    private ProgressActivity activity;

    private BroadcastReceiver screenReceiver;
    private long screenCloseTime;

    private List<SchedulePair> schedule;
    private long lastTick;

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

    public void loadSchedule(List<SchedulePair> schedule) {
        this.schedule = schedule;
    }

    private long getScheduleTotalTime() {
        //assumes no overlap
        long sum = 0;
        long now = System.currentTimeMillis();
        for(SchedulePair pair : schedule){
            sum += pair.getEnd() - Math.max(pair.getBegin(), now);
        }
        return sum;
    }

    public class TimerBinder extends Binder {
        TimerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TimerService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
            screenReceiver = null;
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
        String[] tempNames = intent.getStringArrayExtra(getString(R.string.taskOrderExtra));
        taskNames = new String[tempNames.length + 1];
        taskNames[0] = "Schedule";

        System.arraycopy(tempNames, 0, taskNames, 1, tempNames.length);
        taskCount = taskNames.length;
        active = new boolean[taskCount];
        HashMap<String, Float> map = (HashMap<String, Float>) intent.getSerializableExtra(getString(R.string.allocationMapExtra));

        int totalDuration = intent.getIntExtra(getString(R.string.taskSleepLengthExtra), 0);
        millisRemaining = new long[taskCount];
        for(int i = 1; i < taskCount; i++){
            millisRemaining[i] = (long) (totalDuration * (map.get(taskNames[i]))/100);
        }

        //Prep broadcast receiver for screen locking
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String strAction = intent.getAction();

                if (strAction.equals(Intent.ACTION_SCREEN_OFF)){
                    screenCloseTime = System.currentTimeMillis();
                    countDownTimer.cancel();
                }
                else if(strAction.equals(Intent.ACTION_SCREEN_ON)){
                    handleSleep(screenCloseTime, System.currentTimeMillis());
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);

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

        return START_NOT_STICKY;
    }

    // For time guaranteed to not be interrupted by schedule
    private void distributeSleep(long sleepLength) {
        int divisions = GetDivisions();
        if(divisions == 0){
            int alive = getFirstAlive();
            if(alive == -1) return;
            active[getFirstAlive()] = true;
            divisions = 1;
        }
        long subtracted = sleepLength/divisions;
        for(int i = 1; i < taskCount; i++){
            if(active[i]){
                if(millisRemaining[i] > subtracted){
                    millisRemaining[i] -= subtracted;
                }
                else{
                    active[i] = false;
                    long sleepLeft = subtracted - millisRemaining[i];
                    millisRemaining[i] = 0;
                    distributeSleep(sleepLeft);
                }
            }
        }
    }

    private void handleSleep(long timeStart, long timeEnd) {

        if(!schedule.isEmpty() && schedule.get(0).inSchedule(timeStart)){
            // Sleep started during an event
            if(schedule.get(0).inSchedule(timeEnd)){
                // We're still in the event - do nothing
                startTimer();
            }
            else{
                // Event ended in range
                long nextStart = schedule.get(0).getEnd();
                schedule.remove(0);
                handleSleep(nextStart, timeEnd);
            }
        }
        else if(!schedule.isEmpty() && schedule.get(0).getBegin() < timeEnd){
            // Can assume it started during sleep
            distributeSleep(schedule.get(0).getBegin() - timeStart);
            handleSleep(schedule.get(0).getBegin(), timeEnd);
        }
        else{
            // No schedule in range
            distributeSleep(timeEnd - timeStart);
            if(getFirstAlive() != -1) startTimer();
            else finishedTimer();
        }
    }

    private int getFirstAlive(){
        for(int i = 1; i < taskCount; i++){
            if(millisRemaining[i] > 0){
                return i;
            }
        }
        return -1;
    }

    private void finishedTimer() {
        // ALL DONE!
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

    private void setSchedule(){
        active[0] = true;
        for(int i = 1; i < active.length; i++){
            active[i] = false;
        }
    }

    public void startTimer(){
        if(countDownTimer != null){
            countDownTimer.cancel();
        }

        // Schedule check
        long now = System.currentTimeMillis();
        RemovePastEvents(now);
        millisRemaining[0] = getScheduleTotalTime();
        if(!schedule.isEmpty() && schedule.get(0).inSchedule(now)){
            setSchedule();
        }
        else{
            active[0] = false;
        }

        if(active[0] == true){
            int divisions = 1;

            if(schedule.isEmpty()){
                // Error check
                startTimer();
                return;
            }

            Date endDate = new Date(schedule.get(0).getEnd());
            final String endTime = " (End " + endDate.getHours()%12 + ":" + String.format("%1$02d" , endDate.getMinutes()) + ")";
            lastTick = finalEndTime - System.currentTimeMillis();
            countDownTimer = new CountDownTimer((int) lastTick, 1000) {
                
                @Override
                public void onTick(long millisUntilFinished) {
                 /*   long timeElapsed = lastTick - millisUntilFinished;
                    lastTick = millisUntilFinished;*/
                    long timeElapsed = 1000;
                    String notificationText = "";
                    String timeLeft;
                    int hours, minutes, seconds;
                    millisRemaining[0] = Math.max(0, millisRemaining[0] - timeElapsed);

                    if(schedule.get(0).getEnd() < System.currentTimeMillis()){
                        startTimer();
                        return;
                    }

                    timeLeft = FormatedTime(millisRemaining[0]);
                    notificationText += taskNames[0] + endTime + ": " + timeLeft + System.lineSeparator();
                    if(activity != null){
                        if(activity.timerTexts[0] != null) activity.timerTexts[0].setText(timeLeft);
                        for(int i = 1; i < taskCount; i++){
                            if(activity.timerTexts[i] == null) break;
                            if(millisRemaining[i] > 0){
                                timeLeft = FormatedTime(millisRemaining[i]);
                                activity.timerTexts[i].setText(timeLeft);
                            }
                            else{
                                activity.timerTexts[i].setText("Complete");
                            }
                        }
                    }
                    if(activity != null) activity.notifyChange(); // fixme: might be expensive
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
        else{
            int divisions = GetDivisions();
            if(divisions == 0){
                int alive = getFirstAlive();
                if(alive == -1){
                    // This can happen if the timer finish doesn't occur first
                    finishedTimer();
                    return;
                }
                active[alive] = true;
                divisions = 1;
            }

            long min = Long.MAX_VALUE;
            for(int i = 1; i < taskCount; i++){
                if(active[i] && millisRemaining[i] < min) min = millisRemaining[i];
            }

            final String scheduleString = FormatedTime(millisRemaining[0]);
            lastTick = finalEndTime - System.currentTimeMillis();
            countDownTimer = new CountDownTimer(lastTick, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if(schedule != null && !schedule.isEmpty()){
                        if(schedule.get(0).getBegin() < System.currentTimeMillis()){
                            setSchedule();
                            startTimer();
                        }
                    }
                    if(activity != null && activity.timerTexts[0] != null){
                        activity.timerTexts[0].setText(scheduleString);
                    }

                    long timeElapsed = 1000;
                    String notificationText = "";
                    int divisions = GetDivisions();
                    for(int i = 1; i < taskCount; i++){
                        if(active[i]){
                            millisRemaining[i] = Math.max(0, millisRemaining[i] - timeElapsed/divisions);

                            if(millisRemaining[i] == 0){
                                startTimer();
                                return;
                            }

                            String timeLeft = FormatedTime(millisRemaining[i]);
                            notificationText += taskNames[i] + ": " + timeLeft + System.lineSeparator();
                            if(activity != null && activity.timerTexts[i] != null){
                                activity.timerTexts[i].setText(timeLeft);
                            }
                        }
                        else if(activity != null && activity.timerTexts[i] != null){
                            if(millisRemaining[i] > 0){
                                activity.timerTexts[i].setText(FormatedTime(millisRemaining[i]));
                            }
                            else{
                                activity.timerTexts[i].setText("Complete");
                            }
                        }
                    }

                    //if(activity != null) activity.notifyChange(); // fixme: might be expensive
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
    }

    public void attachActive(boolean[] activeArr){
        active = activeArr;
    }

    public void attachActivity(ProgressActivity act){
        activity = act;
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

    private void RemovePastEvents(long time){
        for(int i = 0; i < schedule.size(); i++){
            if(schedule.get(i).getEnd() < time){
                schedule.remove(i);
                i--;
            }
        }
    }

    private int GetDivisions(){
        int divisions = 0;
        for(int i = 1; i < taskCount; i++){
            if(active[i]){
                divisions++;
            }
        }
        return divisions;
    }

    public void SetEndTime(int hour, int minute){
        Date now = new Date();
        Date future = new Date(now.getTime());
        future.setHours(hour);
        future.setMinutes(minute);
        future.setSeconds(0);
        if(future.getTime() < now.getTime()){
            Calendar c = Calendar.getInstance();
            c.setTime(future);
            c.add(Calendar.DATE, 1);
            future = c.getTime();
        }
        finalEndTime = future.getTime();
    }

    private String FormatedTime(long timeRemaining){
        long seconds = (int) (timeRemaining / 1000) % 60 ;
        long minutes = (int) ((timeRemaining / (1000*60)) % 60);
        long hours   = (int) (timeRemaining / (1000*60*60));
        return hours + ":" + String.format("%1$02d" , minutes) + ":" + String.format("%1$02d" , seconds);
    }
}
