package com.dai.timekeep;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.HashMap;

public class AllocateActivity extends AppCompatActivity {

    private NumberPicker np;
    private SharedPreferences sharedPreferences;
    private int percentChosen;
    private int percentLeft;
    private int totalDuration;
    private int numberOfPercents;
    private String taskName;
    private HashMap<String, Integer> allocation; //Maps taskname to percent

    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allocate);
        if(getIntent().hasExtra(getString(R.string.allocationMapExtra))){
            allocation = (HashMap<String, Integer>) getIntent().getSerializableExtra(getString(R.string.allocationMapExtra));
        }
        else{
            allocation = new HashMap<>();
        }
        Intent intent = getIntent();
        taskName = intent.getStringExtra(getString(R.string.taskNameExtra));
        percentLeft = intent.getIntExtra(getString(R.string.percentLeftExtra), 0);
        totalDuration = intent.getIntExtra(getString(R.string.taskSleepLengthExtra), 0);

        TextView text = findViewById(R.id.taskName);
        text.setText(taskName);

        percentChosen = 5;
        numberOfPercents = percentLeft/5; //why do you scroll down, just reverse the order Android guys
        np = findViewById(R.id.allocater);
        np.setMinValue(1);
        np.setMaxValue(percentLeft/5);
        np.setWrapSelectorWheel(false);
        np.setValue(numberOfPercents);
        np.setOnValueChangedListener(onValueChangeListener);
        np.setFormatter(formatter);

    }

    NumberPicker.OnValueChangeListener onValueChangeListener =
    new NumberPicker.OnValueChangeListener(){
        @Override
        public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
            percentChosen = (numberOfPercents - newVal + 1) * 5;
        }
    };

    NumberPicker.Formatter formatter = new NumberPicker.Formatter(){
        @Override
        public String format(int i) {
            int percent = (numberOfPercents - i + 1) * 5;
            int minutesUsed = (int) (totalDuration / (60*1000) * ((percent / (float) 100)));
            int hours = (int) (minutesUsed)/60;
            int minutes = minutesUsed - hours * 60;
            return Integer.toString(percent) + "% (" + hours + ":" + String.format("%1$02d" , minutes) + ")";
        }
    };

    public void allocate(View view) {
        Intent i1 = new Intent();
        i1.putExtras(getIntent());
        allocation.put(taskName, percentChosen);
        i1.putExtra(getString(R.string.allocationMapExtra), allocation);
        i1.putExtra(getString(R.string.percentLeftExtra), percentLeft - percentChosen);
        if(percentChosen == percentLeft){
            i1.setClass(this, OrderActivity.class);
        }
        else{
            i1.setClass(this, TaskActivity.class);
        }
        startActivity(i1);
    }
}
