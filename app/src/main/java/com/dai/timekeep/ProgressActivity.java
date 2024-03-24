package com.dai.timekeep;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;

public class ProgressActivity extends AppCompatActivity
{
	//=========================
	// Members
	//==========================

	private ProgressAdapter mAdapter;
	private SharedPreferences sharedPreferences;
	private TimerService service = null;

	private boolean mBound = false;


	//=========================
	// Entry Points
	//==========================

	@Override
	protected void onStart()
	{
		super.onStart();
		overridePendingTransition(R.anim.fadein, R.anim.fadeout);
		Intent intent = new Intent(this, TimerService.class);
		bindService(intent, connection, 0);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		if (mBound)
		{
			service.detach();
		}
		unbindService(connection);
		mBound = false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_progress);

		//Check if service is running
		sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
		if (sharedPreferences.getBoolean(getString(R.string.cycleOnKey), false))
		{
			return;
		}

		//Set cycle on because it wasn't yet
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(getString(R.string.cycleOnKey), true);
		editor.commit();

		//Get data from intent
		Intent old = getIntent();

		HashMap<String, Float> map = (HashMap<String, Float>) old.getSerializableExtra(getString(R.string.allocationMapExtra));
		String[] tempNames = map.keySet().toArray(new String[map.size()]);

		//Set up service

		Intent serviceIntent = new Intent(this, TimerService.class);
		serviceIntent.putExtras(old);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			startForegroundService(serviceIntent);
		}
		else
		{
			startService(serviceIntent);
		}
	}

	public void reset(View view)
	{
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(getString(R.string.cycleOnKey), false);
		editor.commit();
		if (mBound)
		{
			service.reset();
		}
		Intent i1 = new Intent(this, MainActivity.class);
		i1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i1);
	}


	//=========================
	// Service Connection (defines callbacks for service binding, passed to bindService())
	//==========================

	private final ServiceConnection connection = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName className,
									   IBinder iBinder)
		{
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			service = ((TimerService.TimerBinder) iBinder).getService();
			bindToService(service);
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			mBound = false;
		}
	};

	private void bindToService(TimerService service)
	{
		recyclerSetup();
		service.attachActivity(this);
	}

	private void recyclerSetup()
	{
		//Set up recycler

		RecyclerView recyclerView = findViewById(R.id.orderList);
		recyclerView.setHasFixedSize(true);

		ItemTouchHelper ith = new ItemTouchHelper(itemTouchHelperCallback);
		ith.attachToRecyclerView(recyclerView);

		mAdapter = new ProgressAdapter(this, ith, service);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(mAdapter);
	}

	ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0)
	{
		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target)
		{
			int dragPosition = viewHolder.getAdapterPosition();
			int targetPosition = target.getAdapterPosition();

			// Can't move Schedule

			if (dragPosition == 0)
				return false;

			// Can't move above Schedule

			targetPosition = Math.max(1, targetPosition);

			// Swap

			if (dragPosition == targetPosition)
				return false;

			mAdapter.notifyItemMoved(dragPosition, targetPosition);

			// Swap order in service so we'll waterfall properly on timer complete

			service.swapOrder(dragPosition, targetPosition);
			return false;
		}

		@Override
		public boolean isLongPressDragEnabled()
		{
			return true;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction)
		{
		}
	};

	//=========================
	// Getters
	//==========================

	public ProgressAdapter getProgressAdapter()
	{
		return mAdapter;
	}
}
