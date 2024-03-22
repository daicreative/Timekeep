package com.dai.timekeep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import java.util.HashMap;

public class AllocateActivity extends AppCompatActivity implements AllocateTypeFragment.OnAllocateTypeListener, AllocateWheelFragment.OnAllocateWheelListener {
    //Tabs
    AllocatePagerAdapater allocatePagerAdapater;
    ViewPager viewPager;
    TabLayout tabLayout;

    //Logic
    private NumberPicker np;
    private EditText allocateTyper;
    private SharedPreferences sharedPreferences;
    private float percentChosen;
    private float percentLeft;
    private int totalDuration;
    private int numberOfPercents;
    private String taskName;
    private HashMap<String, Float> allocation; //Maps taskname to percent
    private HashMap<String, Float> originalAllocation; //Maps taskname to percent

    final int PERCENT = 2; // must be divisible by 100

    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        allocation = new HashMap<>(originalAllocation);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allocate);

        //Tabination
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        setPagerAdapter();

        tabLayout = (TabLayout) findViewById((R.id.tab_layout));
        setTabLayout();

        //Logic
        if(getIntent().hasExtra(getString(R.string.allocationMapExtra))){
            originalAllocation = (HashMap<String, Float>) getIntent().getSerializableExtra(getString(R.string.allocationMapExtra));
        }
        else{
            originalAllocation = new HashMap<>();
        }

        allocation = new HashMap<>(originalAllocation);

        Intent intent = getIntent();
        taskName = intent.getStringExtra(getString(R.string.taskNameExtra));
        percentLeft = intent.getFloatExtra(getString(R.string.percentLeftExtra), 0);
        totalDuration = intent.getIntExtra(getString(R.string.taskSleepLengthExtra), 0);
        TextView text = findViewById(R.id.taskName);
        text.setText(taskName);

    }

    NumberPicker.OnValueChangeListener onValueChangeListener =
    new NumberPicker.OnValueChangeListener(){
        @Override
        public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
            percentChosen = newVal == 1 ? percentLeft : (numberOfPercents - newVal + 1) * PERCENT;
        }
    };

    NumberPicker.Formatter formatter = new NumberPicker.Formatter(){
        @Override
        public String format(int i) {
            if(i == 1){
                int minutesUsed = (int) (totalDuration / (60*1000) * ((percentLeft / (float) 100)));
                int hours = (int) (minutesUsed)/60;
                int minutes = minutesUsed - hours * 60;
                return "Max " + "(" + hours + ":" + String.format("%1$02d" , minutes) + ")";
            }
            int percent = (numberOfPercents - i + 1) * PERCENT;
            int minutesUsed = (int) (totalDuration / (60*1000) * ((percent / (float) 100)));
            int hours = (int) (minutesUsed)/60;
            int minutes = minutesUsed - hours * 60;
            return Integer.toString(percent) + "% (" + hours + ":" + String.format("%1$02d" , minutes) + ")";
        }
    };

    private void setPagerAdapter(){
        allocatePagerAdapater = new AllocatePagerAdapater(getSupportFragmentManager());
        viewPager.setAdapter(allocatePagerAdapater);
    }

    private void setTabLayout() {
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setText("Wheel");
        tabLayout.getTabAt(1).setText("Type");
    }

    @Override
    public void onAllocateTypeButton(int duration) {
        float percent = (float) duration / totalDuration;
        percent *= 100;
        if(percent > percentLeft || duration == 0){
            Toast.makeText(getApplicationContext(),"Invalid duration!",Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i1 = new Intent();
        i1.putExtras(getIntent());
        Allocate();
        i1.putExtra(getString(R.string.allocationMapExtra), allocation);
        i1.putExtra(getString(R.string.percentLeftExtra), percentLeft - percent);
        if(percent == percentLeft){
            i1.setClass(this, OrderActivity.class);
        }
        else{
            i1.setClass(this, TaskActivity.class);
        }
        startActivity(i1);
    }

    @Override
    public void onAllocateWheelButton() {
        Intent i1 = new Intent();
        i1.putExtras(getIntent());
        Allocate();
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

    private void Allocate(){

        if(allocation.containsKey(taskName)){
            float percentExisting = allocation.get(taskName);
            allocation.put(taskName, percentExisting + percentChosen);
        }
        else{
            allocation.put(taskName, percentChosen);
        }
    }

    @Override
    public void SetupNumberPicker(NumberPicker np) {
        percentChosen = PERCENT;
        numberOfPercents = (int) Math.ceil((double) percentLeft/PERCENT); //why do you scroll down, just reverse the order Android guys
        np.setMinValue(1);
        np.setMaxValue(numberOfPercents);
        np.setWrapSelectorWheel(false);
        np.setValue(numberOfPercents);
        np.setOnValueChangedListener(onValueChangeListener);
        np.setFormatter(formatter);
    }
}
