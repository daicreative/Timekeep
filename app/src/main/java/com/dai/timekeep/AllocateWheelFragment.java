package com.dai.timekeep;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

public class AllocateWheelFragment extends Fragment {
    public AllocateWheelFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.allocate_wheel, container, false);
    }

    public NumberPicker GetNumberPicker(){
        return getView().findViewById(R.id.allocater);
    }
}
