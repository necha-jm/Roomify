package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private List<BookingRequest> requests;
    private final OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onAction(BookingRequest request, String action);
    }

    public BookingAdapter(List<BookingRequest> requests, OnBookingActionListener listener) {
        this.requests = requests != null ? requests : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingRequest request = requests.get(position);

        if (request == null) {
            return;
        }

        // Set text with null safety
        holder.tvRoomTitle.setText(getSafeString(request.getRoomTitle(), "Unknown Room"));
        holder.tvUserName.setText("From: " + getSafeString(request.getUserName(), "Unknown User"));
        holder.tvUserPhone.setText("Phone: " + getSafeString(request.getUserPhone(), "Not provided"));

        String status = getSafeString(request.getStatus(), "PENDING");
        holder.tvStatus.setText("Status: " + status);

        // Set status color
        setStatusColor(holder.tvStatus, status);

        // Format and set date
        String formattedDate = formatDate(request);
        holder.tvDate.setText(formattedDate);

        // Set price if available
        if (holder.tvPrice != null) {
            holder.tvPrice.setText(request.getFormattedPrice());
        }

        // Configure buttons based on status
        configureButtons(holder, request, status);
    }

    private void setStatusColor(TextView tvStatus, String status) {
        int color;
        switch (status.toUpperCase()) {
            case "PENDING":
                color = android.graphics.Color.parseColor("#FFA500"); // Orange
                break;
            case "ACCEPTED":
                color = android.graphics.Color.parseColor("#4CAF50"); // Green
                break;
            case "REJECTED":
                color = android.graphics.Color.parseColor("#F44336"); // Red
                break;
            case "CANCELLED":
                color = android.graphics.Color.parseColor("#9E9E9E"); // Gray
                break;
            default:
                color = android.graphics.Color.parseColor("#9E9E9E");
                break;
        }
        tvStatus.setTextColor(color);
    }

    private String formatDate(BookingRequest request) {
        if (request.getBookingDate() != null && !request.getBookingDate().isEmpty()) {
            return request.getBookingDate();
        }

        // Try to use createdAt timestamp
        if (request.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(request.getCreatedAt()));
        }

        // Try start date if available
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            return "From: " + request.getStartDate();
        }

        return "Date not available";
    }

    private void configureButtons(ViewHolder holder, BookingRequest request, String status) {
        // Reset all buttons visibility first
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);
        holder.btnDelete.setVisibility(View.GONE);

        // Clear any previous listeners to avoid multiple calls
        holder.btnAccept.setOnClickListener(null);
        holder.btnReject.setOnClickListener(null);
        holder.btnCancel.setOnClickListener(null);
        holder.btnDelete.setOnClickListener(null);

        switch (status.toUpperCase()) {
            case "PENDING":
                // Pending requests: Show Accept/Reject buttons
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);

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
                break;

            case "ACCEPTED":
                // Accepted requests: Show Cancel button
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnCancel.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAction(request, "cancel");
                    }
                });
                break;

            case "REJECTED":
            case "CANCELLED":
                // Rejected or cancelled: Show only Delete button
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAction(request, "delete");
                    }
                });
                break;

            default:
                // Unknown status: Show no action buttons
                break;
        }
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public void addRequest(BookingRequest request) {
        if (request != null) {
            requests.add(request);
            notifyItemInserted(requests.size() - 1);
        }
    }

    public void setRequests(List<BookingRequest> newRequests) {
        if (newRequests != null) {
            this.requests = new ArrayList<>(newRequests);
        } else {
            this.requests = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    public void removeRequest(BookingRequest request) {
        if (request != null) {
            int index = findRequestIndex(request.getId());
            if (index != -1) {
                requests.remove(index);
                notifyItemRemoved(index);
            }
        }
    }

    public void updateRequest(BookingRequest request) {
        if (request != null) {
            int index = findRequestIndex(request.getId());
            if (index != -1) {
                requests.set(index, request);
                notifyItemChanged(index);
            }
        }
    }

    public void clearRequests() {
        if (requests != null) {
            int size = requests.size();
            requests.clear();
            notifyItemRangeRemoved(0, size);
        }
    }

    // FIXED: Changed parameter from String to Long
    private int findRequestIndex(Long requestId) {
        if (requestId == null || requests == null) {
            return -1;
        }
        for (int i = 0; i < requests.size(); i++) {
            BookingRequest request = requests.get(i);
            if (request != null && request.getId() != null && requestId.equals(request.getId())) {
                return i;
            }
        }
        return -1;
    }

    private String getSafeString(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomTitle, tvUserName, tvUserPhone, tvStatus, tvDate, tvPrice;
        Button btnAccept, btnReject, btnCancel, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomTitle = itemView.findViewById(R.id.tvRoomTitle);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserPhone = itemView.findViewById(R.id.tvUserPhone);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}