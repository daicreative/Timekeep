package com.dai.timekeep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class ProgressAdapater extends RecyclerView.Adapter<ProgressAdapater.ViewHolder>  {

    private String[] labels;
    private boolean[] active;
    private TextView[] timerTexts;
    private LayoutInflater mInflater;
    private OnProgressListener mOnProgressListener;
    private int darkColor;
    private int lightColor;


    public ProgressAdapater(Context context, String[] labels, boolean[] active, OnProgressListener mOnProgressListener) {
        mInflater = LayoutInflater.from(context);
        this.labels = labels;
        this.active = active;
        this.darkColor = ContextCompat.getColor(context, R.color.colorPrimary);
        this.lightColor = ContextCompat.getColor(context, R.color.colorPrimaryDark);
        this.mOnProgressListener = mOnProgressListener;
        timerTexts = new TextView[labels.length];
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.progress_list_element, parent, false);
        final ViewHolder holder = new ViewHolder(view, mOnProgressListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String label = labels[position];
        holder.label.setText(label);
        timerTexts[position] = holder.time;
        if(active[position]){
            holder.layout.setBackgroundColor(darkColor);
            holder.label.setTextColor(lightColor);
            holder.time.setTextColor(lightColor);
        }
        else{
            holder.layout.setBackgroundColor(lightColor);
            holder.label.setTextColor(darkColor);
            holder.time.setTextColor(darkColor);
        }
    }

    @Override
    public int getItemCount() {
        return labels.length;
    }

    public TextView[] getTextViews() {
        return timerTexts;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView label;
        TextView time;
        LinearLayout layout;
        OnProgressListener onProgressListener;
        ImageView drag;
        public ViewHolder(@NonNull View itemView, OnProgressListener onProgressListener) {
            super(itemView);
            label = itemView.findViewById(R.id.taskProgressName);
            time = itemView.findViewById((R.id.taskProgressTime));
            layout = itemView.findViewById(R.id.progressElement);
            this.onProgressListener = onProgressListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onProgressListener.OnProgressClick(getAdapterPosition());
        }
    }

    public interface OnProgressListener{
        void OnProgressClick(int position);
    }

}
