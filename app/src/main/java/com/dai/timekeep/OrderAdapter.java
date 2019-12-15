package com.dai.timekeep;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder>  {

    private String[] labels;
    private LayoutInflater mInflater;
    private OnOrderListener mOnOrderListener;
    private ItemTouchHelper mItemTouchHelper;


    public OrderAdapter(Context context, String[] labels, OnOrderListener onOrderListener, ItemTouchHelper itemTouchHelper) {
        mInflater = LayoutInflater.from(context);
        this.labels = labels;
        this.mOnOrderListener = onOrderListener;
        this.mItemTouchHelper = itemTouchHelper;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.order_list_element, parent, false);
        final ViewHolder holder = new ViewHolder(view, mOnOrderListener);
        holder.drag.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN){
                    mItemTouchHelper.startDrag(holder);
                }
                return true;
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String label = labels[position];
        holder.label.setText(label);


    }

    @Override
    public int getItemCount() {
        return labels.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView label;
        OnOrderListener onOrderListener;
        ImageView drag;
        public ViewHolder(@NonNull View itemView, OnOrderListener onOrderListener) {
            super(itemView);
            label = itemView.findViewById(R.id.orderElement);
            drag = itemView.findViewById((R.id.dragIcon));
            this.onOrderListener = onOrderListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onOrderListener.OnOrderClick(getAdapterPosition());
        }
    }

    public interface OnOrderListener{
        void OnOrderClick(int position);
    }

}
