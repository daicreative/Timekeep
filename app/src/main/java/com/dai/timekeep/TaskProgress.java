package com.dai.timekeep;

public class TaskProgress
{
	public String taskName;
	public long millisRemaining;
	public boolean active;

	public TaskProgress(String taskName, long totalTime)
	{
		this.taskName = taskName;
		this.millisRemaining = totalTime;
		this.active = false;
	}

	public boolean isComplete()
	{
		return millisRemaining <= 0;
	}

	public String getTimeString()
	{
		long seconds = (int) (millisRemaining / 1000) % 60;
		long minutes = (int) ((millisRemaining / (1000 * 60)) % 60);
		long hours = (int) (millisRemaining / (1000 * 60 * 60));
		return hours + ":" + String.format("%1$02d", minutes) + ":" + String.format("%1$02d", seconds);
	}
}
