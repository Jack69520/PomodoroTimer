package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.R;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ImagePreviewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        ImageView imageView = findViewById(R.id.preview_image);
        String path = getIntent().getStringExtra("image_path");
        if (path != null) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(path));
        }
        imageView.setOnClickListener(v -> finish());
    }
}



