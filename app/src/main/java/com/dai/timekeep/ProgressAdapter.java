package com.dai.timekeep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;


public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ViewHolder>
{
	//=========================
	// Members
	//==========================

	private final LayoutInflater mInflater;
	private final ItemTouchHelper mItemTouchHelper;
	private final int darkColor;
	private final int lightColor;

	private final TimerService service;
	private RecyclerView mRecyclerView;


	//=========================
	// Construction
	//==========================

	public ProgressAdapter(Context context, ItemTouchHelper itemTouchHelper, TimerService service)
	{
		mInflater = LayoutInflater.from(context);
		this.darkColor = ContextCompat.getColor(context, R.color.colorPrimary);
		this.lightColor = ContextCompat.getColor(context, R.color.colorPrimaryDark);
		this.mItemTouchHelper = itemTouchHelper;

		this.service = service;
	}

	//=========================
	// Adapter Overrides
	//==========================

	@Override
	public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView)
	{
		super.onAttachedToRecyclerView(recyclerView);

		mRecyclerView = recyclerView;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		View view = mInflater.inflate(R.layout.progress_list_element, parent, false);
		final ViewHolder holder = new ViewHolder(view);

		holder.drag.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{

				// Can't drag schedule

				if (holder.getAdapterPosition() == 0)
					return true;

				// Drag

				if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
				{
					mItemTouchHelper.startDrag(holder);
				}
				return true;
			}
		});

		return holder;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, final int position)
	{
		// Set texts

		TaskProgress taskProgress = service.getProgress(position);

		String label = taskProgress.taskName;
		holder.label.setText(label);

		// Remove drag icon on schedule

		if (holder.getAdapterPosition() == 0)
		{
			holder.drag.setVisibility(View.GONE);
		}

		// Set visual state (for downstream of notifyDataSetChanged calls on this adapter)

		refreshTime();

		TaskProgress taskProgressSchedule = service.getProgress(0);

		if (taskProgressSchedule.active)
		{
			if (position != 0)
			{
				holder.multiRunButtonVisuals(false, false, "X");
			}
			else
			{
				holder.multiRunButtonVisuals(true, false, "*");
			}
		}
		else
		{
			int activeCount = 0;
			for (int i = 1; i < service.getTaskCount(); i++)
			{
				TaskProgress taskProgressIter = service.getProgress(i);

				if (taskProgressIter.active)
				{
					activeCount++;
				}
			}

			if (position == 0)
			{
				holder.multiRunButtonVisuals(false, false, "X");
			}
			else if (taskProgress.active)
			{
				if (activeCount == 1)
				{
					holder.multiRunButtonVisuals(true, false, "*");
				}
				else
				{
					holder.multiRunButtonVisuals(true, true, "-");
				}
			}
			else
			{
				holder.multiRunButtonVisuals(false, true, "+");
			}
		}
	}

	@Override
	public int getItemCount()
	{
		return service.getTaskCount();
	}


	//=========================
	// View Holder (A list element)
	//==========================

	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
	{
		TextView label;
		TextView time;
		LinearLayout layout;
		Button multiRunButton;
		ImageView drag;
		ProgressAdapter adapter;

		public ViewHolder(@NonNull View itemView)
		{
			super(itemView);

			this.adapter = ProgressAdapter.this;

			// Get references

			label = itemView.findViewById(R.id.taskProgressName);
			time = itemView.findViewById(R.id.taskProgressTime);
			layout = itemView.findViewById(R.id.progressElement);
			drag = itemView.findViewById((R.id.dragIcon));
			multiRunButton = itemView.findViewById(R.id.multiRunButton);

			// Set callbacks

			itemView.setOnClickListener(this);

			multiRunButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					onClickMultiRun();
				}
			});
		}

		@Override
		public void onClick(View v)
		{
			// Can't toggle schedule, nor can you switch off of schedule when active

			int position = getAdapterPosition();

			if (position == 0 || service.getProgress(0).active)
				return;

			// Nothing to do if complete

			TaskProgress taskProgress = service.getProgress(position);

			if (taskProgress.isComplete())
				return;

			// Set active

			for (int i = 0; i < service.getTaskCount(); i++)
			{
				TaskProgress taskProgressIter = service.getProgress(i);
				taskProgressIter.active = false;
			}
			taskProgress.active = true;

			adapter.notifyDataSetChanged(); // Refresh visuals
			service.startTimer(); // Re-setup timer
		}

		public void onClickMultiRun()
		{
			// Can't toggle schedule, nor can you switch off of schedule when active

			int position = getAdapterPosition();

			if (position == 0 || service.getProgress(0).active)
				return;

			// Nothing to do if already complete

			TaskProgress taskProgress = service.getProgress(position);

			if (taskProgress.isComplete())
				return;

			// Toggle selection
			// NOTE (imonh) Button clickability prevents this from turning off the only active one

			taskProgress.active = !taskProgress.active;

			adapter.notifyDataSetChanged(); // Refresh visuals
			service.startTimer(); // Re-setup timer
		}

		public void multiRunButtonVisuals(boolean selected, boolean clickable, String symbol)
		{
			if (!selected)
			{
				layout.setBackgroundColor(lightColor);
				label.setTextColor(darkColor);
				time.setTextColor(darkColor);
				drag.setColorFilter(darkColor);
				multiRunButton.setBackgroundResource(R.drawable.border);
				multiRunButton.setTextColor(darkColor);
			}
			else
			{
				layout.setBackgroundColor(darkColor);
				label.setTextColor(lightColor);
				time.setTextColor(lightColor);
				drag.setColorFilter(lightColor);
				multiRunButton.setBackgroundResource(R.drawable.border_white);
				multiRunButton.setTextColor(lightColor);
			}

			multiRunButton.setClickable(clickable);
			multiRunButton.setText(symbol);
		}
	}

	public void refreshTime()
	{
		if (mRecyclerView == null)
			return;

		for (int i = 0; i < service.getTaskCount(); i++)
		{
			ViewHolder holder = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);

			// Could be null if it's been recycled

			if (holder == null) break;

			// Update text

			TaskProgress taskProgress = service.getProgress(i);

			if (taskProgress.isComplete())
			{
				holder.time.setText("Complete");
			}
			else
			{
				holder.time.setText(taskProgress.getTimeString());
			}

		}
	}
}
