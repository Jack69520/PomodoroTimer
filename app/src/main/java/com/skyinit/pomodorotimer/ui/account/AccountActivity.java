package com.skyinit.pomodorotimer.ui.account;

import com.skyinit.pomodorotimer.App;
import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.data.model.AccountUiState;
import com.skyinit.pomodorotimer.ui.profile.ImagePreviewActivity;
import com.skyinit.pomodorotimer.R;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AccountActivity extends BaseActivity {
    private static final int REQUEST_CODE_GALLERY = 1001;
    private static final int REQUEST_CODE_CAMERA = 1002;

    private AccountViewModel viewModel;
    private AccountUiState currentState;

    private ImageView accountAvatar;
    private TextView accountId;
    private TextView accountNickname;
    private TextView accountSignature;
    private Button btnChangePassword;
    private Button btnLogout;
    private Button btnDeleteAccount;
    private Button btnRegister;
    private Button btnLogin;

    private File tempImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_account));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        App app = (App) getApplication();
        viewModel = new ViewModelProvider(this, app.getContainer().getViewModelFactory())
                .get(AccountViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();
    }

    private void initViews() {
        accountAvatar = findViewById(R.id.account_avatar);
        accountId = findViewById(R.id.account_id);
        accountNickname = findViewById(R.id.account_nickname);
        accountSignature = findViewById(R.id.account_signature);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);
        btnRegister = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, this::bindUiState);
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getDeleteSuccessDialog().observe(this, unused ->
                new AlertDialog.Builder(this)
                        .setTitle(R.string.account_delete_success_title)
                        .setMessage(R.string.account_delete_success_message)
                        .setPositiveButton(R.string.confirm, null)
                        .show());
        viewModel.getGuardPrompt().observe(this, state -> showBlockingGuardDialog());
    }

    private void bindUiState(AccountUiState state) {
        if (state == null) {
            return;
        }
        currentState = state;

        accountId.setText(state.idLabel);
        accountNickname.setText(state.nickname);
        accountSignature.setText(state.signature);

        if (state.avatarPath != null && !state.avatarPath.isEmpty()) {
            loadUserAvatar(state.avatarPath);
        } else {
            accountAvatar.setImageResource(R.drawable.ic_default_avatar);
        }

        btnChangePassword.setVisibility(state.registered ? View.VISIBLE : View.GONE);
        btnLogout.setVisibility(state.registered ? View.VISIBLE : View.GONE);
        btnDeleteAccount.setVisibility(state.registered ? View.VISIBLE : View.GONE);
        btnRegister.setVisibility(state.registered ? View.GONE : View.VISIBLE);
        btnLogin.setVisibility(state.registered ? View.GONE : View.VISIBLE);

        if (!state.registered) {
            btnRegister.setText(getString(R.string.account_upgrade_to_registered));
        }
        btnLogin.setText(getString(R.string.account_login_existing));

        btnLogout.setEnabled(!state.actionInProgress);
        btnDeleteAccount.setEnabled(!state.actionInProgress);
    }

    private void setupClickListeners() {
        accountAvatar.setOnClickListener(v -> {
            if (currentState == null) return;
            String[] options = getResources().getStringArray(R.array.account_avatar_options);
            new AlertDialog.Builder(this)
                .setTitle(R.string.account_dialog_avatar_title)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            if (currentState.avatarPath != null && !currentState.avatarPath.isEmpty()) {
                                Intent intent = new Intent(this, ImagePreviewActivity.class);
                                intent.putExtra("image_path", currentState.avatarPath);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, R.string.account_toast_no_avatar, Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 1:
                            showAvatarSelectionDialog();
                            break;
                        case 2:
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
        });

        accountNickname.setOnClickListener(v -> {
            if (currentState != null) {
                showNicknameEditDialog();
            }
        });

        accountSignature.setOnClickListener(v -> {
            if (currentState != null) {
                showSignatureEditDialog();
            }
        });

        btnChangePassword.setOnClickListener(v -> {
            if (currentState != null && !currentState.registered) {
                Toast.makeText(this, R.string.account_registered_only, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, ChangePasswordActivity.class));
        });

        btnLogout.setOnClickListener(v -> viewModel.logout());

        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmDialog());

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private void showBlockingGuardDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.account_guard_blocking_title)
                .setMessage(R.string.account_guard_blocking_message)
                .setNegativeButton(R.string.account_guard_cancel_operation, null)
                .setPositiveButton(R.string.account_guard_disable_blocking_continue,
                        (dialog, which) -> viewModel.continueAfterDisablingBlocking())
                .show();
    }

    private void showDeleteAccountConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.account_dialog_delete_title)
                .setMessage(R.string.account_dialog_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.account_dialog_delete_confirm,
                        (dialog, which) -> viewModel.deleteAccount())
                .show();
    }

    private void showAvatarSelectionDialog() {
        String[] options = getResources().getStringArray(R.array.account_pick_avatar_options);
        new AlertDialog.Builder(this)
                .setTitle(R.string.account_dialog_pick_avatar_title)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            selectFromGallery();
                            break;
                        case 1:
                            takePhoto();
                            break;
                        case 2:
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void selectFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    private void takePhoto() {
        try {
            tempImageFile = new File(getExternalFilesDir(null), "temp_avatar.jpg");
            Uri photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", tempImageFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, REQUEST_CODE_CAMERA);
        } catch (Exception e) {
            Toast.makeText(this, R.string.account_toast_camera_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void showNicknameEditDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentState.nickname);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.account_dialog_edit_nickname_title)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String newNickname = input.getText().toString().trim();
                    if (!newNickname.isEmpty() && !newNickname.equals(currentState.nickname)) {
                        viewModel.updateProfile(newNickname, null, null);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showSignatureEditDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(3);
        String currentSignature = currentState.signature != null ? currentState.signature : "";
        input.setText(currentSignature);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.account_dialog_edit_signature_title)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String newSignature = input.getText().toString().trim();
                    String defaultSignature = getString(R.string.account_default_signature);
                    String oldSignature = currentSignature.equals(defaultSignature) ? "" : currentSignature;
                    if (!newSignature.equals(oldSignature)) {
                        viewModel.updateProfile(null, null, newSignature);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_GALLERY && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    processSelectedImage(selectedImage);
                }
            } else if (requestCode == REQUEST_CODE_CAMERA && tempImageFile != null) {
                processSelectedImage(Uri.fromFile(tempImageFile));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            if (bitmap != null && currentState != null) {
                String avatarPath = saveAvatarImage(bitmap);
                if (avatarPath != null) {
                    viewModel.updateProfile(null, avatarPath, null);
                    Bitmap circular = createCircularBitmap(bitmap);
                    accountAvatar.setImageBitmap(circular);
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, R.string.account_toast_image_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private String saveAvatarImage(Bitmap bitmap) {
        if (currentState == null) {
            return null;
        }
        try {
            File avatarDir = new File(getExternalFilesDir(null), "avatars");
            if (!avatarDir.exists()) {
                avatarDir.mkdirs();
            }

            String fileName = "avatar_" + currentState.userId + "_" + System.currentTimeMillis() + ".jpg";
            File avatarFile = new File(avatarDir, fileName);

            FileOutputStream fos = new FileOutputStream(avatarFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();

            return avatarFile.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private void loadUserAvatar(String avatarPath) {
        try {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(avatarPath);
                if (bitmap != null) {
                    accountAvatar.setImageBitmap(createCircularBitmap(bitmap));
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        accountAvatar.setImageResource(R.drawable.ic_default_avatar);
    }

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
}
