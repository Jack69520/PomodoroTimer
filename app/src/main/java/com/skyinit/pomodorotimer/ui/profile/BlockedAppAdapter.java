package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.util.AppCategory;
import com.skyinit.pomodorotimer.util.WhitelistManager;
import com.skyinit.pomodorotimer.R;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BlockedAppAdapter extends RecyclerView.Adapter<BlockedAppAdapter.ViewHolder> {
    private List<BlockedApp> apps;
    private OnAppToggleListener listener;
    private OnCategoryClickListener categoryClickListener;
    private Context context;

    public interface OnAppToggleListener {
        void onAppToggle(BlockedApp app, boolean isBlocked);
        void onWhitelistToggle(BlockedApp app, boolean isWhitelisted);
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(BlockedApp app);
    }

    public BlockedAppAdapter(List<BlockedApp> apps, OnAppToggleListener listener) {
        this.apps = apps;
        this.listener = listener;
    }

    public void setCategoryClickListener(OnCategoryClickListener categoryClickListener) {
        this.categoryClickListener = categoryClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockedApp app = apps.get(position);

        holder.appNameText.setText(app.appName);
        holder.appPackageText.setText(app.packageName);

        String categoryLabel = app.category;
        if (app.categoryManual) {
            categoryLabel = app.category + context.getString(R.string.blocking_label_manual_suffix);
        }
        holder.appCategoryText.setText(categoryLabel);

        String appType = WhitelistManager.getAppTypeDescription(context, app.packageName);
        String normalType = context.getString(R.string.blocking_app_type_normal);
        if (!normalType.equals(appType)) {
            holder.appTypeText.setText(appType);
            holder.appTypeText.setVisibility(View.VISIBLE);
        } else {
            holder.appTypeText.setVisibility(View.GONE);
        }

        setAppIcon(holder.appIcon, app.packageName);

        holder.blockSwitch.setOnCheckedChangeListener(null);
        holder.whitelistSwitch.setOnCheckedChangeListener(null);

        holder.blockSwitch.setChecked(app.isEnabled);
        holder.whitelistSwitch.setChecked(app.isWhitelisted);

        boolean isSystemCritical = WhitelistManager.isSystemCriticalApp(app.packageName);
        holder.blockSwitch.setEnabled(!isSystemCritical);
        holder.blockSwitch.setAlpha(isSystemCritical ? 0.5f : 1.0f);

        holder.blockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                app.isEnabled = isChecked;
                app.isWhitelisted = !isChecked;
                listener.onAppToggle(app, isChecked);
            }
        });

        holder.whitelistSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                app.isWhitelisted = isChecked;
                app.isEnabled = !isChecked;
                listener.onWhitelistToggle(app, isChecked);
            }
        });

        holder.appCategoryText.setBackgroundResource(AppCategory.getBackgroundRes(app.category));
        holder.appCategoryText.setOnClickListener(v -> {
            if (categoryClickListener != null) {
                categoryClickListener.onCategoryClick(app);
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    private void setAppIcon(ImageView imageView, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            Drawable appIcon = pm.getApplicationIcon(appInfo);
            imageView.setImageDrawable(appIcon);
        } catch (Exception e) {
            imageView.setImageResource(android.R.drawable.ic_menu_info_details);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appNameText;
        TextView appPackageText;
        TextView appCategoryText;
        TextView appTypeText;
        Switch blockSwitch;
        Switch whitelistSwitch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appNameText = itemView.findViewById(R.id.app_name);
            appPackageText = itemView.findViewById(R.id.app_package);
            appCategoryText = itemView.findViewById(R.id.app_category);
            appTypeText = itemView.findViewById(R.id.app_type);
            blockSwitch = itemView.findViewById(R.id.block_switch);
            whitelistSwitch = itemView.findViewById(R.id.whitelist_switch);
        }
    }
}
