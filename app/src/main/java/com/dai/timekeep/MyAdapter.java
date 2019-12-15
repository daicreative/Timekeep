package com.dai.timekeep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>  {

    private List<String> labels;
    private LayoutInflater mInflater;
    private OnTaskListener mOnTaskListener;


    public MyAdapter(Context context, List<String> labels, OnTaskListener onTaskListener) {
        mInflater = LayoutInflater.from(context);
        this.labels = labels;
        this.mOnTaskListener = onTaskListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.task_list_element, parent, false);
        ViewHolder holder = new ViewHolder(view, mOnTaskListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String label = labels.get(position);
        holder.label.setText(label);
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView label;
        OnTaskListener onTaskListener;
        public ViewHolder(@NonNull View itemView, OnTaskListener onTaskListener) {
            super(itemView);
            label = itemView.findViewById(R.id.listElement);
            this.onTaskListener = onTaskListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onTaskListener.OnTaskClick(getAdapterPosition());
        }
    }

    public interface OnTaskListener{
        void OnTaskClick(int position);
    }

}
