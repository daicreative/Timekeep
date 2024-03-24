package com.dai.timekeep;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.fragment.app.Fragment;

public class AllocateWheelFragment extends Fragment
{
	private OnAllocateWheelListener mListener;
	private NumberPicker np;

	public AllocateWheelFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
							 ViewGroup container,
							 Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.allocate_wheel, container, false);

		//Get number picker
		np = v.findViewById(R.id.allocater);
		mListener.SetupNumberPicker(np);

		//Set up Button
		Button button = v.findViewById(R.id.wheelButton);
		button.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				allocateWheelButton(v);
			}
		});
		return v;
	}

	public void allocateWheelButton(View view)
	{
		if (mListener != null)
		{
			mListener.onAllocateWheelButton();
		}
	}

	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
		if (context instanceof OnAllocateWheelListener)
		{
			mListener = (OnAllocateWheelListener) context;
		}
		else
		{
			throw new RuntimeException(context.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		mListener = null;
	}

	public interface OnAllocateWheelListener
	{
		void onAllocateWheelButton();

		void SetupNumberPicker(NumberPicker np);
	}
}
