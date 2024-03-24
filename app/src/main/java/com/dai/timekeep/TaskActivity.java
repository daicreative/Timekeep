package com.dai.timekeep;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskActivity extends AppCompatActivity implements MyAdapter.OnTaskListener
{

	static Toast taskToast = null;
	private boolean _configuring;
	private RecyclerView recyclerView;
	private MyAdapter mAdapter;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;
	private List<String> tasksList;
	private Set<String> taskSet;
	ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT)
	{
		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target)
		{
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction)
		{
			String removed = tasksList.remove(viewHolder.getAdapterPosition());
			taskSet.remove(removed);
			mAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
			editor.putStringSet(getString(R.string.taskListKey), taskSet);
			editor.commit();
		}
	};
	private HashMap<String, Float> map;

	protected void onStart()
	{
		super.onStart();
		overridePendingTransition(R.anim.fadein, R.anim.fadeout);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_task);
		Intent old = getIntent();
		_configuring = old.getBooleanExtra(getString(R.string.taskBooleanExtra), false);
		if (old.hasExtra(getString(R.string.allocationMapExtra)))
		{
			map = (HashMap<String, Float>) getIntent().getSerializableExtra(getString(R.string.allocationMapExtra));
		}
		sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
		editor = sharedPreferences.edit();
		taskSet = sharedPreferences.getStringSet(getString(R.string.taskListKey), new HashSet<String>());
		tasksList = new ArrayList<>();
		if (taskSet != null)
		{
			tasksList.addAll(taskSet);
		}
		recyclerView = findViewById(R.id.taskList);
		recyclerView.setHasFixedSize(true);
		mAdapter = new MyAdapter(this, tasksList, this);
		new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(mAdapter);
	}

	public void addTask(View view)
	{
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		final EditText inputField = new EditText(this);
		dialogBuilder.setView(inputField);
		dialogBuilder.setMessage("Enter new task name: ")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						String input = inputField.getText().toString();
						if (input.compareTo("") != 0 && !tasksList.contains(input))
						{
							tasksList.add(0, input);
							taskSet.add(input);
							mAdapter.notifyItemInserted(0);
							editor.putStringSet(getString(R.string.taskListKey), taskSet);
							editor.commit();
						}
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				});
		AlertDialog alert = dialogBuilder.create();
		alert.setTitle("Add Task");
		alert.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		alert.show();

	}

	@Override
	public void OnTaskClick(int position)
	{
		if (_configuring)
		{
			String taskName = tasksList.get(position);
			if (map != null && map.containsKey(taskName))
			{
				int percentExisting = map.get(taskName).intValue();
				int totalDuration = getIntent().getIntExtra(getString(R.string.taskSleepLengthExtra), 0);
				int minutesUsed = (int) (totalDuration / (60 * 1000) * ((percentExisting / (float) 100)));
				int hours = (int) (minutesUsed) / 60;
				int minutes = minutesUsed - hours * 60;
				String text = String.format("Already used: %d%% (%d:%02d)", percentExisting, hours, minutes);

				if (taskToast != null)
				{
					taskToast.cancel();
				}

				taskToast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
				taskToast.show();
			}
			Intent i1 = new Intent(this, AllocateActivity.class);
			Intent old = getIntent();
			i1.putExtras(old);
			float percentLeft = old.getFloatExtra(getString(R.string.percentLeftExtra), 100);
			i1.putExtra(getString(R.string.taskNameExtra), taskName);
			i1.putExtra(getString(R.string.percentLeftExtra), percentLeft);
			startActivity(i1);
		}
	}
}
