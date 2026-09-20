package com.skyinit.pomodorotimer.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.MainActivity;
import com.skyinit.pomodorotimer.R;
import com.skyinit.pomodorotimer.data.model.ProfileAvatarImage;
import com.skyinit.pomodorotimer.ui.common.ModernPromptDialog;
import com.skyinit.pomodorotimer.util.AppBlockingEnabler;
import com.skyinit.pomodorotimer.util.AppBlockingServiceUtils;
import com.skyinit.pomodorotimer.util.AppLog;

/**
 * 「我的」页：渲染 {@link ProfileUiState}、消费 {@link ProfileEffect}，业务经 Intent 交给 ViewModel。
 */
public class ProfileFragment extends Fragment {

    /** 累计数值中计量单位相对数字的字号比例（约 14–15sp @ 24sp）。 */
    private static final float PROFILE_STAT_UNIT_SCALE = 0.62f;

    private ProfileViewModel viewModel;

    private View accountSection;
    private ImageView accountAvatar;
    private TextView accountNickname;
    private TextView accountId;
    private TextView accountSignature;
    private View statsSummarySection;
    private TextView countText;
    private TextView durationText;
    private SwitchCompat appBlockingSwitch;
    private TextView appBlockingDescription;
    private MaterialButton btnAppBlockingSettings;
    private View btnSettings;
    private View btnDevLab;
    private View dividerBeforeDevLab;
    private View btnFaq;
    private View btnAbout;

    private boolean suppressSwitchCallback;
    @Nullable
    private String currentAvatarPath;

