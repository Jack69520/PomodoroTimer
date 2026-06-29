package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.data.model.ProfileUiState;
import com.skyinit.pomodorotimer.data.repository.SettingsManager;
import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.service.AppBlockingService;
import com.skyinit.pomodorotimer.ui.account.AccountActivity;
import com.skyinit.pomodorotimer.util.AppBlockingServiceUtils;
import com.skyinit.pomodorotimer.util.PermissionUtils;
import com.skyinit.pomodorotimer.R;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import java.io.File;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.res.Configuration;

public class ProfileFragment extends Fragment {
    private TextView countText;
    private ProfileViewModel viewModel;
    
    // 账户相关变量
    private LinearLayout accountSection;
    private ImageView accountAvatar;
    private TextView accountNickname;
    private TextView accountId;

    // 应用屏蔽相关变量
    private Switch appBlockingSwitch;
    private TextView appBlockingDescription;
    private Button btnAppBlockingSettings;
    private static final int REQUEST_USAGE_STATS = 1001;
    private static final int REQUEST_OVERLAY_PERMISSION = 1002;
    private static final int REQUEST_QUERY_ALL_PACKAGES = 1003;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App app = (App) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(ProfileViewModel.class);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 添加设置按钮
        LinearLayout settingsButton = view.findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
            // 添加简单的进入动画
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        LinearLayout devLabButton = view.findViewById(R.id.btn_dev_lab);
        devLabButton.setOnClickListener(v -> showDevLabConfirmDialog());

