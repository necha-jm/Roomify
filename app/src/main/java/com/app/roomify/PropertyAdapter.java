package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.MyViewHolder> {

   private ArrayList<Room> requests;

    public PropertyAdapter(ArrayList<Room> requests) {
        this.requests = requests;
    }

    @NonNull
    @Override
    public PropertyAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.owner_property_item,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyAdapter.MyViewHolder holder, int position) {

        Room room = requests.get(position);
        holder.tvPropertyTitle.setText(room.getTitle());
        holder.tvPrice.setText("TZS " + room.getPrice());

    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public void setRooms(List<Room> propertyList) {
        requests.clear();
        requests.addAll(propertyList);
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvPropertyTitle,tvLocation,tvPrice,tvBookingsCount;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            tvBookingsCount = itemView.findViewById(R.id.tvBookingsCount);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPropertyTitle = itemView.findViewById(R.id.tvPropertyTitle);



        }
    }
}
