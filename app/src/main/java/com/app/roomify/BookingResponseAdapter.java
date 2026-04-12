package com.app.roomify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.roomify.models.BookingResponse;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookingResponseAdapter extends RecyclerView.Adapter<BookingResponseAdapter.ViewHolder> {

    private List<BookingResponse> bookings;
    private final OnBookingActionListener actionListener;

    public interface OnBookingActionListener {
        void onAction(BookingResponse booking, String action);
    }

    public BookingResponseAdapter(List<BookingResponse> bookings, OnBookingActionListener listener) {
        this.bookings = bookings != null ? bookings : new ArrayList<>();
        this.actionListener = listener;
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
        BookingResponse booking = bookings.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void setBookings(List<BookingResponse> newBookings) {
        this.bookings = new ArrayList<>(newBookings);
        notifyDataSetChanged();
    }

    public void updateBooking(BookingResponse updatedBooking) {
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).getId().equals(updatedBooking.getId())) {
                bookings.set(i, updatedBooking);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeBooking(BookingResponse booking) {
        int position = -1;
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).getId().equals(booking.getId())) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            bookings.remove(position);
            notifyItemRemoved(position);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        // Views from your layout
        TextView tvRoomTitle, tvStatus, tvUserName, tvUserPhone, tvDate;
        Button btnAccept, btnReject, btnCancel, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views with the correct IDs from your layout
            tvRoomTitle = itemView.findViewById(R.id.tvRoomTitle);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserPhone = itemView.findViewById(R.id.tvUserPhone);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(BookingResponse booking) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
            String formattedPrice = currencyFormat.format(booking.getTotalPrice());

            // Set room title
            if (tvRoomTitle != null) {
                String title = booking.getRoomTitle() != null ? booking.getRoomTitle() : "Room #" + booking.getRoomId();
                tvRoomTitle.setText(title);
            }

            // Set status with color
            if (tvStatus != null) {
                String status = booking.getStatus();
                tvStatus.setText(status != null ? status : "PENDING");

                // Set status background color
                if (status != null) {
                    if (status.equalsIgnoreCase("ACCEPTED") || status.equalsIgnoreCase("CONFIRMED")) {
                        tvStatus.setBackgroundColor(0xFF4CAF50);
                    } else if (status.equalsIgnoreCase("REJECTED")) {
                        tvStatus.setBackgroundColor(0xFFF44336);
                    } else if (status.equalsIgnoreCase("CANCELLED")) {
                        tvStatus.setBackgroundColor(0xFF9E9E9E);
                    } else if (status.equalsIgnoreCase("PENDING")) {
                        tvStatus.setBackgroundColor(0xFFFF9800);
                    }
                }
            }

            // Set user name
            if (tvUserName != null) {
                String userName = booking.getTenantName() != null ? booking.getTenantName() :
                        (booking.getUserName() != null ? booking.getUserName() : "Tenant");
                tvUserName.setText(userName);
            }

            // Set user phone (if available, otherwise show email)
            if (tvUserPhone != null) {
                String contact = booking.getTenantEmail() != null ? booking.getTenantEmail() :
                        (booking.getUserEmail() != null ? booking.getUserEmail() : "No contact");
                tvUserPhone.setText(contact);
            }

            // Set date range
            if (tvDate != null) {
                String dateRange = booking.getStartDate() + " - " + booking.getEndDate();
                tvDate.setText(dateRange);
            }

            // Hide all buttons first
            if (btnAccept != null) btnAccept.setVisibility(View.GONE);
            if (btnReject != null) btnReject.setVisibility(View.GONE);
            if (btnCancel != null) btnCancel.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);

            // Show appropriate buttons based on status
            String status = booking.getStatus();
            if (status != null) {
                if (status.equalsIgnoreCase("PENDING")) {
                    if (btnAccept != null) {
                        btnAccept.setVisibility(View.VISIBLE);
                        btnAccept.setOnClickListener(v -> actionListener.onAction(booking, "accept"));
                    }
                    if (btnReject != null) {
                        btnReject.setVisibility(View.VISIBLE);
                        btnReject.setOnClickListener(v -> actionListener.onAction(booking, "reject"));
                    }
                } else if (status.equalsIgnoreCase("ACCEPTED") || status.equalsIgnoreCase("CONFIRMED")) {
                    if (btnCancel != null) {
                        btnCancel.setVisibility(View.VISIBLE);
                        btnCancel.setOnClickListener(v -> actionListener.onAction(booking, "cancel"));
                    }
                } else if (status.equalsIgnoreCase("REJECTED") || status.equalsIgnoreCase("CANCELLED")) {
                    if (btnDelete != null) {
                        btnDelete.setVisibility(View.VISIBLE);
                        btnDelete.setOnClickListener(v -> actionListener.onAction(booking, "delete"));
                    }
                }
            }
        }
    }
}