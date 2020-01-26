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

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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

    private BroadcastReceiver screenReceiver;
    private long screenCloseTime;

    private List<SchedulePair> schedule;
    private SchedulePair currentEvent = null;


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
        millisRemaining[0] = getScheduleTotalTime();
        long now = (new Date()).getTime();
        if(!schedule.isEmpty() && schedule.get(0).getBegin() <= now){
            //end schedule if in one
            setSchedule();
        }
    }

    private long getScheduleTotalTime() {
        //assumes not in range
        long sum = 0;
        for(SchedulePair pair : schedule){
            sum += pair.getEnd() - pair.getBegin();
        }
        return sum;
    }

    private void updateScheduleTotalTime() {
        long sum = 0;
        long now = (new Date()).getTime();
        for(int i = 0; i < schedule.size(); i++){
            if(schedule.get(i).getEnd() < now){
                schedule.remove(i);
                i--;
            }
            else if(schedule.get(i).getBegin() < now){
                sum += schedule.get(i).getEnd() - now;
            }
            else{
                sum += schedule.get(i).getEnd() - schedule.get(i).getBegin();
            }
        }
        millisRemaining[0] = sum;
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
        HashMap<String, Integer> map = (HashMap<String, Integer>) intent.getSerializableExtra(getString(R.string.allocationMapExtra));

        int totalDuration = intent.getIntExtra(getString(R.string.taskSleepLengthExtra), 0);
        millisRemaining = new long[taskCount];
        for(int i = 1; i < taskCount; i++){
            millisRemaining[i] = (long) (totalDuration * ((float) map.get(taskNames[i]))/100);
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
                    screenCloseTime = System.currentTimeMillis() - screenCloseTime;
                    if(active[0] == true){
                        handleSleepWithSchedule(screenCloseTime);
                    }
                    else{
                        handleSleep(screenCloseTime);
                    }
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

        return START_STICKY;
    }

    private void handleSleepWithSchedule(long screenCloseTime) {
        long now = (new Date()).getTime();
        if(now < currentEvent.getEnd()){
            //still in current event
            millisRemaining[0] = getScheduleTotalTime() + (currentEvent.getEnd() - now);
            startTimer();
        }
        else{
            //out of current event
            long timeAfterEventEnd = now - currentEvent.getEnd();
            currentEvent = getCurrentEvent();
            millisRemaining[0] = getScheduleTotalTime();
            int firstAlive = getFirstAlive();
            if(firstAlive == -1){
                //totally done
                finishedTimer();
            }
            else{
                active[firstAlive] = true;
                active[0] = false;
                handleSleep(timeAfterEventEnd);
            }
        }
    }

    private void handleSleep(long sleepLength) {
        long sleepAfterEvent = 0;
        if(!schedule.isEmpty()){
            long now = (new Date()).getTime();
            if(now > schedule.get(0).getBegin()){
                //we entered an event during sleep
                sleepAfterEvent = now - schedule.get(0).getBegin();
                sleepLength = sleepLength - sleepAfterEvent;
                currentEvent = schedule.remove(0);
            }
        }
        long sleepLeft = 0;
        divisions = 0;
        for(int i = 1; i < taskCount; i++){
            if(active[i]){
                divisions++;
            }
        }
        if(divisions == 0){
            return;
        }
        long subtracted = sleepLength/divisions;
        for(int i = 1; i < taskCount; i++){
            if(active[i]){
                if(millisRemaining[i] > subtracted){
                    millisRemaining[i] -= subtracted;
                }
                else{
                    active[i] = false;
                    if(activity != null){
                        activity.changeAt(i);
                        activity.timerTexts[i].setText("Complete");
                    }
                    if(divisions == 1){
                        int firstAlive = getFirstAlive();
                        if(firstAlive != -1){
                            active[firstAlive] = true;
                        }
                        else if(sleepAfterEvent == 0){
                            //totally done
                            timerIndex = i;
                            finishedTimer(); //fixme can probably separate the ending functionality
                            return;
                        }
                    }
                    sleepLeft += subtracted - millisRemaining[i];
                    millisRemaining[i] = 0;
                }
            }
        }
        if(sleepLeft > 0){
            handleSleep(sleepLeft);
        }
        if(sleepAfterEvent > 0){
            setSchedule();
            handleSleepWithSchedule(sleepAfterEvent);
        }
        else{
            startTimer();
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
        active[timerIndex] = false;
        if(timerIndex != 0){
            millisRemaining[timerIndex] = 0;
            if(activity != null){
                activity.timerTexts[timerIndex].setText("Complete");
            }
        }
        if(activity != null){
            activity.changeAt(timerIndex);
        }

        int activeCount = 0;
        int firstAlive = -1;
        for(int i = 1; i < taskCount; i++){
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
        else if(!schedule.isEmpty()){
            updateScheduleTotalTime();
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

    private void setSchedule(){
        active[0] = true;
        for(int i = 1; i < active.length; i++){
            active[i] = false;
        }
        if(activity != null){
            activity.notifyChange();
        }
    }



    public void startTimer(){
        if(countDownTimer != null){
            countDownTimer.cancel();
        }
        if(active[0] == true){
            divisions = 1;
            timerIndex = 0;
            currentEvent = getCurrentEvent();
            Date endDate = new Date(currentEvent.getEnd());
            final String endTime = " (End " + endDate.getHours()%12 + ":" + String.format("%1$02d" , endDate.getMinutes()) + ")";
            countDownTimer = new CountDownTimer((int) (currentEvent.getEnd() - currentEvent.getBegin()), 1000) {
                
                @Override
                public void onTick(long millisUntilFinished) {
                    String notificationText = "";
                    String timeLeft;
                    int hours, minutes, seconds;
                    millisRemaining[0] -= 1000;
                    seconds = (int) (millisRemaining[0] / 1000) % 60 ;
                    minutes = (int) ((millisRemaining[0] / (1000*60)) % 60);
                    hours   = (int) (millisRemaining[0] / (1000*60*60));
                    timeLeft = hours + ":" + String.format("%1$02d" , minutes) + ":" + String.format("%1$02d" , seconds);
                    notificationText += taskNames[0] + endTime + ": " + timeLeft + System.lineSeparator();
                    if(activity != null){
                        activity.timerTexts[0].setText(timeLeft);
                        for(int i = 1; i < taskCount; i++){
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
        else{
            divisions = 0;
            timerIndex = -1;
            for(int i = 1; i < taskCount; i++){
                if(active[i]){
                    divisions++;
                    if(timerIndex == -1 || millisRemaining[timerIndex] < millisRemaining[i]){
                        timerIndex = i;
                    }
                }
            }
            int sSeconds = (int) (millisRemaining[0] / 1000) % 60 ;
            int sMinutes = (int) ((millisRemaining[0] / (1000*60)) % 60);
            int sHours   = (int) (millisRemaining[0] / (1000*60*60));
            final String scheduleString = sHours + ":" + String.format("%1$02d" , sMinutes) + ":" + String.format("%1$02d" , sSeconds);

            countDownTimer = new CountDownTimer((int) millisRemaining[timerIndex] * divisions, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if(schedule != null && !schedule.isEmpty()){
                        long now = (new Date()).getTime();
                        schedule.get(0).getBegin();
                        if(schedule.get(0).getBegin() < now && now < schedule.get(0).getEnd()){
                            setSchedule();
                            startTimer();
                        }
                    }
                    if(activity != null && activity.timerTexts[0] != null){
                        activity.timerTexts[0].setText(scheduleString);
                    }
                    String notificationText = "";
                    String timeLeft;
                    int hours, minutes, seconds;
                    for(int i = 1; i < taskCount; i++){
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

    private SchedulePair getCurrentEvent(){
        long now = (new Date()).getTime();
        for(int i = 0; i < schedule.size(); i++){
            if(schedule.get(i).getEnd() < now){
                schedule.remove(i);
                i--;
            }
            else if(schedule.get(i).getBegin() <= now && now <= schedule.get(i).getEnd()){
                return schedule.get(i);
            }
        }
        return null;
    }
}