    private final AppBlockingEnabler.Host blockingHost = new AppBlockingEnabler.Host() {
        @Override
        public Activity getActivity() {
            return requireActivity();
        }

        @Override
        public void onBlockingEnabled() {
            if (!isAdded() || viewModel == null) {
                return;
            }
            viewModel.dispatch(ProfileIntent.blockingExternalResult(true));
        }

        @Override
        public void onBlockingEnableFailed() {
            if (!isAdded() || viewModel == null) {
                return;
            }
            viewModel.dispatch(ProfileIntent.blockingExternalResult(false));
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(ProfileViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupClicks();
        observeViewModel();
        viewModel.dispatch(ProfileIntent.refresh());
    }

    private void bindViews(@NonNull View view) {
        accountSection = view.findViewById(R.id.account_section);
        accountAvatar = view.findViewById(R.id.account_avatar);
        accountNickname = view.findViewById(R.id.account_nickname);
        accountId = view.findViewById(R.id.account_id);
        accountSignature = view.findViewById(R.id.account_signature);
        statsSummarySection = view.findViewById(R.id.stats_summary_section);
        countText = view.findViewById(R.id.count_text);
        durationText = view.findViewById(R.id.duration_text);
        appBlockingSwitch = view.findViewById(R.id.app_blocking_switch);
        appBlockingDescription = view.findViewById(R.id.app_blocking_description);
        btnAppBlockingSettings = view.findViewById(R.id.btn_app_blocking_settings);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnDevLab = view.findViewById(R.id.btn_dev_lab);
        dividerBeforeDevLab = view.findViewById(R.id.divider_before_dev_lab);
        btnFaq = view.findViewById(R.id.btn_faq);
        btnAbout = view.findViewById(R.id.btn_about);
    }

    private void setupClicks() {
        accountSection.setOnClickListener(v ->
                viewModel.dispatch(ProfileIntent.openAccount()));

        statsSummarySection.setOnClickListener(v ->
                viewModel.dispatch(ProfileIntent.openStats()));

        btnSettings.setOnClickListener(v ->
                viewModel.dispatch(ProfileIntent.openSettings()));
        btnDevLab.setOnClickListener(v ->
                viewModel.dispatch(ProfileIntent.openDevLab()));
        btnFaq.setOnClickListener(v ->
                viewModel.dispatch(ProfileIntent.openFaq()));
        btnAbout.setOnClickListener(v ->
                viewModel.dispatch(ProfileIntent.openAbout()));

        btnAppBlockingSettings.setOnClickListener(v ->
                viewModel.dispatch(ProfileIntent.openManageBlocking()));

        appBlockingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallback) {
                return;
            }
            viewModel.dispatch(ProfileIntent.toggleBlocking(isChecked));
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getAvatarImage().observe(getViewLifecycleOwner(), this::bindAvatar);
        viewModel.getEffects().observe(getViewLifecycleOwner(), this::handleEffect);
    }

    private void render(@Nullable ProfileUiState state) {
        if (state == null || accountNickname == null) {
            return;
        }

        accountNickname.setText(state.nickname);
        if (state.idLabel == null || state.idLabel.isEmpty()) {
            accountId.setVisibility(View.GONE);
        } else {
            accountId.setVisibility(View.VISIBLE);
            accountId.setText(state.idLabel);
        }

        accountSignature.setText(state.signatureText);
        accountSignature.setAlpha(state.hasSignature ? 1f : 0.65f);

        currentAvatarPath = state.hasAvatar ? state.avatarPath : null;
        if (state.hasAvatar) {
            accountAvatar.setClickable(true);
            accountAvatar.setFocusable(true);
            accountAvatar.setOnClickListener(v ->
                    viewModel.dispatch(ProfileIntent.openAvatarPreview()));
        } else {
            accountAvatar.setImageResource(R.drawable.ic_default_avatar);
            accountAvatar.setOnClickListener(null);
            accountAvatar.setClickable(false);
            accountAvatar.setFocusable(false);
        }

        accountSection.setEnabled(state.accountSectionEnabled);
        accountSection.setClickable(state.accountSectionEnabled);

        if (state.statsReady) {
            countText.setText(formatStyledFocusCount(state.totalCompletedCount));
            durationText.setText(formatStyledDuration(state.totalFocusDurationMs));
        } else {
            countText.setText(R.string.profile_stats_placeholder);
            durationText.setText(R.string.profile_stats_placeholder);
        }
        statsSummarySection.setEnabled(state.statsClickable);
        statsSummarySection.setClickable(state.statsClickable);
        statsSummarySection.setAlpha(state.statsClickable ? 1f : 0.55f);

        appBlockingDescription.setText(state.blockingDescriptionRes);
        boolean showManage = state.blockingChecked && state.manageBlockingEnabled;
        btnAppBlockingSettings.setVisibility(showManage ? View.VISIBLE : View.GONE);
        btnAppBlockingSettings.setEnabled(showManage);
        btnAppBlockingSettings.setAlpha(1f);

        appBlockingSwitch.setEnabled(state.blockingToggleEnabled);
        suppressSwitchCallback = true;
        appBlockingSwitch.setChecked(state.blockingChecked);
        suppressSwitchCallback = false;

        int devLabVisibility = state.devLabVisible ? View.VISIBLE : View.GONE;
        btnDevLab.setVisibility(devLabVisibility);
        dividerBeforeDevLab.setVisibility(devLabVisibility);
    }

    private void bindAvatar(@Nullable ProfileAvatarImage image) {
        if (accountAvatar == null) {
            return;
        }
        if (image == null || image.bitmap == null) {
            if (currentAvatarPath == null) {
                accountAvatar.setImageResource(R.drawable.ic_default_avatar);
            }
            return;
        }
        if (currentAvatarPath == null || !currentAvatarPath.equals(image.path)) {
            return;
        }
        accountAvatar.setImageBitmap(image.bitmap);
    }

    private void handleEffect(@Nullable ProfileEffect effect) {
        if (effect == null || !isAdded()) {
            return;
        }
        switch (effect.type) {
            case SHOW_TOAST:
                Toast.makeText(requireContext(), effect.toastRes,
                        effect.toastLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
                break;
            case START_ACTIVITY:
                if (effect.activityClass != null) {
                    openActivity(effect.activityClass);
                }
                break;
            case NAVIGATE_TO_STATISTICS:
                Activity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).navigateToStatisticsTab();
                }
                break;
            case REQUEST_ENABLE_BLOCKING:
                AppBlockingEnabler.tryEnable(requireActivity(), blockingHost);
                break;
            case OPEN_IMAGE_PREVIEW:
                openAvatarPreview(effect.imagePath);
                break;
            case SHOW_DEV_LAB_CONFIRM:
                showDevLabConfirmDialog();
                break;
            case REQUIRE_AUTH:
                // 访客拨开开关后若状态尚未回刷，关闭弹窗前强制复位，避免 UI 卡在开启。
                if (appBlockingSwitch != null && appBlockingSwitch.isChecked()) {
                    suppressSwitchCallback = true;
                    appBlockingSwitch.setChecked(false);
                    suppressSwitchCallback = false;
                }
                com.skyinit.pomodorotimer.ui.auth.AuthGate.show(requireActivity());
                break;
            default:
                break;
        }
    }

