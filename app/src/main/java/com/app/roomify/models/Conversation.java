package com.app.roomify.models;

import android.os.Message;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Conversation {
    @SerializedName("conversationId")
    private long conversationId;

    @SerializedName("otherPartyId")
    private long otherPartyId;

    @SerializedName("otherPartyName")
    private String otherPartyName;

    @SerializedName("otherPartyRole")
    private String otherPartyRole;

    @SerializedName("lastMessage")
    private String lastMessage;

    @SerializedName("lastMessageTime")
    private String lastMessageTime;

    @SerializedName("unreadCount")
    private int unreadCount;

    @SerializedName("messages")
    private List<Message> messages;

    // Getters and setters
    public long getConversationId() { return conversationId; }
    public void setConversationId(long conversationId) { this.conversationId = conversationId; }

    public long getOtherPartyId() { return otherPartyId; }
    public void setOtherPartyId(long otherPartyId) { this.otherPartyId = otherPartyId; }

    public String getOtherPartyName() { return otherPartyName; }
    public void setOtherPartyName(String otherPartyName) { this.otherPartyName = otherPartyName; }

    public String getOtherPartyRole() { return otherPartyRole; }
    public void setOtherPartyRole(String otherPartyRole) { this.otherPartyRole = otherPartyRole; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
}