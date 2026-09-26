package com.skyinit.pomodorotimer.ui.home;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.domain.timer.PauseReasonPromptMode;

/**
 * 暂停原因标注对话框。计时已在弹出前暂停。
 */
public class PauseReasonDialog extends DialogFragment {

    public static final String TAG = "pause_reason_dialog";
    private static final String ARG_MODE = "mode";

    public interface PauseReasonListener {
        void onReasonSelected(String reason);

        void onReasonSkipped();

        void onReasonQuickResume();
    }

    private PauseReasonListener listener;
    private PauseReasonPromptMode mode = PauseReasonPromptMode.ASK_SKIPPABLE;
    private boolean settledByUser;

    public static PauseReasonDialog newInstance(PauseReasonPromptMode mode) {
        PauseReasonDialog dialog = new PauseReasonDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode != null ? mode.storageValue : 0);
        dialog.setArguments(args);
        return dialog;
    }

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
        if (getArguments() != null) {
            mode = PauseReasonPromptMode.fromStorage(getArguments().getInt(ARG_MODE, 0));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.timer_dialog_pause_reason_title);

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_pause_reason, null);
        builder.setView(view);

        String[] reasons = getResources().getStringArray(R.array.pause_reasons);
        Button reason1 = view.findViewById(R.id.btn_reason_1);
        Button reason2 = view.findViewById(R.id.btn_reason_2);
        Button reason3 = view.findViewById(R.id.btn_reason_3);
        Button reason4 = view.findViewById(R.id.btn_reason_4);
        Button reason5 = view.findViewById(R.id.btn_reason_5);
        Button reason6 = view.findViewById(R.id.btn_reason_6);
        Button skipBtn = view.findViewById(R.id.btn_skip);
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
        skipBtn.setOnClickListener(v -> skip());
        resumeBtn.setOnClickListener(v -> quickResume());

        if (mode.isRequired()) {
            skipBtn.setVisibility(View.GONE);
            setCancelable(false);
        } else {
            skipBtn.setVisibility(View.VISIBLE);
            setCancelable(true);
        }

        AlertDialog dialog = builder.create();
        if (mode.isRequired()) {
            dialog.setCanceledOnTouchOutside(false);
        }
        return dialog;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (!settledByUser && mode.isSkippable() && listener != null) {
            listener.onReasonSkipped();
        }
    }

    private void selectReason(String reason) {
        settledByUser = true;
        if (listener != null) {
            listener.onReasonSelected(reason);
        }
        dismissAllowingStateLoss();
    }

    private void skip() {
        settledByUser = true;
        if (listener != null) {
            listener.onReasonSkipped();
        }
        dismissAllowingStateLoss();
    }

    private void quickResume() {
        settledByUser = true;
        if (listener != null) {
            listener.onReasonQuickResume();
        }
        dismissAllowingStateLoss();
    }
}
