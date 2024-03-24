package com.dai.timekeep;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

public class AllocateTypeFragment extends Fragment
{
	private OnAllocateTypeListener mListener;
	private EditText hourEdit;
	private EditText minuteEdit;

	public AllocateTypeFragment()
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
		View v = inflater.inflate(R.layout.allocate_type, container, false);

		//Set up formatting for the timer text
		hourEdit = (EditText) v.findViewById(R.id.hourEdit);
		hourEdit.setSelection(hourEdit.getText().length());
		hourEdit.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				hourEdit.setSelection(hourEdit.getText().length());
			}
		});
		hourEdit.addTextChangedListener(new TextWatcher()
		{
			private boolean mSelfChange = false;

			@Override
			public void afterTextChanged(Editable s)
			{
				if (mSelfChange) return;

				String edit = s.toString();
				int hour = Integer.parseInt(edit);
				if (edit.length() == 1)
				{
					mSelfChange = true;
					s.clear();
					s.append("0" + hour);
					mSelfChange = false;
					return;
				}
				else if (edit.length() > 2)
				{
					edit = edit.substring(edit.length() - 2);
					hour = Integer.parseInt(edit);
					mSelfChange = true;
					s.clear();
					s.append(edit);
					mSelfChange = false;
				}
				if (hour > 24)
				{
					mSelfChange = true;
					s.clear();
					s.append("24");
					mSelfChange = false;
				}

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{

			}
		});

		minuteEdit = (EditText) v.findViewById(R.id.minuteEdit);
		minuteEdit.setSelection(minuteEdit.getText().length());
		minuteEdit.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				minuteEdit.setSelection(minuteEdit.getText().length());
			}
		});
		minuteEdit.addTextChangedListener(new TextWatcher()
		{
			private boolean mSelfChange = false;

			@Override
			public void afterTextChanged(Editable s)
			{
				if (mSelfChange) return;
				String edit = s.toString();
				int minute = Integer.parseInt(edit);
				if (edit.length() == 1)
				{
					mSelfChange = true;
					s.clear();
					s.append("0" + minute);
					mSelfChange = false;
					return;
				}
				else if (edit.length() > 2)
				{
					edit = edit.substring(edit.length() - 2);
					minute = Integer.parseInt(edit);
					mSelfChange = true;
					s.clear();
					s.append(edit);
					mSelfChange = false;
				}
				if (minute > 59)
				{
					mSelfChange = true;
					s.clear();
					s.append("59");
					mSelfChange = false;
				}

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{

			}
		});

		//Set up Button
		Button button = v.findViewById(R.id.typeButton);
		button.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				allocateTypeButton(v);
			}
		});
		return v;
	}

	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
		if (context instanceof OnAllocateTypeListener)
		{
			mListener = (OnAllocateTypeListener) context;
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

	public void allocateTypeButton(View view)
	{
		if (mListener != null)
		{
			int hour = Integer.parseInt(hourEdit.getText().toString());
			int minute = Integer.parseInt(minuteEdit.getText().toString());
			mListener.onAllocateTypeButton((hour * 60 + minute) * 60 * 1000);
		}
	}

	public interface OnAllocateTypeListener
	{
		void onAllocateTypeButton(int duration);
	}

}

