package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.R;
import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

public class RingtonePicker {
    private final Fragment fragment;
    private final ActivityResultLauncher<Intent> ringtonePickerLauncher;
    private RingtoneSelectedListener listener;

    public interface RingtoneSelectedListener {
        void onRingtoneSelected(Uri uri);
    }

    public RingtonePicker(Fragment fragment) {
        this.fragment = fragment;
        ringtonePickerLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = getRingtonePickedUri(result.getData());
                        if (listener != null && uri != null) {
                            listener.onRingtoneSelected(uri);
                        }
                    }
                });
    }

    public void setListener(RingtoneSelectedListener listener) {
        this.listener = listener;
    }

    private Uri getRingtonePickedUri(Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
        }
        return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
    }

    public void pickRingtone() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                fragment.getString(R.string.select_ringtone));
        ringtonePickerLauncher.launch(intent);
    }
}