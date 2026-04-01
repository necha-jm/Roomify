package com.app.roomify;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class MediaViewerActivity extends AppCompatActivity {

    private static final String TAG = "MediaViewerActivity";

    public static final String EXTRA_MEDIA_TYPE = "media_type";
    public static final String EXTRA_MEDIA_URLS = "media_urls";
    public static final String EXTRA_CURRENT_POSITION = "current_position";
    public static final String EXTRA_ROOM_TITLE = "room_title";

    public static final int MEDIA_TYPE_IMAGES = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_DOCUMENT = 3;

    private ViewPager2 viewPagerImages;
    private VideoView videoView;
    private ImageView ivDocumentPreview;
    private TextView tvDocumentName, tvDocumentSize;
    private MaterialButton btnDownload, btnShare, btnOpenWith;
    private ProgressBar progressBar;
    private TextView tvTitle;

    private int mediaType;
    private List<String> mediaUrls;
    private int currentPosition;
    private String roomTitle;
    private String documentUrl;
    private String documentName;

    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_viewer);

        storage = FirebaseStorage.getInstance();
        initViews();
        getIntentData();
        setupContent();
        setupClickListeners();
    }

    private void initViews() {
        viewPagerImages = findViewById(R.id.viewPagerImages);
        videoView = findViewById(R.id.videoView);
        ivDocumentPreview = findViewById(R.id.ivDocumentPreview);
        tvDocumentName = findViewById(R.id.tvDocumentName);
        tvDocumentSize = findViewById(R.id.tvDocumentSize);
        btnDownload = findViewById(R.id.btnDownload);
        btnShare = findViewById(R.id.btnShare);
        btnOpenWith = findViewById(R.id.btnOpenWith);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);
    }

    private void getIntentData() {
        mediaType = getIntent().getIntExtra(EXTRA_MEDIA_TYPE, MEDIA_TYPE_IMAGES);
        mediaUrls = getIntent().getStringArrayListExtra(EXTRA_MEDIA_URLS);
        currentPosition = getIntent().getIntExtra(EXTRA_CURRENT_POSITION, 0);
        roomTitle = getIntent().getStringExtra(EXTRA_ROOM_TITLE);
        documentUrl = getIntent().getStringExtra("document_url");
        documentName = getIntent().getStringExtra("document_name");
    }

    private void setupContent() {
        if (roomTitle != null) {
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
        }
    }

    private void setupImageViewer() {
        viewPagerImages.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.GONE);
        ivDocumentPreview.setVisibility(View.GONE);

        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            ImagePagerAdapter adapter = new ImagePagerAdapter(mediaUrls);
            viewPagerImages.setAdapter(adapter);
            viewPagerImages.setCurrentItem(currentPosition, false);

            // Setup image indicator
            setupImageIndicator();
        } else {
            Toast.makeText(this, "No images available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private void setupVideoViewer() {
        viewPagerImages.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        ivDocumentPreview.setVisibility(View.GONE);

        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            String videoUrl = mediaUrls.get(0);
            playVideo(videoUrl);
        } else {
            Toast.makeText(this, "No video available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void playVideo(String videoUrl) {
        progressBar.setVisibility(View.VISIBLE);

        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.setOnPreparedListener(mp -> {
            progressBar.setVisibility(View.GONE);
            mp.setLooping(false);
            videoView.start();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
            return false;
        });

        // Add media controller
        android.widget.MediaController mediaController = new android.widget.MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
    }

    private void setupDocumentViewer() {
        viewPagerImages.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        ivDocumentPreview.setVisibility(View.VISIBLE);

        if (documentName != null) {
            tvDocumentName.setText(documentName);
        }

        // Set document icon based on type
        setDocumentIcon(documentName);

        // Show document size (if available)
        if (documentUrl != null) {
            getDocumentSize(documentUrl);
        }
    }

    private void setDocumentIcon(String fileName) {
        if (fileName == null) return;

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

    private void getDocumentSize(String url) {
        // In a real app, you would fetch the file size from Firebase Storage
        tvDocumentSize.setText("Size: ~2.5 MB");
    }

    private void setupImageIndicator() {
        LinearLayout indicator = findViewById(R.id.imageIndicator);
        if (indicator == null) return;

        indicator.removeAllViews();

        for (int i = 0; i < mediaUrls.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(8), dpToPx(8)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            indicator.addView(dot);
        }

        if (indicator.getChildCount() > 0) {
            indicator.getChildAt(currentPosition).setBackgroundResource(R.drawable.dot_active);
        }

        viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < indicator.getChildCount(); i++) {
                    View dot = indicator.getChildAt(i);
                    dot.setBackgroundResource(
                            i == position ? R.drawable.dot_active : R.drawable.dot_inactive
                    );
                }
            }
        });
    }

    private void setupClickListeners() {
        btnDownload.setOnClickListener(v -> downloadFile());
        btnShare.setOnClickListener(v -> shareFile());
        btnOpenWith.setOnClickListener(v -> openWith());

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }

    private void downloadFile() {
        if (mediaType == MEDIA_TYPE_DOCUMENT && documentUrl != null) {
            downloadDocument();
        } else if (mediaType == MEDIA_TYPE_VIDEO && mediaUrls != null && !mediaUrls.isEmpty()) {
            downloadVideo(mediaUrls.get(0));
        } else if (mediaType == MEDIA_TYPE_IMAGES && mediaUrls != null && !mediaUrls.isEmpty()) {
            downloadImage(mediaUrls.get(currentPosition));
        }
    }

    private void downloadDocument() {
        progressBar.setVisibility(View.VISIBLE);

        // Create a download intent
        android.app.DownloadManager downloadManager = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(documentUrl));

        request.setTitle(documentName != null ? documentName : "Document");
        request.setDescription("Downloading document...");
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, documentName);

        downloadManager.enqueue(request);

        Toast.makeText(this, "Download started: " + documentName, Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
    }

    private void downloadVideo(String videoUrl) {
        progressBar.setVisibility(View.VISIBLE);

        android.app.DownloadManager downloadManager = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(videoUrl));

        String fileName = "video_" + System.currentTimeMillis() + ".mp4";
        request.setTitle("Room Video");
        request.setDescription("Downloading video...");
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);

        downloadManager.enqueue(request);

        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
    }

    private void downloadImage(String imageUrl) {
        progressBar.setVisibility(View.VISIBLE);

        android.app.DownloadManager downloadManager = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(imageUrl));

        String fileName = "image_" + System.currentTimeMillis() + ".jpg";
        request.setTitle("Room Image");
        request.setDescription("Downloading image...");
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_PICTURES, fileName);

        downloadManager.enqueue(request);

        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
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
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void openWith() {
        if (mediaType == MEDIA_TYPE_DOCUMENT && documentUrl != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(documentUrl), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            try {
                startActivity(Intent.createChooser(intent, "Open with"));
            } catch (Exception e) {
                Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
            }
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
}