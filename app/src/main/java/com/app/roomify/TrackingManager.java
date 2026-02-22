package com.app.roomify;

public class TrackingManager {

    // Singleton instance
    private static TrackingManager instance;

    private boolean isTrackingPaused = false;

    // Private constructor
    private TrackingManager() { }

    // Get instance
    public static TrackingManager getInstance() {
        if (instance == null) {
            instance = new TrackingManager();
        }
        return instance;
    }

    // Pause tracking actions
    public void pauseTracking() {
        isTrackingPaused = true;
        // Here you can stop network calls or stop posting room updates
        // Example: stop sending room data to server
        System.out.println("Tracking paused due to no internet");
    }

    // Resume tracking actions
    public void resumeTracking() {
        if (isTrackingPaused) {
            isTrackingPaused = false;
            // Resume network calls or continue posting room updates
            System.out.println("Tracking resumed as internet is back");
        }
    }

    // Optional: check if tracking is paused
    public boolean isPaused() {
        return isTrackingPaused;
    }
}