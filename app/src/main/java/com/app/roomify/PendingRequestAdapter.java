package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PendingRequestAdapter extends RecyclerView.Adapter<PendingRequestAdapter.MyHolderView> {

    private List<BookingRequest> requests;
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAction(BookingRequest request, String action);
    }

    public PendingRequestAdapter(List<BookingRequest> requests, OnRequestActionListener listener) {
        this.requests = requests != null ? requests : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyHolderView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.owner_pending_request_item, parent, false);
        return new MyHolderView(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolderView holder, int position) {
        if (requests == null || position >= requests.size()) return;

        BookingRequest request = requests.get(position);

        // Set data
        holder.tvPropertyTitle.setText(getSafeString(request.getRoomTitle(), "Unknown Property"));
        holder.tvTenantName.setText(getSafeString(request.getUserName(), "Unknown User"));
        holder.tvPhone.setText(getSafeString(request.getUserPhone(), "No phone"));
        holder.tvBookingDate.setText(getSafeString(request.getBookingDate(), "Date not set"));

        // Set click listeners for buttons
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAction(request, "accept");
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAction(request, "reject");
            }
        });
    }

    private String getSafeString(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public void setRequests(List<BookingRequest> pendingRequests) {
        this.requests.clear();
        this.requests.addAll(pendingRequests);
        notifyDataSetChanged();
    }

    public static class MyHolderView extends RecyclerView.ViewHolder {
        TextView tvBookingDate, tvPhone, tvPropertyTitle, tvTenantName;
        Button btnAccept, btnReject;

        public MyHolderView(@NonNull View itemView) {
            super(itemView);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvPropertyTitle = itemView.findViewById(R.id.tvPropertyTitle);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}