package com.skyinit.pomodorotimer.util;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.skyinit.pomodorotimer.R;

/**
 * 专注/待办分类标签的统一样式：柔和底色 + 深色文字，浅色/深色模式下均可读。
 */
public final class TaskCategoryStyle {

    private TaskCategoryStyle() {
    }

    @DrawableRes
    public static int getBackgroundRes(String category) {
        if (CategoryDefaults.getDefault().equals(category)) {
            return R.drawable.category_default_background;
        }
        if (CategoryDefaults.getWork().equals(category)) {
            return R.drawable.category_work_background;
        }
        if (CategoryDefaults.getStudy().equals(category)) {
            return R.drawable.category_study_background;
        }
        if (CategoryDefaults.getLife().equals(category)) {
            return R.drawable.category_life_background;
        }
        if (CategoryDefaults.getSports().equals(category)) {
            return R.drawable.category_sports_background;
        }
        if (CategoryDefaults.getEntertainment().equals(category)) {
            return R.drawable.category_entertainment_background;
        }
        return R.drawable.category_other_background;
    }

    @ColorRes
    public static int getTextColorRes(String category) {
        if (CategoryDefaults.getDefault().equals(category)) {
            return R.color.task_cat_default_text;
        }
        if (CategoryDefaults.getWork().equals(category)) {
            return R.color.task_cat_work_text;
        }
        if (CategoryDefaults.getStudy().equals(category)) {
            return R.color.task_cat_study_text;
        }
        if (CategoryDefaults.getLife().equals(category)) {
            return R.color.task_cat_life_text;
        }
        if (CategoryDefaults.getSports().equals(category)) {
            return R.color.task_cat_sports_text;
        }
        if (CategoryDefaults.getEntertainment().equals(category)) {
            return R.color.task_cat_entertainment_text;
        }
        return R.color.task_cat_other_text;
    }

    public static void apply(TextView textView, String category) {
        Context context = textView.getContext();
        String resolvedCategory = category;
        if (resolvedCategory == null || resolvedCategory.isEmpty()) {
            resolvedCategory = CategoryDefaults.getOther();
        }
        textView.setBackgroundResource(getBackgroundRes(resolvedCategory));
        textView.setTextColor(ContextCompat.getColor(context, getTextColorRes(resolvedCategory)));
    }
}
