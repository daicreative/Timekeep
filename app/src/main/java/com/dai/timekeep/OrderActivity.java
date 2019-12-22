package com.dai.timekeep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

//Add FAB button for next
//Finish layout so can swap
public class OrderActivity extends AppCompatActivity implements OrderAdapter.OnOrderListener {

    private RecyclerView recyclerView;
    private OrderAdapter mAdapter;
    private SharedPreferences sharedPreferences;
    private String[] taskNames;

    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        sharedPreferences = getSharedPreferences(getString(R.string.sharedPrefs), MODE_PRIVATE);
        HashMap<String, Integer> map = (HashMap<String, Integer>) getIntent().getSerializableExtra(getString(R.string.allocationMapExtra));
        taskNames = new String[map.size()];
        map.keySet().toArray(taskNames);
        recyclerView = findViewById(R.id.orderList);
        recyclerView.setHasFixedSize(true);
        ItemTouchHelper ith = new ItemTouchHelper(itemTouchHelperCallback);
        mAdapter = new OrderAdapter(this, taskNames, this, ith);
        ith.attachToRecyclerView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
    }

    public void doneRearranging(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WAKE_LOCK}, 1);
        }
        else{
            Intent i1 = new Intent(this, ProgressActivity.class);
            i1.putExtras(getIntent());
            i1.putExtra(getString(R.string.taskOrderExtra), taskNames);
            //start up notifications
            i1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i = 0; i < permissions.length; i++){
            if(permissions[i].compareTo(Manifest.permission.WAKE_LOCK) == 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED){
                doneRearranging(null);
            }
        }
    }

    @Override
    public void OnOrderClick(int position) {
        return;
    }

    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int dragPosition = viewHolder.getAdapterPosition();
            int targetPosition = target.getAdapterPosition();
            String temp = taskNames[dragPosition];
            taskNames[dragPosition] = taskNames[targetPosition];
            taskNames[targetPosition] = temp;
            mAdapter.notifyItemMoved(dragPosition, targetPosition);
            return false;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            return;
        }
    };
}