    /**
     * 由 {@link MainActivity} 在权限页返回或快捷方式启用后回调。
     *
     * @param success true=启用成功，false=失败；null=仅刷新状态（无 Toast）
     */
    public void onExternalBlockingChanged(@Nullable Boolean success) {
        if (viewModel == null) {
            return;
        }
        if (success == null) {
            viewModel.onBlockingExternalChanged();
        } else {
            viewModel.dispatch(ProfileIntent.blockingExternalResult(success));
        }
    }

    /** 兼容旧调用：仅刷新，不弹 Toast。 */
    public void onExternalBlockingChanged() {
        onExternalBlockingChanged(null);
    }

    private void openActivity(@NonNull Class<? extends Activity> activityClass) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        startActivity(new Intent(activity, activityClass));
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void openAvatarPreview(@Nullable String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()) {
            return;
        }
        try {
            Intent intent = new Intent(requireActivity(), ImagePreviewActivity.class);
            intent.putExtra("image_path", avatarPath);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            AppLog.w("ProfileFragment", "Failed to open avatar preview", e);
        }
    }

    private void showDevLabConfirmDialog() {
        ModernPromptDialog.builder(requireContext())
                .icon(R.drawable.ic_lab)
                .accent(ModernPromptDialog.Accent.BRAND)
                .title(R.string.title_dev_lab)
                .message(R.string.blocking_dialog_dev_lab_message)
                .primary(R.string.confirm, () -> openActivity(DevLabActivity.class))
                .tertiary(R.string.cancel, null)
                .show();
    }

    @NonNull
    private CharSequence formatStyledFocusCount(int count) {
        String text = getString(R.string.profile_label_total_focus, count);
        return applyStatUnitStyle(text, getString(R.string.profile_unit_times));
    }

    @NonNull
    private CharSequence formatStyledDuration(long durationMs) {
        return applyStatUnitStyle(
                formatDuration(durationMs),
                getString(R.string.home_duration_unit_hour),
                getString(R.string.home_duration_unit_minute));
    }

    @NonNull
    private String formatDuration(long durationMs) {
        long minutes = Math.max(0L, durationMs) / (1000L * 60L);
        long hours = minutes / 60L;
        minutes = minutes % 60L;
        // 超长累计：省略分钟，优先保证单行可读
        if (hours >= 100L) {
            return getString(R.string.format_duration_hours, hours);
        }
        if (hours > 0L) {
            return getString(R.string.format_duration_hours_minutes, hours, minutes);
        }
        return getString(R.string.format_duration_minutes, minutes);
    }

    /**
     * 缩小并弱化计量单位字号/颜色，数字保持 TextView 主色与字号。
     */
    @NonNull
    private CharSequence applyStatUnitStyle(@NonNull String text, @NonNull String... units) {
        SpannableString spannable = new SpannableString(text);
        int unitColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        for (String unit : units) {
            if (unit.isEmpty()) {
                continue;
            }
            int start = 0;
            while ((start = text.indexOf(unit, start)) >= 0) {
                int end = start + unit.length();
                spannable.setSpan(
                        new RelativeSizeSpan(PROFILE_STAT_UNIT_SCALE),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(
                        new ForegroundColorSpan(unitColor),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = end;
            }
        }
        return spannable;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.dispatch(ProfileIntent.refresh());
        AppBlockingServiceUtils.syncStandaloneServiceStatus(requireContext());
        applyThemeToFragment();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyThemeToFragment();
    }

    private void applyThemeToFragment() {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).applyTheme();
        }
    }
}
