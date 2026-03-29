package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PendingRequestAdapter extends RecyclerView.Adapter<PendingRequestAdapter.MyHolderView> {

    private ArrayList<BookingRequest> requests;

    public PendingRequestAdapter(ArrayList<BookingRequest> requests) {
        this.requests = requests;
    }


    @NonNull
    @Override
    public PendingRequestAdapter.MyHolderView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.owner_pending_request_item,parent,false);

        return new MyHolderView(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingRequestAdapter.MyHolderView holder, int position) {
        if (requests == null || position >= requests.size()) return;

        BookingRequest request = requests.get(position);

        holder.tvPropertyTitle.setText(request.getRoomTitle());
        holder.tvTenantName.setText(request.getUserName());
        holder.tvPhone.setText(request.getUserPhone());
        holder.tvBookingDate.setText(request.getBookingDate());

    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public void setRequests(List<BookingRequest> pendingRequests) {
        requests.clear();
        requests.addAll(pendingRequests);
        notifyDataSetChanged();
    }

    public class MyHolderView extends RecyclerView.ViewHolder {

        TextView tvBookingDate, tvPhone, tvPropertyTitle, tvTenantName;

        public MyHolderView(@NonNull View itemView) {
            super(itemView);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvPropertyTitle = itemView.findViewById(R.id.tvPropertyTitle);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvBookingDate = itemView.findViewById(R.id.tvBookingDate);



        }
    }
}
