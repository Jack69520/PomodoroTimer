package com.skyinit.pomodorotimer.ui.profile;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.entity.BlockedApp;
import com.skyinit.pomodorotimer.domain.blocking.AppTypeLabelResolver;
import com.skyinit.pomodorotimer.domain.blocking.BlockingPolicyEngine;
import com.skyinit.pomodorotimer.domain.blocking.BlockingRole;
import com.skyinit.pomodorotimer.util.AppCategory;

import java.util.Objects;

/**
 * 屏蔽应用列表：ListAdapter + DiffUtil，避免全量刷新与开关竞态闪烁。
 */
public class BlockedAppAdapter extends ListAdapter<BlockedApp, BlockedAppAdapter.ViewHolder> {

    public interface OnAppToggleListener {
        void onBlockToggle(BlockedApp app, boolean isBlocked);
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(BlockedApp app);
    }

    private final OnAppToggleListener listener;
    private final OnCategoryClickListener categoryClickListener;
    private final BlockingPolicyEngine policyEngine;
    private final AppTypeLabelResolver typeLabelResolver;
    private final LruCache<String, Drawable> iconCache = new LruCache<>(80);
    private Context context;

    private static final DiffUtil.ItemCallback<BlockedApp> DIFF = new DiffUtil.ItemCallback<BlockedApp>() {
        @Override
        public boolean areItemsTheSame(@NonNull BlockedApp oldItem, @NonNull BlockedApp newItem) {
            return Objects.equals(oldItem.packageName, newItem.packageName);
        }

        @Override
        public boolean areContentsTheSame(@NonNull BlockedApp oldItem, @NonNull BlockedApp newItem) {
            return Objects.equals(oldItem.appName, newItem.appName)
                    && Objects.equals(oldItem.category, newItem.category)
                    && oldItem.categoryManual == newItem.categoryManual
                    && oldItem.isEnabled == newItem.isEnabled
                    && oldItem.isWhitelisted == newItem.isWhitelisted
                    && Objects.equals(oldItem.provenance, newItem.provenance)
                    && Objects.equals(oldItem.blockingRole, newItem.blockingRole);
        }
    };

    public BlockedAppAdapter(OnAppToggleListener listener,
                             OnCategoryClickListener categoryClickListener,
                             BlockingPolicyEngine policyEngine,
                             AppTypeLabelResolver typeLabelResolver) {
        super(DIFF);
        this.listener = listener;
        this.categoryClickListener = categoryClickListener;
        this.policyEngine = policyEngine;
        this.typeLabelResolver = typeLabelResolver;
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
        BlockedApp app = getItem(position);
        holder.boundPackageName = app.packageName;

        holder.appNameText.setText(app.appName);
        holder.appPackageText.setText(app.packageName);

        String categoryLabel = app.category;
        if (app.categoryManual) {
            categoryLabel = app.category + context.getString(R.string.blocking_label_manual_suffix);
        }
        holder.appCategoryText.setText(categoryLabel);
        holder.appCategoryText.setBackgroundResource(AppCategory.getBackgroundRes(app.category));

        String appType = typeLabelResolver.resolve(context, app.packageName, app.provenance);
        String normalType = context.getString(R.string.blocking_app_type_normal);
        if (!normalType.equals(appType)
                && !context.getString(R.string.app_type_system_critical).equals(appType)) {
            holder.appTypeText.setText(appType);
            holder.appTypeText.setVisibility(View.VISIBLE);
        } else {
            holder.appTypeText.setVisibility(View.GONE);
        }

        boolean isCritical = BlockingRole.CRITICAL == BlockingRole.fromStorage(app.blockingRole)
                || policyEngine.isCritical(app.packageName);

        holder.lockedBadge.setVisibility(isCritical ? View.VISIBLE : View.GONE);

        bindAppIcon(holder, app.packageName);

        String displayName = app.appName != null ? app.appName : app.packageName;
        holder.blockSwitch.setOnCheckedChangeListener(null);
        holder.blockSwitch.setChecked(app.isEnabled);
        holder.blockSwitch.setEnabled(!isCritical);
        holder.blockSwitch.setAlpha(isCritical ? 0.45f : 1f);

        if (isCritical) {
            holder.statusLabel.setText(R.string.blocking_status_locked);
            holder.blockSwitch.setContentDescription(
                    context.getString(R.string.blocking_a11y_switch_locked, displayName));
        } else if (app.isEnabled) {
            holder.statusLabel.setText(R.string.blocking_status_blocked);
            holder.blockSwitch.setContentDescription(
                    context.getString(R.string.blocking_a11y_switch_blocked, displayName));
        } else {
            holder.statusLabel.setText(R.string.blocking_status_allowed);
            holder.blockSwitch.setContentDescription(
                    context.getString(R.string.blocking_a11y_switch_allowed, displayName));
        }

        holder.blockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isCritical) {
                buttonView.setChecked(app.isEnabled);
                return;
            }
            if (listener != null) {
                listener.onBlockToggle(app, isChecked);
            }
        });

        holder.appCategoryText.setOnClickListener(v -> {
            if (categoryClickListener != null) {
                categoryClickListener.onCategoryClick(app);
            }
        });
    }

    private void bindAppIcon(ViewHolder holder, String packageName) {
        Drawable cached = iconCache.get(packageName);
        if (cached != null) {
            holder.appIcon.setImageDrawable(cached);
            return;
        }
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            Drawable appIcon = pm.getApplicationIcon(appInfo);
            iconCache.put(packageName, appIcon);
            if (packageName.equals(holder.boundPackageName)) {
                holder.appIcon.setImageDrawable(appIcon);
            }
        } catch (Exception e) {
            if (packageName.equals(holder.boundPackageName)) {
                holder.appIcon.setImageResource(android.R.drawable.ic_menu_info_details);
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView appIcon;
        final TextView appNameText;
        final TextView appPackageText;
        final TextView appCategoryText;
        final TextView appTypeText;
        final TextView lockedBadge;
        final SwitchCompat blockSwitch;
        final TextView statusLabel;
        String boundPackageName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appNameText = itemView.findViewById(R.id.app_name);
            appPackageText = itemView.findViewById(R.id.app_package);
            appCategoryText = itemView.findViewById(R.id.app_category);
            appTypeText = itemView.findViewById(R.id.app_type);
            lockedBadge = itemView.findViewById(R.id.locked_badge);
            blockSwitch = itemView.findViewById(R.id.block_switch);
            statusLabel = itemView.findViewById(R.id.block_status_label);
        }
    }
}