        // 添加关于按钮
        LinearLayout aboutButton = view.findViewById(R.id.btn_about);
        aboutButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AboutActivity.class);
            startActivity(intent);
            // 添加简单的进入动画
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 添加常见问题按钮
        LinearLayout faqButton = view.findViewById(R.id.btn_faq);
        faqButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FAQActivity.class);
            startActivity(intent);
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 初始化应用屏蔽功能
        initAppBlockingFeatures(view);
        
        // 初始化账户功能
        initAccountFeatures(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        countText = view.findViewById(R.id.count_text);
        
        viewModel.getTotalCompletedCount().observe(getViewLifecycleOwner(), count -> {
            if (countText == null) {
                return;
            }
            if (count != null && count > 0) {
                countText.setVisibility(View.VISIBLE);
                countText.setText(getString(R.string.profile_label_total_focus, count));
            } else {
                countText.setVisibility(View.GONE);
            }
        });

        viewModel.getProfileUiState().observe(getViewLifecycleOwner(), this::bindProfileUiState);
        viewModel.refresh();
    }

    private void bindProfileUiState(ProfileUiState state) {
        if (state == null || accountNickname == null) {
            return;
        }
        accountNickname.setText(state.nickname);
        accountId.setText(state.idLabel);
        if (state.hasAvatar) {
            loadUserAvatar(state.avatarPath);
            accountAvatar.setOnClickListener(v -> openAvatarPreview(state.avatarPath));
        } else {
            accountAvatar.setImageResource(R.drawable.ic_default_avatar);
            accountAvatar.setOnClickListener(null);
        }
    }

    private SettingsManager getSettings() {
        return viewModel.getSettingsManager();
    }




    // 应用屏蔽功能相关方法
    private void initAppBlockingFeatures(View view) {
        appBlockingSwitch = view.findViewById(R.id.app_blocking_switch);
        appBlockingDescription = view.findViewById(R.id.app_blocking_description);
        btnAppBlockingSettings = view.findViewById(R.id.btn_app_blocking_settings);

        // 根据权限状态设置初始状态
        updateAppBlockingUI();

        // 设置开关监听器
        appBlockingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkAndRequestPermissions();
            } else {
                getSettings().setAppBlockingEnabled(false);
                updateAppBlockingUI();
                stopAppBlockingService();
            }
        });

        // 设置管理按钮监听器
        btnAppBlockingSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AppBlockingManagementActivity.class);
            startActivity(intent);
            // 添加简单的进入动画
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

    }

    private void updateAppBlockingUI() {
        boolean hasAllPermissions = PermissionUtils.hasAllAppBlockingPermissions(requireContext());
        boolean isEnabled = hasAllPermissions && getSettings().isAppBlockingEnabled();
        
        // 如果权限丢失但设置仍为启用，则关闭设置
        if (!hasAllPermissions && getSettings().isAppBlockingEnabled()) {
            getSettings().setAppBlockingEnabled(false);
            stopAppBlockingService();
            isEnabled = false;
        }
        
        // 根据权限状态设置开关状态
        appBlockingSwitch.setChecked(isEnabled);
        btnAppBlockingSettings.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        
        if (isEnabled) {
            appBlockingDescription.setText(R.string.blocking_description_enabled);
        } else if (!hasAllPermissions) {
            appBlockingDescription.setText(R.string.blocking_description_need_permission);
        } else {
            appBlockingDescription.setText(R.string.blocking_description_default);
        }
    }

    private void checkAndRequestPermissions() {
        if (PermissionUtils.hasAllAppBlockingPermissions(requireContext())) {
            enableAppBlocking();
        } else {
            showPermissionDialog();
        }
    }

    private void showPermissionDialog() {
        String missingPermissions = PermissionUtils.getMissingPermissionDescription(requireContext());
        
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.common_dialog_permission_title)
            .setMessage(getString(R.string.blocking_dialog_permission_message, missingPermissions))
            .setPositiveButton(R.string.confirm, (dialog, which) -> requestPermissions())
            .setNegativeButton(R.string.cancel, (dialog, which) -> {
                appBlockingSwitch.setChecked(false);
            })
            .show();
    }

    private void requestPermissions() {
        if (!PermissionUtils.hasUsageStatsPermission(requireContext())) {
            PermissionUtils.requestUsageStatsPermission(requireActivity(), REQUEST_USAGE_STATS);
        } else if (!PermissionUtils.hasOverlayPermission(requireContext())) {
            PermissionUtils.requestOverlayPermission(requireActivity(), REQUEST_OVERLAY_PERMISSION);
        } else if (!PermissionUtils.hasQueryAllPackagesPermission(requireContext())) {
            PermissionUtils.requestQueryAllPackagesPermission(requireActivity(), REQUEST_QUERY_ALL_PACKAGES);
        } else {
            enableAppBlocking();
        }
    }

    private void enableAppBlocking() {
        viewModel.setAppBlockingEnabled(true);
        updateAppBlockingUI();
        startAppBlockingService();
        Toast.makeText(getContext(), R.string.blocking_toast_enabled, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 检查并同步服务状态
     */
    private void checkAndSyncServiceStatus() {
        AppBlockingServiceUtils.syncServiceStatus(requireContext());
    }

    private void startAppBlockingService() {
        Intent intent = new Intent(requireContext(), AppBlockingService.class);
        intent.putExtra("action", "start_blocking");
        requireContext().startService(intent);
    }

    private void stopAppBlockingService() {
        Intent intent = new Intent(requireContext(), AppBlockingService.class);
        intent.putExtra("action", "stop_blocking");
        requireContext().startService(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_USAGE_STATS || 
            requestCode == REQUEST_OVERLAY_PERMISSION || 
            requestCode == REQUEST_QUERY_ALL_PACKAGES) {
            
            // 延迟检查权限，给用户时间完成授权
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (PermissionUtils.hasAllAppBlockingPermissions(requireContext())) {
                    enableAppBlocking();
                } else {
                    appBlockingSwitch.setChecked(false);
                    Toast.makeText(getContext(), R.string.blocking_toast_permission_failed, Toast.LENGTH_LONG).show();
                }
            }, 1000);
        }
    }

    private void showDevLabConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_dev_lab)
                .setMessage(R.string.blocking_dialog_dev_lab_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    Intent intent = new Intent(getActivity(), DevLabActivity.class);
                    startActivity(intent);
                    // 添加简单的进入动画
                    getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    // 初始化账户功能
    private void initAccountFeatures(View view) {
        accountSection = view.findViewById(R.id.account_section);
        accountAvatar = view.findViewById(R.id.account_avatar);
        accountNickname = view.findViewById(R.id.account_nickname);
        accountId = view.findViewById(R.id.account_id);
        
        // 设置账户区域点击事件
        accountSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AccountActivity.class);
            startActivity(intent);
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 初始化功能按钮的对比色（在统一圆角背景基础上加色调）
        updateFunctionButtonColors(view);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
        // 更新应用屏蔽功能UI状态（检查权限变化）
        if (appBlockingSwitch != null) {
            updateAppBlockingUI();
            // 同步服务状态
            checkAndSyncServiceStatus();
        }
        // 应用主题背景
        applyThemeToFragment();
        // 重新应用按钮对比色，防止主题变化后颜色过近
        if (getView() != null) {
            updateFunctionButtonColors(getView());
        }
    }

    // 根据当前主题颜色动态为功能按钮分配对比明显的背景色
    private void updateFunctionButtonColors(View root) {
        try {
            SettingsManager settingsManager = getSettings();
            int themeColorRes = settingsManager.getThemeColor();
            int themeColor = getResources().getColor(themeColorRes);

            // 计算4个与主题色区分明显的色相（+120/+180/+240/+300）
            float[] hsv = new float[3];
            android.graphics.Color.colorToHSV(themeColor, hsv);
            float baseHue = hsv[0];
            float s = Math.max(0.35f, hsv[1]);
            float v = Math.max(0.75f, hsv[2]);

            int[] candidateColors = new int[] {
                    android.graphics.Color.HSVToColor(new float[]{(baseHue + 120f) % 360f, s, v}),
                    android.graphics.Color.HSVToColor(new float[]{(baseHue + 180f) % 360f, s, v}),
                    android.graphics.Color.HSVToColor(new float[]{(baseHue + 240f) % 360f, s, v}),
                    android.graphics.Color.HSVToColor(new float[]{(baseHue + 300f) % 360f, s, v})
            };

            int[] btnIds = new int[] { R.id.btn_settings, R.id.btn_dev_lab, R.id.btn_about, R.id.btn_faq };

            for (int i = 0; i < btnIds.length; i++) {
                View btn = root.findViewById(btnIds[i]);
                if (btn == null) continue;
                int color = candidateColors[i % candidateColors.length];
                androidx.core.view.ViewCompat.setBackgroundTintList(btn,
                        android.content.res.ColorStateList.valueOf(color));
                btn.setElevation(6f);
            }
        } catch (Exception ignored) {}
    }
    
    // 加载用户头像（圆形显示）
    private void loadUserAvatar(String avatarPath) {
        try {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(avatarPath);
                if (bitmap != null) {
                    Bitmap circular = createCircularBitmap(bitmap);
                    accountAvatar.setImageBitmap(circular);
                    return;
                }
            }
        } catch (Exception e) {
            // 如果加载失败，使用默认头像
        }
        accountAvatar.setImageResource(R.drawable.ic_default_avatar);
    }

    // 生成圆形位图
    private Bitmap createCircularBitmap(Bitmap source) {
        try {
            int size = Math.min(source.getWidth(), source.getHeight());
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(output);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setAntiAlias(true);
            float radius = size / 2f;
            android.graphics.Path path = new android.graphics.Path();
            path.addCircle(radius, radius, radius, android.graphics.Path.Direction.CCW);
            canvas.save();
            canvas.clipPath(path);
            int left = (size - source.getWidth()) / 2;
            int top = (size - source.getHeight()) / 2;
            canvas.drawBitmap(source, left, top, paint);
            canvas.restore();
            return output;
        } catch (Exception e) {
            return source;
        }
    }

    // 打开头像原图预览
    private void openAvatarPreview(String avatarPath) {
        try {
            Intent intent = new Intent(getActivity(), ImagePreviewActivity.class);
            intent.putExtra("image_path", avatarPath);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 当配置变化时（如深色模式切换），重新应用主题
        applyThemeToFragment();
    }
    
    private void applyThemeToFragment() {
        if (getActivity() instanceof BaseActivity) {
            BaseActivity baseActivity = (BaseActivity) getActivity();
            baseActivity.applyTheme();
        }
    }

}
