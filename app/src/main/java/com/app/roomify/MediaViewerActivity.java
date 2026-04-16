package com.app.roomify;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.app.roomify.network.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class MediaViewerActivity extends AppCompatActivity {

    private static final String TAG = "MediaViewerActivity";
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 201;

    public static final String EXTRA_MEDIA_TYPE = "media_type";
    public static final String EXTRA_MEDIA_URLS = "media_urls";
    public static final String EXTRA_CURRENT_POSITION = "current_position";
    public static final String EXTRA_ROOM_TITLE = "room_title";

    public static final int MEDIA_TYPE_IMAGES = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_DOCUMENT = 3;

    private ViewPager2 viewPagerImages;
    private VideoView videoView;
    private LinearLayout documentViewer;
    private ImageView ivDocumentPreview;
    private TextView tvDocumentName, tvDocumentSize;
    private Button btnDownload, btnOpenWith;
    private ProgressBar progressBar, videoProgressBar;
    private TextView tvTitle;
    private LinearLayout imageIndicator;
    private MediaController mediaController;
    private TokenManager tokenManager;


    private int mediaType;
    private List<String> mediaUrls;
    private int currentPosition;
    private String roomTitle;
    private String documentUrl;
    private String documentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_viewer);

        initViews();
        getIntentData();
        setupContent();
        setupClickListeners();
    }

    private void initViews() {
        viewPagerImages = findViewById(R.id.viewPagerImages);
        videoView = findViewById(R.id.videoView);
        documentViewer = findViewById(R.id.documentViewer);
        ivDocumentPreview = findViewById(R.id.ivDocumentPreview);
        tvDocumentName = findViewById(R.id.tvDocumentName);
        tvDocumentSize = findViewById(R.id.tvDocumentSize);
        btnDownload = findViewById(R.id.btnDownload);
        btnOpenWith = findViewById(R.id.btnOpenWith);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);
        imageIndicator = findViewById(R.id.imageIndicator);

        // Initialize MediaController
        mediaController = new MediaController(this);

        // Initialize TokenManager
        tokenManager = new TokenManager(this);
    }

    // Update setupImageViewer method
    private void setupImageViewer() {
        if (viewPagerImages != null) {
            viewPagerImages.setVisibility(View.VISIBLE);
        }
        if (videoView != null) {
            videoView.setVisibility(View.GONE);
        }
        if (documentViewer != null) {
            documentViewer.setVisibility(View.GONE);
        }
        if (imageIndicator != null) {
            imageIndicator.setVisibility(View.VISIBLE);
        }

        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            // Pass TokenManager to adapter
            ImagePagerAdapter adapter = new ImagePagerAdapter(mediaUrls, tokenManager);
            viewPagerImages.setAdapter(adapter);
            viewPagerImages.setCurrentItem(currentPosition, false);
            setupImageIndicator();
        } else {
            Toast.makeText(this, "No images available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        // Handle media_type (supports both Integer and String)
        if (bundle != null && bundle.containsKey(EXTRA_MEDIA_TYPE)) {
            Object value = bundle.get(EXTRA_MEDIA_TYPE);
            if (value instanceof Integer) {
                mediaType = (Integer) value;
            } else if (value instanceof String) {
                String mediaTypeStr = (String) value;
                if ("images".equalsIgnoreCase(mediaTypeStr)) {
                    mediaType = MEDIA_TYPE_IMAGES;
                } else if ("video".equalsIgnoreCase(mediaTypeStr)) {
                    mediaType = MEDIA_TYPE_VIDEO;
                } else if ("document".equalsIgnoreCase(mediaTypeStr)) {
                    mediaType = MEDIA_TYPE_DOCUMENT;
                } else {
                    mediaType = MEDIA_TYPE_IMAGES;
                }
            } else {
                mediaType = MEDIA_TYPE_IMAGES;
            }
        } else {
            mediaType = MEDIA_TYPE_IMAGES;
        }

        // Get media URLs
        mediaUrls = intent.getStringArrayListExtra(EXTRA_MEDIA_URLS);
        if (mediaUrls == null) {
            mediaUrls = intent.getStringArrayListExtra("image_urls");
        }

        // Handle current_position
        if (bundle != null && bundle.containsKey(EXTRA_CURRENT_POSITION)) {
            Object value = bundle.get(EXTRA_CURRENT_POSITION);
            if (value instanceof Integer) {
                currentPosition = (Integer) value;
            } else if (value instanceof String) {
                try {
                    currentPosition = Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    currentPosition = 0;
                }
            } else {
                currentPosition = 0;
            }
        } else {
            currentPosition = 0;
        }

        roomTitle = intent.getStringExtra(EXTRA_ROOM_TITLE);
        documentUrl = intent.getStringExtra("document_url");
        documentName = intent.getStringExtra("document_name");

        // Debug logging
        android.util.Log.d(TAG, "Media Type: " + mediaType);
        android.util.Log.d(TAG, "Media URLs: " + (mediaUrls != null ? mediaUrls.size() : "null"));
        android.util.Log.d(TAG, "Document URL: " + documentUrl);
    }

    private void setupContent() {
        if (roomTitle != null && tvTitle != null) {
            tvTitle.setText(roomTitle);
        }

        switch (mediaType) {
            case MEDIA_TYPE_IMAGES:
                setupImageViewer();
                break;
            case MEDIA_TYPE_VIDEO:
                setupVideoViewer();
                break;
            case MEDIA_TYPE_DOCUMENT:
                setupDocumentViewer();
                break;
            default:
                finish();
                break;
        }
    }

    private void setupVideoViewer() {
        if (viewPagerImages != null) {
            viewPagerImages.setVisibility(View.GONE);
        }
        if (videoView != null) {
            videoView.setVisibility(View.VISIBLE);
        }
        if (documentViewer != null) {
            documentViewer.setVisibility(View.GONE);
        }
        if (imageIndicator != null) {
            imageIndicator.setVisibility(View.GONE);
        }

        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            String videoUrl = mediaUrls.get(0);
            playVideo(videoUrl);
        } else {
            Toast.makeText(this, "No video available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private void playVideo(String videoUrl) {
        if (videoProgressBar != null) {
            videoProgressBar.setVisibility(View.VISIBLE);
        }

        try {
            Uri videoUri = Uri.parse(videoUrl);

            // Clear any existing video
            videoView.stopPlayback();
            videoView.suspend();

            // Set up media controller
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            // Set video URI and start
            videoView.setVideoURI(videoUri);

            videoView.setOnPreparedListener(mp -> {
                if (videoProgressBar != null) {
                    videoProgressBar.setVisibility(View.GONE);
                }
                mp.setLooping(false);
                mp.setVolume(1.0f, 1.0f);
                videoView.start();
            });

            videoView.setOnCompletionListener(mp -> {
                android.util.Log.d(TAG, "Video playback completed");
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                if (videoProgressBar != null) {
                    videoProgressBar.setVisibility(View.GONE);
                }
                android.util.Log.e(TAG, "Video error: what=" + what + ", extra=" + extra);

                // Try alternative approach - open with external player
                showVideoErrorDialog(videoUrl);
                return true;
            });

        } catch (Exception e) {
            if (videoProgressBar != null) {
                videoProgressBar.setVisibility(View.GONE);
            }
            android.util.Log.e(TAG, "Video playback error: " + e.getMessage());
            showVideoErrorDialog(videoUrl);
        }
    }
    private void showVideoErrorDialog(String videoUrl) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Video Playback Error")
                .setMessage("Cannot play video in the app. Would you like to open it with an external player?")
                .setPositiveButton("Open External", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(videoUrl), "video/*");
                    startActivity(Intent.createChooser(intent, "Play video with"));
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private void setupDocumentViewer() {
        if (viewPagerImages != null) {
            viewPagerImages.setVisibility(View.GONE);
        }
        if (videoView != null) {
            videoView.setVisibility(View.GONE);
        }
        if (documentViewer != null) {
            documentViewer.setVisibility(View.VISIBLE);
        }
        if (imageIndicator != null) {
            imageIndicator.setVisibility(View.GONE);
        }

        if (documentName != null && !documentName.isEmpty()) {
            tvDocumentName.setText(documentName);
        } else {
            tvDocumentName.setText("Contract Document");
        }

        setDocumentIcon(documentName);
        tvDocumentSize.setText("Ready to view");
    }

    private void setDocumentIcon(String fileName) {
        if (fileName == null) {
            ivDocumentPreview.setImageResource(R.drawable.ic_pdf);
            return;
        }

        if (fileName.toLowerCase().endsWith(".pdf")) {
            ivDocumentPreview.setImageResource(R.drawable.ic_pdf);
        } else if (fileName.toLowerCase().endsWith(".doc") || fileName.toLowerCase().endsWith(".docx")) {
            ivDocumentPreview.setImageResource(R.drawable.c_document);
        } else if (fileName.toLowerCase().endsWith(".txt")) {
            ivDocumentPreview.setImageResource(R.drawable.ic_txt);
        } else {
            ivDocumentPreview.setImageResource(R.drawable.c_document);
        }
    }

    private void setupImageIndicator() {
        if (imageIndicator == null || mediaUrls == null) return;

        imageIndicator.removeAllViews();

        for (int i = 0; i < mediaUrls.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            imageIndicator.addView(dot);
        }

        if (imageIndicator.getChildCount() > 0 && currentPosition < imageIndicator.getChildCount()) {
            imageIndicator.getChildAt(currentPosition).setBackgroundResource(R.drawable.dot_active);
        }

        viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < imageIndicator.getChildCount(); i++) {
                    View dot = imageIndicator.getChildAt(i);
                    dot.setBackgroundResource(i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
                }
            }
        });
    }

    private void setupClickListeners() {
        // Close button
        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        // Share button
        View btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> shareFile());
        }

        // Download button
        View btnDownloadMedia = findViewById(R.id.btnDownloadMedia);
        if (btnDownloadMedia != null) {
            btnDownloadMedia.setOnClickListener(v -> downloadFile());
        }

        // Document action buttons
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> downloadFile());
        }

        if (btnOpenWith != null) {
            btnOpenWith.setOnClickListener(v -> openWith());
        }
    }

    private void downloadFile() {
        // Check storage permission first
        if (!checkStoragePermission()) {
            requestStoragePermission();
            return;
        }

        if (mediaType == MEDIA_TYPE_DOCUMENT && documentUrl != null) {
            downloadDocument();
        } else if (mediaType == MEDIA_TYPE_VIDEO && mediaUrls != null && !mediaUrls.isEmpty()) {
            downloadVideo(mediaUrls.get(0));
        } else if (mediaType == MEDIA_TYPE_IMAGES && mediaUrls != null && !mediaUrls.isEmpty()) {
            downloadImage(mediaUrls.get(currentPosition));
        } else {
            Toast.makeText(this, "No file available to download", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadDocument() {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(documentUrl));

            String fileName = documentName != null ? documentName : "contract_" + System.currentTimeMillis() + ".pdf";
            request.setTitle(fileName);
            request.setDescription("Downloading document...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            downloadManager.enqueue(request);
            Toast.makeText(this, "Download started: " + fileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            android.util.Log.e(TAG, "Download error: " + e.getMessage());
        }
    }

    private void downloadVideo(String videoUrl) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoUrl));

            String fileName = "video_" + System.currentTimeMillis() + ".mp4";
            request.setTitle("Room Video");
            request.setDescription("Downloading video...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            downloadManager.enqueue(request);
            Toast.makeText(this, "Download started: " + fileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadImage(String imageUrl) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));

            String fileName = "image_" + System.currentTimeMillis() + ".jpg";
            request.setTitle("Room Image");
            request.setDescription("Downloading image...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, fileName);

            downloadManager.enqueue(request);
            Toast.makeText(this, "Download started: " + fileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        if (mediaType == MEDIA_TYPE_DOCUMENT && documentUrl != null) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this document: " + documentUrl);
        } else if (mediaType == MEDIA_TYPE_VIDEO && mediaUrls != null && !mediaUrls.isEmpty()) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this room video: " + mediaUrls.get(0));
        } else if (mediaType == MEDIA_TYPE_IMAGES && mediaUrls != null && !mediaUrls.isEmpty()) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this room image: " + mediaUrls.get(currentPosition));
        } else {
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this room on Roomify app!");
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void openWith() {
        if (mediaType == MEDIA_TYPE_DOCUMENT && documentUrl != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(documentUrl), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(Intent.createChooser(intent, "Open with"));
            } catch (Exception e) {
                Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
            downloadFile(); // Retry download
        }
    }
}