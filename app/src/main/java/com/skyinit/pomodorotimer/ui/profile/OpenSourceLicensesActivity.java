package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.R;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OpenSourceLicensesActivity extends BaseActivity {

    private static final String APACHE_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0";

    private static final float COLUMN_WEIGHT_NAME = 3f;
    private static final float COLUMN_WEIGHT_LICENSE = 2f;

    private static final class OpenSourceLibrary {
        final String name;
        final String projectUrl;
        final String licenseName;
        final String licenseUrl;

        OpenSourceLibrary(String name, String projectUrl, String licenseName, String licenseUrl) {
            this.name = name;
            this.projectUrl = projectUrl;
            this.licenseName = licenseName;
            this.licenseUrl = licenseUrl;
        }
    }

    /** 与 app/build.gradle.kts 中 implementation 声明一致的运行时直接依赖。 */
    private static final OpenSourceLibrary[] DIRECT_DEPENDENCIES = {
            new OpenSourceLibrary(
                    "AndroidX AppCompat",
                    "https://github.com/androidx/androidx/tree/androidx-main/appcompat",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "Material Components",
                    "https://github.com/material-components/material-components-android",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "AndroidX ConstraintLayout",
                    "https://github.com/androidx/constraintlayout",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "AndroidX Navigation (Fragment)",
                    "https://github.com/androidx/androidx/tree/androidx-main/navigation",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "AndroidX Navigation (UI)",
                    "https://github.com/androidx/androidx/tree/androidx-main/navigation",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "AndroidX Lifecycle (LiveData)",
                    "https://github.com/androidx/androidx/tree/androidx-main/lifecycle",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "AndroidX Lifecycle (ViewModel)",
                    "https://github.com/androidx/androidx/tree/androidx-main/lifecycle",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "AndroidX Room",
                    "https://github.com/androidx/androidx/tree/androidx-main/room",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "AndroidX Core",
                    "https://github.com/androidx/androidx/tree/androidx-main/core",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "MPAndroidChart",
                    "https://github.com/PhilJay/MPAndroidChart",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
            new OpenSourceLibrary(
                    "AndroidX WorkManager",
                    "https://github.com/androidx/androidx/tree/androidx-main/work",
                    "Apache License 2.0",
                    APACHE_LICENSE_URL),
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_source_licenses);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_open_source_licenses));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        TableLayout table = findViewById(R.id.table_open_source);
        addTableHeader(table);
        for (OpenSourceLibrary library : DIRECT_DEPENDENCIES) {
            addTableRow(table, library);
        }

        TextView licenseContentView = findViewById(R.id.tv_apache_license_content);
        String licenseText = loadAssetText("apache_license_2.0.txt");
        if (licenseText == null) {
            Toast.makeText(this, R.string.open_source_license_load_failed, Toast.LENGTH_SHORT).show();
            licenseContentView.setText(R.string.open_source_license_load_failed);
        } else {
            licenseContentView.setText(licenseText);
        }
    }

    private void addTableHeader(TableLayout table) {
        TableRow headerRow = new TableRow(this);
        headerRow.addView(createHeaderCell(getString(R.string.open_source_column_name), COLUMN_WEIGHT_NAME));
        headerRow.addView(createHeaderCell(getString(R.string.open_source_column_license), COLUMN_WEIGHT_LICENSE));
        table.addView(headerRow);

        TableRow dividerRow = new TableRow(this);
        View divider = new View(this);
        TableRow.LayoutParams dividerParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                (int) (1 * getResources().getDisplayMetrics().density));
        dividerParams.span = 2;
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.text_secondary));
        divider.getBackground().setAlpha(80);
        dividerRow.addView(divider);
        table.addView(dividerRow);
    }

    private TextView createHeaderCell(String text, float columnWeight) {
        TextView textView = new TextView(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                columnWeight);
        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        textView.setLayoutParams(params);
        textView.setPadding(padding, padding, padding, padding);
        textView.setText(text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        textView.setTextSize(14);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        return textView;
    }

    private void addTableRow(TableLayout table, OpenSourceLibrary library) {
        TableRow row = new TableRow(this);
        row.addView(createLinkCell(library.name, library.projectUrl, COLUMN_WEIGHT_NAME));
        row.addView(createLinkCell(library.licenseName, library.licenseUrl, COLUMN_WEIGHT_LICENSE));
        table.addView(row);
    }

    private TextView createLinkCell(String text, String url, float columnWeight) {
        TextView textView = new TextView(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                columnWeight);
        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        textView.setLayoutParams(params);
        textView.setPadding(padding, padding, padding, padding);
        textView.setTextSize(14);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        int linkColor = ContextCompat.getColor(this, R.color.primary);
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new NoUnderlineLinkSpan(url, linkColor), 0, text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannable);
        return textView;
    }

    private static final class NoUnderlineLinkSpan extends ClickableSpan {
        private final String url;
        private final int linkColor;

        NoUnderlineLinkSpan(String url, int linkColor) {
            this.url = url;
            this.linkColor = linkColor;
        }

        @Override
        public void onClick(View widget) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            widget.getContext().startActivity(intent);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(linkColor);
            ds.setUnderlineText(false);
        }
    }

    @Nullable
    private String loadAssetText(String fileName) {
        try (InputStream inputStream = getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
