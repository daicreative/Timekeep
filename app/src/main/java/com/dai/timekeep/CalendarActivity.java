package com.dai.timekeep;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity implements CalendarAdapter.OnCalListener
{

	private RecyclerView recyclerView;
	private CalendarAdapter mAdapter;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;
	private String[] calendarNames;
	private MutableInteger selectedIndex;

	protected void onStart()
	{
		super.onStart();
		overridePendingTransition(R.anim.fadein, R.anim.fadeout);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calendar);
		sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
		editor = sharedPreferences.edit();
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, 1);
		}
		else
		{
			loadCalendars();
			setupList();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		for (int i = 0; i < permissions.length; i++)
		{
			if (permissions[i].compareTo(Manifest.permission.READ_CALENDAR) == 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED)
			{
				loadCalendars();
				setupList();
			}
		}
	}

	private void setupList()
	{
		recyclerView = findViewById(R.id.calendarList);
		recyclerView.setHasFixedSize(true);
		selectedIndex = new MutableInteger(0);
		if (sharedPreferences.contains(getString(R.string.calendarAccountKey)))
		{
			//find cal index with id
			String chosenAccount = sharedPreferences.getString(getString(R.string.calendarAccountKey), "None");
			for (int i = 0; i < calendarNames.length; i++)
			{
				if (calendarNames[i].equals(chosenAccount))
				{
					selectedIndex.value = i;
				}
			}
		}
		mAdapter = new CalendarAdapter(this, calendarNames, selectedIndex, this);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(mAdapter);
	}

	private void loadCalendars()
	{
		Cursor cursor;

		cursor = getContentResolver().query(Uri.parse("content://com.android.calendar/calendars"),
				new String[]{CalendarContract.Calendars.ACCOUNT_NAME}, null, null, null);

		// Get calendars name
		if (cursor.getCount() > 0)
		{
			cursor.moveToFirst();
			Set<String> accounts = new HashSet<>();
			for (int i = 0; i < cursor.getCount(); i++)
			{
				accounts.add(cursor.getString(0));
				cursor.moveToNext();
			}
			calendarNames = new String[accounts.size() + 1];
			calendarNames[0] = "None";
			System.arraycopy(accounts.toArray(), 0, calendarNames, 1, calendarNames.length - 1);
		}
	}

	@Override
	public void OnCalClick(int position)
	{
		if (position == selectedIndex.value)
		{
			return;
		}
		if (position == 0)
		{
			editor.remove(getString(R.string.calendarAccountKey));
			editor.commit();
		}
		else
		{
			editor.putString(getString(R.string.calendarAccountKey), calendarNames[position]);
			editor.commit();
		}
		int old = selectedIndex.value;
		selectedIndex.value = position;
		mAdapter.notifyItemChanged(position);
		mAdapter.notifyItemChanged(old);
	}
}
