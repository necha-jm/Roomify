package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.MyViewHolder> {
   private final ArrayList<Room> RoomList;

    public RoomAdapter(ArrayList<Room> roomList) {
        RoomList = roomList;
    }

    @NonNull
    @Override
    public RoomAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tenant_dashboard_recommendation_item,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomAdapter.MyViewHolder holder, int position) {
        Room room = RoomList.get(position);
        holder.tvPrice.setText(String.valueOf(room.getPrice()));
        holder.tvLocation.setText(room.getAddress());
        holder.tvRoomTitle.setText(room.getTitle());

        List<String> images = room.getImages();
        if(images != null && !images.isEmpty()){
            Glide.with(holder.ivRoomImage.getContext())
                    .load(images.get(0))
                    .placeholder(R.drawable.ic_back)
                    .error(R.drawable.ic_back)
                    .into(holder.ivRoomImage);
        }else{
            holder.ivRoomImage.setImageResource(R.drawable.ic_back);
        }


    }

    @Override
    public int getItemCount() {
        return RoomList.size();
    }

    public void setRooms(List<Room> rooms) {
        RoomList.clear();          // clear old data
        RoomList.addAll(rooms);    // add new data
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomTitle,tvLocation,tvPrice;
        ImageView ivRoomImage;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            ivRoomImage = itemView.findViewById(R.id.ivRoomImage);
            tvRoomTitle = itemView.findViewById(R.id.tvRoomTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);



        }
    }
}
