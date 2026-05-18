package com.app.roomify.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EarningsResponse {
    @SerializedName("totalEarnings")
    private double totalEarnings;

    @SerializedName("pendingEarnings")
    private double pendingEarnings;

    @SerializedName("withdrawnEarnings")
    private double withdrawnEarnings;

    @SerializedName("transactions")
    private List<EarningTransaction> transactions;

    // Getters and setters
    public double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }

    public double getPendingEarnings() { return pendingEarnings; }
    public void setPendingEarnings(double pendingEarnings) { this.pendingEarnings = pendingEarnings; }

    public double getWithdrawnEarnings() { return withdrawnEarnings; }
    public void setWithdrawnEarnings(double withdrawnEarnings) { this.withdrawnEarnings = withdrawnEarnings; }

    public List<EarningTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<EarningTransaction> transactions) { this.transactions = transactions; }
}