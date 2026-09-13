package com.skyinit.pomodorotimer.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.R;

import java.util.List;

/**
 * 功能介绍页 ViewPager2 适配器。
 */
final class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.PageHolder> {

    static final class Page {
        @DrawableRes
        final int iconRes;
        @StringRes
        final int titleRes;
        @StringRes
        final int bodyRes;

        Page(@DrawableRes int iconRes, @StringRes int titleRes, @StringRes int bodyRes) {
            this.iconRes = iconRes;
            this.titleRes = titleRes;
            this.bodyRes = bodyRes;
        }
    }

    private final List<Page> pages;

    OnboardingPagerAdapter(@NonNull List<Page> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new PageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        Page page = pages.get(position);
        holder.icon.setImageResource(page.iconRes);
        ImageViewCompat.setImageTintList(
                holder.icon,
                ContextCompat.getColorStateList(holder.icon.getContext(), R.color.brand));
        holder.title.setText(page.titleRes);
        holder.body.setText(page.bodyRes);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static final class PageHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView body;

        PageHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_onboarding_icon);
            title = itemView.findViewById(R.id.tv_onboarding_title);
            body = itemView.findViewById(R.id.tv_onboarding_body);
        }
    }
}
