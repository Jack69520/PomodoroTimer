package com.skyinit.pomodorotimer.ui.profile;

import android.content.Context;

import com.skyinit.pomodorotimer.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * FAQ 内容与分组定义，供 {@link FAQActivity} 动态构建界面。
 */
public final class FaqData {

    public static final class FaqItem {
        public final String question;
        public final String answer;

        public FaqItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    public static final class FaqSection {
        public final String title;
        public final int iconRes;
        public final List<FaqItem> items;

        public FaqSection(String title, int iconRes, List<FaqItem> items) {
            this.title = title;
            this.iconRes = iconRes;
            this.items = items;
        }
    }

    private FaqData() {
    }

    public static List<FaqSection> getSections(Context context) {
        return buildSections(context);
    }

    private static FaqItem item(Context ctx, int questionRes, int answerRes) {
        return new FaqItem(ctx.getString(questionRes), ctx.getString(answerRes));
    }

    private static List<FaqSection> buildSections(Context ctx) {
        return Collections.unmodifiableList(Arrays.asList(
                new FaqSection(ctx.getString(R.string.faq_section_timer), R.drawable.ic_timer, Arrays.asList(
                        item(ctx, R.string.faq_timer_q_how_to_start, R.string.faq_timer_a_how_to_start),
                        item(ctx, R.string.faq_timer_q_need_login, R.string.faq_timer_a_need_login),
                        item(ctx, R.string.faq_timer_q_duration, R.string.faq_timer_a_duration),
                        item(ctx, R.string.faq_timer_q_quick_duration, R.string.faq_timer_a_quick_duration),
                        item(ctx, R.string.faq_timer_q_pause_rules, R.string.faq_timer_a_pause_rules),
                        item(ctx, R.string.faq_timer_q_after_study, R.string.faq_timer_a_after_study),
                        item(ctx, R.string.faq_timer_q_long_break, R.string.faq_timer_a_long_break),
                        item(ctx, R.string.faq_timer_q_leave_page, R.string.faq_timer_a_leave_page),
                        item(ctx, R.string.faq_timer_q_settings_options, R.string.faq_timer_a_settings_options)
                )),
                new FaqSection(ctx.getString(R.string.faq_section_task), R.drawable.ic_edit, Arrays.asList(
                        item(ctx, R.string.faq_task_q_crud, R.string.faq_task_a_crud),
                        item(ctx, R.string.faq_task_q_subtasks, R.string.faq_task_a_subtasks),
                        item(ctx, R.string.faq_task_q_recurring, R.string.faq_task_a_recurring),
                        item(ctx, R.string.faq_task_q_pomodoro_progress, R.string.faq_task_a_pomodoro_progress),
                        item(ctx, R.string.faq_task_q_features, R.string.faq_task_a_features),
                        item(ctx, R.string.faq_task_q_overdue, R.string.faq_task_a_overdue),
                        item(ctx, R.string.faq_task_q_auto_delete, R.string.faq_task_a_auto_delete)
                )),
                new FaqSection(ctx.getString(R.string.faq_section_statistics), R.drawable.ic_statistics, Arrays.asList(
                        item(ctx, R.string.faq_statistics_q_view, R.string.faq_statistics_a_view),
                        item(ctx, R.string.faq_statistics_q_calendar, R.string.faq_statistics_a_calendar),
                        item(ctx, R.string.faq_statistics_q_total_count, R.string.faq_statistics_a_total_count),
                        item(ctx, R.string.faq_statistics_q_empty_buttons, R.string.faq_statistics_a_empty_buttons)
                )),
                new FaqSection(ctx.getString(R.string.faq_section_blocking), R.drawable.ic_settings, Arrays.asList(
                        item(ctx, R.string.faq_blocking_q_purpose, R.string.faq_blocking_a_purpose),
                        item(ctx, R.string.faq_blocking_q_manage, R.string.faq_blocking_a_manage),
                        item(ctx, R.string.faq_blocking_q_default_whitelist, R.string.faq_blocking_a_default_whitelist),
                        item(ctx, R.string.faq_blocking_q_troubleshoot, R.string.faq_blocking_a_troubleshoot)
                )),
                new FaqSection(ctx.getString(R.string.faq_section_account), R.drawable.ic_profile, Arrays.asList(
                        item(ctx, R.string.faq_account_q_local_vs_registered, R.string.faq_account_a_local_vs_registered),
                        item(ctx, R.string.faq_account_q_auth, R.string.faq_account_a_auth),
                        item(ctx, R.string.faq_account_q_profile, R.string.faq_account_a_profile),
                        item(ctx, R.string.faq_account_q_landscape, R.string.faq_account_a_landscape),
                        item(ctx, R.string.faq_account_q_notifications, R.string.faq_account_a_notifications)
                )),
                new FaqSection(ctx.getString(R.string.faq_section_dev_lab), R.drawable.ic_lab, Arrays.asList(
                        item(ctx, R.string.faq_dev_lab_q_purpose, R.string.faq_dev_lab_a_purpose)
                ))
        ));
    }
}
