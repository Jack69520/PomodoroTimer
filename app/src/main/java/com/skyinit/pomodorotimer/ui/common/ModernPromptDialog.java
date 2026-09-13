package com.skyinit.pomodorotimer.ui.common;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.skyinit.pomodorotimer.R;

/**
 * 现代化提示弹窗：圆角卡片 + 图标区 + 纵向按钮，统一账户/鉴权相关交互。
 */
public final class ModernPromptDialog {

    public enum Accent {
        BRAND,
        DANGER,
        SUCCESS
    }

    private ModernPromptDialog() {
    }

    @NonNull
    public static Builder builder(@NonNull Context context) {
        return new Builder(context);
    }

    public static final class Builder {
        private final Context context;
        @DrawableRes
        private int iconRes;
        private Accent accent = Accent.BRAND;
        @Nullable
        private CharSequence title;
        @Nullable
        private CharSequence message;
        @Nullable
        private CharSequence primaryText;
        @Nullable
        private CharSequence secondaryText;
        @Nullable
        private CharSequence tertiaryText;
        @Nullable
        private Runnable onPrimary;
        @Nullable
        private Runnable onSecondary;
        @Nullable
        private Runnable onTertiary;
        private boolean cancelable = true;
        private boolean primaryDanger;

        private Builder(@NonNull Context context) {
            this.context = context;
        }

        @NonNull
        public Builder icon(@DrawableRes int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        @NonNull
        public Builder accent(@NonNull Accent accent) {
            this.accent = accent;
            return this;
        }

        @NonNull
        public Builder title(@StringRes int titleRes) {
            this.title = context.getString(titleRes);
            return this;
        }

        @NonNull
        public Builder title(@Nullable CharSequence title) {
            this.title = title;
            return this;
        }

        @NonNull
        public Builder message(@StringRes int messageRes) {
            this.message = context.getString(messageRes);
            return this;
        }

        @NonNull
        public Builder message(@Nullable CharSequence message) {
            this.message = message;
            return this;
        }

        @NonNull
        public Builder primary(@StringRes int textRes, @Nullable Runnable action) {
            this.primaryText = context.getString(textRes);
            this.onPrimary = action;
            return this;
        }

        @NonNull
        public Builder primaryDanger(@StringRes int textRes, @Nullable Runnable action) {
            this.primaryDanger = true;
            return primary(textRes, action);
        }

        @NonNull
        public Builder secondary(@StringRes int textRes, @Nullable Runnable action) {
            this.secondaryText = context.getString(textRes);
            this.onSecondary = action;
            return this;
        }

        @NonNull
        public Builder tertiary(@StringRes int textRes, @Nullable Runnable action) {
            this.tertiaryText = context.getString(textRes);
            this.onTertiary = action;
            return this;
        }

        @NonNull
        public Builder cancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        @NonNull
        public AlertDialog show() {
            View content = LayoutInflater.from(context).inflate(R.layout.dialog_modern_prompt, null, false);
            FrameLayout iconContainer = content.findViewById(R.id.dialog_icon_container);
            ImageView iconView = content.findViewById(R.id.dialog_icon);
            TextView titleView = content.findViewById(R.id.dialog_title);
            TextView messageView = content.findViewById(R.id.dialog_message);
            TextView primaryBtn = content.findViewById(R.id.dialog_btn_primary);
            TextView secondaryBtn = content.findViewById(R.id.dialog_btn_secondary);
            TextView tertiaryBtn = content.findViewById(R.id.dialog_btn_tertiary);

            applyAccent(iconContainer, iconView);
            if (iconRes != 0) {
                iconView.setImageResource(iconRes);
                iconContainer.setVisibility(View.VISIBLE);
            } else {
                iconContainer.setVisibility(View.GONE);
            }

            titleView.setText(title);
            titleView.setVisibility(isEmpty(title) ? View.GONE : View.VISIBLE);
            messageView.setText(message);
            messageView.setVisibility(isEmpty(message) ? View.GONE : View.VISIBLE);

            AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                    .setView(content)
                    .setCancelable(cancelable)
                    .create();

            bindAction(primaryBtn, primaryText, () -> {
                dialog.dismiss();
                if (onPrimary != null) {
                    onPrimary.run();
                }
            });
            if (primaryDanger) {
                primaryBtn.setBackgroundResource(R.drawable.bg_dialog_btn_danger);
                primaryBtn.setTextColor(ContextCompat.getColor(context, R.color.on_brand));
            }
            bindAction(secondaryBtn, secondaryText, () -> {
                dialog.dismiss();
                if (onSecondary != null) {
                    onSecondary.run();
                }
            });
            bindAction(tertiaryBtn, tertiaryText, () -> {
                dialog.dismiss();
                if (onTertiary != null) {
                    onTertiary.run();
                }
            });

            dialog.setOnShowListener(d -> {
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                }
            });
            dialog.show();
            return dialog;
        }

        private void applyAccent(@NonNull FrameLayout iconContainer, @NonNull ImageView iconView) {
            int bgRes;
            int tint;
            switch (accent) {
                case DANGER:
                    bgRes = R.drawable.bg_dialog_icon_circle_danger;
                    tint = R.color.semantic_error;
                    break;
                case SUCCESS:
                    bgRes = R.drawable.bg_dialog_icon_circle_success;
                    tint = R.color.semantic_success;
                    break;
                case BRAND:
                default:
                    bgRes = R.drawable.bg_dialog_icon_circle;
                    tint = R.color.brand;
                    break;
            }
            iconContainer.setBackgroundResource(bgRes);
            iconView.setColorFilter(ContextCompat.getColor(context, tint));
        }

        private static void bindAction(@NonNull TextView button,
                                       @Nullable CharSequence text,
                                       @NonNull Runnable action) {
            if (isEmpty(text)) {
                button.setVisibility(View.GONE);
                return;
            }
            button.setVisibility(View.VISIBLE);
            button.setText(text);
            button.setOnClickListener(v -> action.run());
        }

        private static boolean isEmpty(@Nullable CharSequence text) {
            return text == null || text.length() == 0;
        }
    }
}
