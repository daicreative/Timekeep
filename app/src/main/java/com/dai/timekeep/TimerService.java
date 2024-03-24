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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class TimerService extends Service
{
	//=========================
	// Members
	//==========================

	static final String TAG = "TIMER_SERVICE";

	private final IBinder binder = new TimerBinder();

	private final int NOTIFICATION_ID = 1234;
	private final int NOTIFICATION_ID2 = 1235;

	private TaskProgress[] taskProgresses;
	private long finalEndTime;

	private CountDownTimer countDownTimer;
	private NotificationCompat.Builder mBuilder;
	private NotificationManager notificationManager;

	private ProgressActivity activity; // If not null, that means we're attached

	private BroadcastReceiver screenReceiver;
	private long screenCloseTime;

	private List<SchedulePair> schedule = null;
	private long lastTick;


	//=========================
	// Entry Points
	//==========================

	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	@Override
	public void onDestroy()
	{
		if (screenReceiver != null)
		{
			unregisterReceiver(screenReceiver);
			screenReceiver = null;
		}

		super.onDestroy();
	}


	//=========================
	// Service Overrides
	//==========================

	@Override
	public IBinder onBind(Intent intent)
	{
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		//Get data from intent

		schedule = (List<SchedulePair>) intent.getSerializableExtra(getString(R.string.scheduleExtra));
		HashMap<String, Float> map = (HashMap<String, Float>) intent.getSerializableExtra(getString(R.string.allocationMapExtra));
		String[] tempNames = map.keySet().toArray(new String[map.size()]);

		// Set up tasks

		taskProgresses = new TaskProgress[map.size() + 1];

		int totalDuration = intent.getIntExtra(getString(R.string.taskSleepLengthExtra), 0);

		for (int iTask = 0; iTask < taskProgresses.length; iTask++)
		{
			String taskName = iTask == 0 ? "Schedule" : tempNames[iTask - 1];
			long millisRemaining = (iTask == 0) ?
					getScheduleTotalTime() :
					(long) (totalDuration * (map.get(taskName)) / 100);

			taskProgresses[iTask] = new TaskProgress(taskName, millisRemaining);
		}

		// Define the end tick

		SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
		int hour = sharedPreferences.getInt(getString(R.string.sleepHourKey), 0);
		int minute = sharedPreferences.getInt(getString(R.string.sleepMinuteKey), 0);
		SetEndTime(hour, minute);

		//Prep broadcast receiver for screen locking

		screenReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				String strAction = intent.getAction();

				if (strAction.equals(Intent.ACTION_SCREEN_OFF))
				{
					screenCloseTime = System.currentTimeMillis();
					countDownTimer.cancel();
				}
				else if (strAction.equals(Intent.ACTION_SCREEN_ON))
				{
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
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, myIntent, 0);
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			NotificationChannel channel = new NotificationChannel("1", "Timer", NotificationManager.IMPORTANCE_DEFAULT);
			notificationManager.createNotificationChannel(channel);
			startForeground(NOTIFICATION_ID, mBuilder.build());
		}
		else
		{
			notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
		}

		// Start the timer! - Main Entry Point

		startTimer();

		return START_NOT_STICKY;
	}


	//=========================
	// Timer
	//==========================

	// For time guaranteed to not be interrupted by schedule
	private void distributeSleep(long sleepLength)
	{
		int divisions = getDivisionsAndEnsureActives();

		if (divisions == 0)
		{
			// We've recursed such that nothing is selected, so get the first unselected

			TaskProgress taskProgress = getFirstIncomplete();
			if (taskProgress == null) return;

			taskProgress.active = true;
			divisions = 1;
		}

		long subtracted = sleepLength / divisions;
		long leftOver = 0;

		for (int i = 1; i < taskProgresses.length; i++)
		{
			TaskProgress taskProgress = taskProgresses[i];

			if (taskProgress.active)
			{
				if (taskProgress.millisRemaining > subtracted)
				{
					taskProgress.millisRemaining -= subtracted;
				}
				else
				{
					taskProgress.active = false;
					leftOver += subtracted - taskProgress.millisRemaining;
					taskProgress.millisRemaining = 0;
				}
			}
		}

		if (leftOver > 0) distributeSleep(leftOver);
	}

	private void handleSleep(long timeStart, long timeEnd)
	{
		if (!schedule.isEmpty() && schedule.get(0).inSchedule(timeStart))
		{
			// Sleep started during an event
			if (schedule.get(0).inSchedule(timeEnd))
			{
				// We're still in the event - do nothing
				startTimer();
			}
			else
			{
				// Event ended in range
				long nextStart = schedule.get(0).getEnd();
				schedule.remove(0);
				handleSleep(nextStart, timeEnd);
			}
		}
		else if (!schedule.isEmpty() && schedule.get(0).getBegin() < timeEnd)
		{
			// Can assume it started during sleep
			distributeSleep(schedule.get(0).getBegin() - timeStart);
			handleSleep(schedule.get(0).getBegin(), timeEnd);
		}
		else
		{
			// No schedule in range
			distributeSleep(timeEnd - timeStart);
			if (getFirstIncomplete() != null) startTimer();
			else finishedTimer();
		}
	}

	private TaskProgress getFirstIncomplete()
	{
		for (int i = 1; i < taskProgresses.length; i++)
		{
			if (!taskProgresses[i].isComplete())
			{
				return taskProgresses[i];
			}
		}
		return null;
	}

	private void finishedTimer()
	{
		// ALL DONE!

		// Update the times

		if (activity != null)
		{
			for (TaskProgress taskProgress : taskProgresses)
			{
				taskProgress.millisRemaining = 0;
			}

			activity.getProgressAdapter().refreshTime();
		}

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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			NotificationChannel channel = new NotificationChannel("2", "Finish", NotificationManager.IMPORTANCE_HIGH);
			notificationManager.createNotificationChannel(channel);
		}
		notificationManager.notify(NOTIFICATION_ID2, mBuilder.build());

		//Kill service
		notificationManager.cancel(NOTIFICATION_ID);
		stopSelf();
	}

	private void setScheduleToActive()
	{
		taskProgresses[0].active = true;

		for (int i = 1; i < taskProgresses.length; i++)
		{
			taskProgresses[i].active = false;
		}
	}

	private long getScheduleTotalTime()
	{
		// assumes no overlap
		long sum = 0;
		long now = System.currentTimeMillis();
		for (SchedulePair pair : schedule)
		{
			sum += pair.getEnd() - Math.max(pair.getBegin(), now);
		}
		return sum;
	}

	public void startTimer()
	{
		if (countDownTimer != null)
		{
			countDownTimer.cancel();
		}

		// Schedule check
		long now = System.currentTimeMillis();
		RemovePastEvents(now);

		TaskProgress taskProgressSchedule = taskProgresses[0];
		taskProgressSchedule.millisRemaining = getScheduleTotalTime();

		if (!schedule.isEmpty() && schedule.get(0).inSchedule(now))
		{
			setScheduleToActive();
		}
		else
		{
			taskProgressSchedule.active = false;
		}

		if (taskProgressSchedule.active)
		{
			int divisions = 1;

			if (schedule.isEmpty())
			{
				// Error check
				startTimer();
				return;
			}

			Date endDate = new Date(schedule.get(0).getEnd());
			final String endTime = " (End " + endDate.getHours() % 12 + ":" + String.format("%1$02d", endDate.getMinutes()) + ")";
			lastTick = finalEndTime - System.currentTimeMillis();
			countDownTimer = new CountDownTimer((int) lastTick, 1000)
			{
				@Override
				public void onTick(long millisUntilFinished)
				{
					long timeElapsed = 1000;
					String notificationText = "";
					String timeLeft;

					TaskProgress taskProgressSchedule = taskProgresses[0];
					taskProgressSchedule.millisRemaining = Math.max(0, taskProgressSchedule.millisRemaining - timeElapsed);

					if (schedule.get(0).getEnd() < System.currentTimeMillis())
					{
						startTimer();
						return;
					}

					timeLeft = taskProgressSchedule.getTimeString();
					notificationText += taskProgressSchedule.taskName + endTime + ": " + timeLeft + System.lineSeparator();

					// Update time text on activity

					if (activity != null)
					{
						activity.getProgressAdapter().refreshTime();
					}

					mBuilder.setContentText(notificationText);
					notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
				}

				@Override
				public void onFinish()
				{
					finishedTimer();
				}
			};
			countDownTimer.start();
		}
		else
		{
			int divisions = getDivisionsAndEnsureActives();
			if (divisions == 0)
			{
				TaskProgress alive = getFirstIncomplete();
				if (alive == null)
				{
					// This can happen if the timer finish doesn't occur first
					finishedTimer();
					return;
				}

				alive.active = true;
			}

			lastTick = finalEndTime - System.currentTimeMillis();
			countDownTimer = new CountDownTimer(lastTick, 1000)
			{
				@Override
				public void onTick(long millisUntilFinished)
				{
					TaskProgress taskProgressSchedule = taskProgresses[0];
					final String scheduleString = taskProgressSchedule.getTimeString();

					if (schedule != null && !schedule.isEmpty())
					{
						if (schedule.get(0).getBegin() < System.currentTimeMillis())
						{
							setScheduleToActive();
							startTimer();
						}
					}

					long timeElapsed = 1000;
					String notificationText = "";
					int divisions = getDivisionsAndEnsureActives();
					for (int i = 1; i < taskProgresses.length; i++)
					{
						TaskProgress taskProgress = taskProgresses[i];

						if (taskProgress.active)
						{
							taskProgress.millisRemaining = Math.max(0, taskProgress.millisRemaining - timeElapsed / divisions);

							if (taskProgress.isComplete())
							{
								startTimer();
								return;
							}

							String timeLeft = taskProgress.getTimeString();
							notificationText += taskProgress.taskName + ": " + timeLeft + System.lineSeparator();
						}
					}

					// Update time text on activity

					if (activity != null)
					{
						activity.getProgressAdapter().refreshTime();
					}

					mBuilder.setContentText(notificationText);
					notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
				}

				@Override
				public void onFinish()
				{
					finishedTimer();
				}
			};
			countDownTimer.start();
		}
	}

	private void RemovePastEvents(long time)
	{
		for (int i = 0; i < schedule.size(); i++)
		{
			if (schedule.get(i).getEnd() < time)
			{
				schedule.remove(i);
				i--;
			}
		}
	}

	private int getDivisionsAndEnsureActives()
	{
		int divisions = 0;
		for (int i = 1; i < taskProgresses.length; i++)
		{
			TaskProgress taskProgress = taskProgresses[i];
			if (taskProgress.isComplete())
			{
				taskProgress.active = false;
			}
			else if (taskProgress.active)
			{
				divisions++;
			}
		}
		return divisions;
	}

	public void SetEndTime(int hour, int minute)
	{
		Date now = new Date();
		Date future = new Date(now.getTime());
		future.setHours(hour);
		future.setMinutes(minute);
		future.setSeconds(0);
		if (future.getTime() < now.getTime())
		{
			Calendar c = Calendar.getInstance();
			c.setTime(future);
			c.add(Calendar.DATE, 1);
			future = c.getTime();
		}
		finalEndTime = future.getTime();
	}

	public void swapOrder(int dragPosition, int targetPosition)
	{
		TaskProgress temp = taskProgresses[dragPosition];
		taskProgresses[dragPosition] = taskProgresses[targetPosition];
		taskProgresses[targetPosition] = temp;
	}


	//=========================
	// Activity Handling
	//==========================

	public void attachActivity(ProgressActivity act)
	{
		activity = act;
	}

	public void detach()
	{
		activity = null;
	}

	public boolean hasBeenInitialized()
	{
		return schedule != null;
	}

	public void reset()
	{
		detach();
		countDownTimer.cancel();
		stopSelf();
	}

	public class TimerBinder extends Binder
	{
		TimerService getService()
		{
			// Return this instance of LocalService so clients can call public methods
			return TimerService.this;
		}
	}

	public TaskProgress getProgress(int index)
	{
		return taskProgresses[index];
	}

	public int getTaskCount()
	{
		return taskProgresses.length;
	}
}
