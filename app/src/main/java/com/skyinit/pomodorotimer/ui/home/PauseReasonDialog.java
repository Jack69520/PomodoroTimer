package com.skyinit.pomodorotimer.ui.home;

import com.skyinit.pomodorotimer.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class PauseReasonDialog extends DialogFragment {
    public interface PauseReasonListener {
        void onReasonSelected(String reason);
        void resumeTimer();
    }

    private PauseReasonListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof PauseReasonListener) {
            listener = (PauseReasonListener) context;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.timer_dialog_pause_reason_title);

        String[] reasons = getResources().getStringArray(R.array.pause_reasons);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_pause_reason, null);
        builder.setView(view);

        // 设置原因按钮
        Button reason1 = view.findViewById(R.id.btn_reason_1);
        Button reason2 = view.findViewById(R.id.btn_reason_2);
        Button reason3 = view.findViewById(R.id.btn_reason_3);
        Button reason4 = view.findViewById(R.id.btn_reason_4);
        Button reason5 = view.findViewById(R.id.btn_reason_5);
        Button reason6 = view.findViewById(R.id.btn_reason_6);
        Button resumeBtn = view.findViewById(R.id.btn_resume);

        reason1.setText(reasons[0]);
        reason2.setText(reasons[1]);
        reason3.setText(reasons[2]);
        reason4.setText(reasons[3]);
        reason5.setText(reasons[4]);
        reason6.setText(reasons[5]);

        reason1.setOnClickListener(v -> selectReason(reasons[0]));
        reason2.setOnClickListener(v -> selectReason(reasons[1]));
        reason3.setOnClickListener(v -> selectReason(reasons[2]));
        reason4.setOnClickListener(v -> selectReason(reasons[3]));
        reason5.setOnClickListener(v -> selectReason(reasons[4]));
        reason6.setOnClickListener(v -> selectReason(reasons[5]));
        resumeBtn.setOnClickListener(v -> resumeTimer());

        return builder.create();
    }

    private void selectReason(String reason) {
        if (listener != null) {
            listener.onReasonSelected(reason);
        }
        dismiss();
    }

    private void resumeTimer() {
        if (listener != null) {
            listener.resumeTimer();
        }
        dismiss();
    }
}
