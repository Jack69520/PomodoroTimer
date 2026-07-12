package com.skyinit.pomodorotimer.ui.home;

import android.graphics.Canvas;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.R;

/**
 * 左滑待办卡片以露出后方垃圾桶图标；滑动过程中图标有轻微位移与旋转。
 */
public class TodoSwipeCallback extends ItemTouchHelper.SimpleCallback {

    private static final float SWIPE_THRESHOLD = 0.35f;
    private static final float MAX_SWIPE_RATIO = 0.38f;

    public interface OnSwipeListener {
        void onSwiped(int position);
    }

    private final OnSwipeListener listener;

    public TodoSwipeCallback(OnSwipeListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.listener = listener;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            listener.onSwiped(position);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return SWIPE_THRESHOLD;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        View itemView = viewHolder.itemView;
        View foreground = itemView.findViewById(R.id.todo_item_container);
        ImageView trashIcon = itemView.findViewById(R.id.swipe_delete_icon);

        if (foreground == null) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        float maxSwipe = itemView.getWidth() * MAX_SWIPE_RATIO;
        float clampedDx = Math.max(dX, -maxSwipe);
        if (dX > 0f) {
            clampedDx = 0f;
        }

        foreground.setTranslationX(clampedDx);
        updateTrashIconAnimation(trashIcon, clampedDx, maxSwipe);

        super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        resetSwipeViews(viewHolder.itemView);
    }

    static void updateTrashIconAnimation(ImageView trashIcon, float clampedDx, float maxSwipe) {
        if (trashIcon == null || maxSwipe <= 0f) {
            return;
        }

        float progress = Math.min(Math.abs(clampedDx) / maxSwipe, 1f);
        float wobble = (float) Math.sin(progress * Math.PI * 2.5);
        float density = trashIcon.getResources().getDisplayMetrics().density;

        float iconTranslationX = (-3f + wobble * 2.5f) * density * progress;
        float rotation = wobble * 10f * progress;

        trashIcon.setTranslationX(iconTranslationX);
        trashIcon.setRotation(rotation);
        trashIcon.setAlpha(0.45f + 0.55f * progress);
    }

    static void resetSwipeViews(View itemView) {
        View foreground = itemView.findViewById(R.id.todo_item_container);
        ImageView trashIcon = itemView.findViewById(R.id.swipe_delete_icon);

        if (foreground != null) {
            foreground.setTranslationX(0f);
        }
        if (trashIcon != null) {
            trashIcon.setTranslationX(0f);
            trashIcon.setRotation(0f);
            trashIcon.setAlpha(1f);
        }
    }
}
