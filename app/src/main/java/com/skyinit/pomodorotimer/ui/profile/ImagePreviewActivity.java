package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.R;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

/**
 * 头像/图片全屏预览：Edge-to-Edge，背景铺满系统栏区域。
 */
public class ImagePreviewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_image_preview);

        ImageView imageView = findViewById(R.id.preview_image);
        String path = getIntent().getStringExtra("image_path");
        if (path != null) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(path));
        }
        imageView.setOnClickListener(v -> finish());
    }
}
