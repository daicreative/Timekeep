package com.dai.timekeep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class AllocateTypeFragment extends Fragment {
    public AllocateTypeFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.allocate_type, container, false);
        final EditText editText = (EditText)v.findViewById(R.id.hourEdit);
        editText.setSelection(editText.getText().length());
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setSelection(editText.getText().length());
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            private boolean mSelfChange = false;

            @Override
            public void afterTextChanged(Editable s) {
                if(mSelfChange) return;

                String edit = s.toString();
                int hour = Integer.parseInt(edit);
                if(edit.length() == 1){
                    mSelfChange = true;
                    s.clear();
                    s.append("0" + Integer.toString(hour));
                    mSelfChange = false;
                    return;
                }
                else if(edit.length() > 2){
                    edit = edit.substring(edit.length() - 2, edit.length());
                    hour = Integer.parseInt(edit);
                    mSelfChange = true;
                    s.clear();
                    s.append(edit);
                    mSelfChange = false;
                }
                if(hour > 24){
                    mSelfChange = true;
                    s.clear();
                    s.append("24");
                    mSelfChange = false;
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        final EditText editText2 = (EditText)v.findViewById(R.id.minuteEdit);
        editText2.setSelection(editText2.getText().length());
        editText2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText2.setSelection(editText2.getText().length());
            }
        });
        editText2.addTextChangedListener(new TextWatcher() {
            private boolean mSelfChange = false;

            @Override
            public void afterTextChanged(Editable s) {
                if(mSelfChange) return;

                String edit = s.toString();
                int minute = Integer.parseInt(edit);
                if(edit.length() == 1){
                    mSelfChange = true;
                    s.clear();
                    s.append("0" + Integer.toString(minute));
                    mSelfChange = false;
                    return;
                }
                else if(edit.length() > 2){
                    edit = edit.substring(edit.length() - 2, edit.length());
                    minute = Integer.parseInt(edit);
                    mSelfChange = true;
                    s.clear();
                    s.append(edit);
                    mSelfChange = false;
                }
                if(minute > 59){
                    mSelfChange = true;
                    s.clear();
                    s.append("59");
                    mSelfChange = false;
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }
}

