package com.skyinit.pomodorotimer.ui.settings;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.skyinit.pomodorotimer.R;

/**
 * 主题色单选卡片列表（当前分类下）。
 */
public class ThemeColorAdapter extends ListAdapter<ThemeColorUiState.Item, ThemeColorAdapter.CardHolder> {

    public interface Listener {
        void onThemeSelected(@NonNull String themeKey);
    }

    private final Listener listener;

    public ThemeColorAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_theme_color_card, parent, false);
        return new CardHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class CardHolder extends RecyclerView.ViewHolder {
        private final View root;
        private final View preview;
        private final TextView name;
        private final ImageView check;

        CardHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.theme_card_root);
            preview = itemView.findViewById(R.id.theme_preview);
            name = itemView.findViewById(R.id.theme_name);
            check = itemView.findViewById(R.id.theme_check);
        }

        void bind(@NonNull ThemeColorUiState.Item item, @NonNull Listener listener) {
            name.setText(item.name);
            root.setBackgroundResource(item.selected
                    ? R.drawable.bg_theme_color_card_selected
                    : R.drawable.bg_theme_color_card);
            check.setVisibility(item.selected ? View.VISIBLE : View.GONE);
            applyPreview(item);
            root.setOnClickListener(v -> {
                if (!item.selected) {
                    listener.onThemeSelected(item.key);
                }
            });
            root.setEnabled(!item.selected);
            root.setClickable(!item.selected);
        }

        private void applyPreview(@NonNull ThemeColorUiState.Item item) {
            try {
                if (item.gradient) {
                    preview.setBackground(ContextCompat.getDrawable(itemView.getContext(), item.resId));
                    preview.setClipToOutline(true);
                    preview.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, android.graphics.Outline outline) {
                            outline.setOval(0, 0, view.getWidth(), view.getHeight());
                        }
                    });
                } else {
                    GradientDrawable shape = new GradientDrawable();
                    shape.setShape(GradientDrawable.OVAL);
                    shape.setColor(ContextCompat.getColor(itemView.getContext(), item.resId));
                    preview.setBackground(shape);
                    preview.setClipToOutline(false);
                }
            } catch (Exception e) {
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.wallpaper_standard_00));
                preview.setBackground(shape);
            }
        }
    }

    private static final DiffUtil.ItemCallback<ThemeColorUiState.Item> DIFF =
            new DiffUtil.ItemCallback<ThemeColorUiState.Item>() {
                @Override
                public boolean areItemsTheSame(@NonNull ThemeColorUiState.Item oldItem,
                                               @NonNull ThemeColorUiState.Item newItem) {
                    return oldItem.key.equals(newItem.key);
                }

                @Override
                public boolean areContentsTheSame(@NonNull ThemeColorUiState.Item oldItem,
                                                  @NonNull ThemeColorUiState.Item newItem) {
                    return oldItem.selected == newItem.selected
                            && oldItem.name.equals(newItem.name)
                            && oldItem.gradient == newItem.gradient
                            && oldItem.resId == newItem.resId;
                }
            };
}
