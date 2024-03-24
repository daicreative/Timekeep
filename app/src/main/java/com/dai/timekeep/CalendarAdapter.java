package com.dai.timekeep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder>
{

	private final String[] labels;
	private final MutableInteger selected;
	private final LayoutInflater mInflater;
	private final OnCalListener mOnCalListener;


	public CalendarAdapter(Context context, String[] labels, MutableInteger selected, OnCalListener onCalListener)
	{
		mInflater = LayoutInflater.from(context);
		this.labels = labels;
		this.mOnCalListener = onCalListener;
		this.selected = selected;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		View view = mInflater.inflate(R.layout.calendar_list_element, parent, false);
		ViewHolder holder = new ViewHolder(view, mOnCalListener);
		return holder;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position)
	{
		String label = labels[position];
		holder.label.setText(label);
		holder.button.setChecked(position == selected.value);
	}

	@Override
	public int getItemCount()
	{
		return labels.length;
	}

	public interface OnCalListener
	{
		void OnCalClick(int position);
	}

	public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
	{

		TextView label;
		RadioButton button;
		OnCalListener onCalListener;

		public ViewHolder(@NonNull View itemView, OnCalListener onCalListener)
		{
			super(itemView);
			label = itemView.findViewById(R.id.calendarElement);
			button = itemView.findViewById(R.id.calRadio);
			this.onCalListener = onCalListener;
			itemView.setOnClickListener(this);
		}

		@Override
		public void onClick(View v)
		{
			onCalListener.OnCalClick(getAdapterPosition());
		}
	}

}
