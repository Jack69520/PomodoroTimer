package com.skyinit.pomodorotimer.ui.profile;

import com.skyinit.pomodorotimer.BaseActivity;
import com.skyinit.pomodorotimer.R;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FAQActivity extends BaseActivity {

    private static final long ANIM_DURATION_MS = 220L;

    private LinearLayout faqContainer;
    private String currentQuery = "";

    private final List<SectionViews> sectionViewsList = new ArrayList<>();
    private final List<ItemViews> allItemViews = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.title_faq));
        }

        faqContainer = findViewById(R.id.faq_container);

        if (faqContainer != null) {
            buildFaqUi();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_faq, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.faq_search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterFaq(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFaq(newText);
                return true;
            }
        });
        return true;
    }

    private void buildFaqUi() {
        LayoutInflater inflater = LayoutInflater.from(this);

        TextView intro = new TextView(this);
        intro.setText(R.string.faq_intro);
        intro.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        intro.setTextSize(14f);
        intro.setLineSpacing(dpToPx(4), 1f);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        introLp.bottomMargin = dpToPx(16);
        intro.setLayoutParams(introLp);
        faqContainer.addView(intro);

        List<FaqData.FaqSection> sections = FaqData.getSections(this);
        for (FaqData.FaqSection section : sections) {
            View sectionRoot = inflater.inflate(R.layout.item_faq_section, faqContainer, false);
            SectionViews sectionViews = bindSection(sectionRoot, section);
            sectionViewsList.add(sectionViews);

            LinearLayout questionsContainer = sectionViews.questionsContainer;
            int itemCount = section.items.size();
            for (int i = 0; i < itemCount; i++) {
                FaqData.FaqItem faqItem = section.items.get(i);
                View itemRoot = inflater.inflate(R.layout.item_faq_qa, questionsContainer, false);
                ItemViews itemViews = bindItem(itemRoot, faqItem, sectionViews);
                itemViews.isLastInSection = (i == itemCount - 1);
                if (itemViews.isLastInSection && itemViews.divider != null) {
                    itemViews.divider.setVisibility(View.GONE);
                }
                questionsContainer.addView(itemRoot);
                allItemViews.add(itemViews);
            }

            sectionViews.header.setOnClickListener(v -> toggleSection(sectionViews));
            faqContainer.addView(sectionRoot);
        }

        View bottomSpacer = new View(this);
        bottomSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(28)
        ));
        faqContainer.addView(bottomSpacer);
    }

    private SectionViews bindSection(View sectionRoot, FaqData.FaqSection section) {
        SectionViews views = new SectionViews();
        views.root = sectionRoot;
        views.header = sectionRoot.findViewById(R.id.faq_section_header);
        views.icon = sectionRoot.findViewById(R.id.faq_section_icon);
        views.title = sectionRoot.findViewById(R.id.faq_section_title);
        views.badge = sectionRoot.findViewById(R.id.faq_section_badge);
        views.chevron = sectionRoot.findViewById(R.id.faq_section_chevron);
        views.questionsContainer = sectionRoot.findViewById(R.id.faq_section_questions);

        views.icon.setImageResource(section.iconRes);
        views.title.setText(section.title);
        views.badge.setText(getString(R.string.faq_section_count, section.items.size()));
        views.expanded = true;
        return views;
    }

    private ItemViews bindItem(View itemRoot, FaqData.FaqItem faqItem, SectionViews sectionViews) {
        ItemViews views = new ItemViews();
        views.root = itemRoot;
        views.sectionViews = sectionViews;
        views.questionRow = itemRoot.findViewById(R.id.faq_question_row);
        views.questionText = itemRoot.findViewById(R.id.faq_question);
        views.answerText = itemRoot.findViewById(R.id.faq_answer);
        views.answerContainer = itemRoot.findViewById(R.id.faq_answer_container);
        views.chevron = itemRoot.findViewById(R.id.faq_chevron);
        views.divider = itemRoot.findViewById(R.id.faq_divider);

        views.question = faqItem.question;
        views.answer = faqItem.answer;
        views.questionText.setText(faqItem.question);
        views.answerText.setText(faqItem.answer);
        views.expanded = false;

        views.questionRow.setOnClickListener(v -> toggleItem(views));
        return views;
    }

    private void toggleItem(ItemViews views) {
        if (!currentQuery.isEmpty()) {
            return;
        }
        boolean willExpand = !views.expanded;
        beginTransition(views.sectionViews.questionsContainer);
        setItemExpanded(views, willExpand, true);
    }

    private void toggleSection(SectionViews views) {
        if (!currentQuery.isEmpty()) {
            return;
        }
        boolean willExpand = !views.expanded;
        beginTransition(faqContainer);
        setSectionExpanded(views, willExpand, true);
    }

    private void setSectionExpanded(SectionViews views, boolean expanded, boolean animateChevron) {
        views.expanded = expanded;
        views.questionsContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (animateChevron) {
            animateSectionChevron(views.chevron, expanded);
        } else {
            views.chevron.setRotation(expanded ? 0f : -90f);
        }
    }

    private void setItemExpanded(ItemViews views, boolean expanded, boolean animateChevron) {
        views.expanded = expanded;
        views.answerContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (animateChevron) {
            animateItemChevron(views.chevron, expanded);
        } else {
            views.chevron.setRotation(expanded ? 180f : 0f);
        }
    }

    private void animateItemChevron(ImageView chevron, boolean expanded) {
        float target = expanded ? 180f : 0f;
        if (chevron.getRotation() == target) {
            return;
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(chevron, View.ROTATION, chevron.getRotation(), target);
        animator.setDuration(ANIM_DURATION_MS);
        animator.start();
    }

    private void animateSectionChevron(ImageView chevron, boolean expanded) {
        float target = expanded ? 0f : -90f;
        if (chevron.getRotation() == target) {
            return;
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(chevron, View.ROTATION, chevron.getRotation(), target);
        animator.setDuration(ANIM_DURATION_MS);
        animator.start();
    }

    private void beginTransition(ViewGroup sceneRoot) {
        AutoTransition transition = new AutoTransition();
        transition.setDuration(ANIM_DURATION_MS);
        TransitionManager.beginDelayedTransition(sceneRoot, transition);
    }

    private void filterFaq(String query) {
        currentQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean isSearching = !currentQuery.isEmpty();
        int highlightColor = ContextCompat.getColor(this, R.color.primary);

        for (SectionViews sectionViews : sectionViewsList) {
            boolean sectionHasMatch = false;

            for (int i = 0; i < sectionViews.questionsContainer.getChildCount(); i++) {
                View child = sectionViews.questionsContainer.getChildAt(i);
                ItemViews itemViews = findItemViewsByRoot(child);
                if (itemViews == null) {
                    continue;
                }

                boolean questionMatch = itemViews.question.toLowerCase(Locale.ROOT).contains(currentQuery);
                boolean answerMatch = itemViews.answer.toLowerCase(Locale.ROOT).contains(currentQuery);
                boolean match = !isSearching || questionMatch || answerMatch;

                itemViews.root.setVisibility(match ? View.VISIBLE : View.GONE);
                if (match) {
                    sectionHasMatch = true;
                }

                if (isSearching) {
                    applyHighlight(itemViews.questionText, itemViews.question, currentQuery, highlightColor);
                    applyHighlight(itemViews.answerText, itemViews.answer, currentQuery, highlightColor);
                    setItemExpanded(itemViews, match, false);
                } else {
                    itemViews.questionText.setText(itemViews.question);
                    itemViews.answerText.setText(itemViews.answer);
                    setItemExpanded(itemViews, false, false);
                }
            }

            sectionViews.root.setVisibility(isSearching && !sectionHasMatch ? View.GONE : View.VISIBLE);
            if (isSearching && sectionHasMatch) {
                setSectionExpanded(sectionViews, true, false);
            } else if (!isSearching) {
                setSectionExpanded(sectionViews, true, false);
            }
        }
    }

    private ItemViews findItemViewsByRoot(View root) {
        for (ItemViews itemViews : allItemViews) {
            if (itemViews.root == root) {
                return itemViews;
            }
        }
        return null;
    }

    private void applyHighlight(TextView textView, String fullText, String query, int color) {
        if (query.isEmpty()) {
            textView.setText(fullText);
            return;
        }
        String lower = fullText.toLowerCase(Locale.ROOT);
        SpannableString spannable = new SpannableString(fullText);
        int start = 0;
        while (start < lower.length()) {
            int index = lower.indexOf(query, start);
            if (index < 0) {
                break;
            }
            spannable.setSpan(
                    new ForegroundColorSpan(color),
                    index,
                    index + query.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            start = index + query.length();
        }
        textView.setText(spannable);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private static final class SectionViews {
        View root;
        View header;
        ImageView icon;
        TextView title;
        TextView badge;
        ImageView chevron;
        LinearLayout questionsContainer;
        boolean expanded;
    }

    private static final class ItemViews {
        View root;
        SectionViews sectionViews;
        View questionRow;
        TextView questionText;
        TextView answerText;
        View answerContainer;
        ImageView chevron;
        View divider;
        String question;
        String answer;
        boolean expanded;
        boolean isLastInSection;
    }
}
