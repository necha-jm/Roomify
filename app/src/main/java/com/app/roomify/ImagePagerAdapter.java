package com.app.roomify;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.app.roomify.network.APIClient;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.app.roomify.network.TokenManager;

import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {

    private List<String> imageUrls;
    private TokenManager tokenManager;

    public ImagePagerAdapter(List<String> imageUrls, TokenManager tokenManager) {
        this.imageUrls = imageUrls;
        this.tokenManager = tokenManager;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_slider, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = imageUrls.get(position);

        if (holder.progressBar != null) {
            holder.progressBar.setVisibility(View.VISIBLE);
        }

        // Ensure full URL for Glide
        if (url != null && !url.startsWith("http")) {
            if (url.startsWith("/")) {
                url = APIClient.BASE_URL.replaceAll("/$", "") + url;
            } else {
                url = APIClient.BASE_URL.replaceAll("/$", "") + "/" + url;
            }
        }

        final String finalUrl = url;

        // Use Glide with the authenticated client provided by RoomifyGlideModule
        Glide.with(holder.imageView.getContext())
                .load(finalUrl)
                .apply(new RequestOptions()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .timeout(30000))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        if (holder.progressBar != null) {
                            holder.progressBar.setVisibility(View.GONE);
                        }
                        android.util.Log.e("ImagePagerAdapter", "Failed to load image: " + finalUrl + ", error: " + (e != null ? e.getMessage() : "Unknown"));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (holder.progressBar != null) {
                            holder.progressBar.setVisibility(View.GONE);
                        }
                        return false;
                    }
                })
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ProgressBar progressBar;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivRoomImage);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}