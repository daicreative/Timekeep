package com.dai.timekeep;

import java.io.Serializable;

public class SchedulePair implements Serializable
{
	private final long begin;
	private final long end;

	public SchedulePair(long begin, long end)
	{
		this.begin = begin;
		this.end = end;
	}

	public boolean inSchedule(long time)
	{
		return time >= begin && time < end;
	}

	public long getBegin()
	{
		return begin;
	}

	public long getEnd()
	{
		return end;
	}
}
