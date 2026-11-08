package com.example.findlink.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.findlink.R;
import com.example.findlink.adapters.ItemAdapter;
import com.example.findlink.models.ItemModel;
import com.example.findlink.util.WindowInsetsUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchFragment extends Fragment {

    private static final long SUGGESTION_DEBOUNCE_MS = 250L;
    private static final int MAX_SUGGESTIONS = 6;

    // সার্চ ফিল্টার: সব / শুধু হারানো / শুধু পাওয়া / অফিসে জমা
    private static final int FILTER_ALL = 0;
    private static final int FILTER_LOST = 1;
    private static final int FILTER_FOUND = 2;
    private static final int FILTER_OFFICE = 3;

    private EditText etSearchQuery;
    private Button btnSearch;
    private RecyclerView recyclerViewSearch;
    private ChipGroup chipGroupSuggestions, chipGroupFilters;
    private TextView tvSuggestionsLabel, tvResultCount;
    private View layoutEmptySearch;

    private ItemAdapter itemAdapter;
    private List<ItemModel> searchList;
    private final List<ItemModel> allItems = new ArrayList<>();
    private FirebaseFirestore db;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSuggestionTask;
    private String lastQuery = "";
    private int filter = FILTER_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        WindowInsetsUtil.applyTopPadding(view);

        etSearchQuery = view.findViewById(R.id.etSearchQuery);
        btnSearch = view.findViewById(R.id.btnSearch);
        recyclerViewSearch = view.findViewById(R.id.recyclerViewSearch);
        chipGroupSuggestions = view.findViewById(R.id.chipGroupSuggestions);
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters);
        tvSuggestionsLabel = view.findViewById(R.id.tvSuggestionsLabel);
        tvResultCount = view.findViewById(R.id.tvResultCount);
        layoutEmptySearch = view.findViewById(R.id.layoutEmptySearch);

        recyclerViewSearch.setHasFixedSize(true);
        recyclerViewSearch.setLayoutManager(new LinearLayoutManager(getContext()));

        searchList = new ArrayList<>();
        itemAdapter = new ItemAdapter(searchList);
        recyclerViewSearch.setAdapter(itemAdapter);

        db = FirebaseFirestore.getInstance();

        // একবার সব আইটেম এনে মেমোরিতে রাখা হচ্ছে — টাইপ করার সময় নেটওয়ার্ক কল লাগবে না
        loadAllItems();

        // টাইপ করার সাথে সাথে সাজেশন
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lastQuery = s == null ? "" : s.toString();
                scheduleSuggestions(lastQuery);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch();
                return true;
            }
            return false;
        });

        btnSearch.setOnClickListener(v -> submitSearch());

        setupFilters();

        return view;
    }

    /**
     * All / Lost / Found / At office — ফিল্টার বদলালেই তালিকা আবার সাজানো হয়।
     */
    private void setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipFilterLost) {
                filter = FILTER_LOST;
            } else if (id == R.id.chipFilterFound) {
                filter = FILTER_FOUND;
            } else if (id == R.id.chipFilterOffice) {
                filter = FILTER_OFFICE;
            } else {
                filter = FILTER_ALL;
            }

            hideSuggestions();
            String query = etSearchQuery.getText().toString().trim();
            if (query.isEmpty()) {
                showBrowsableResults();
            } else {
                runSearch(query);
            }
        });
    }

    private void loadAllItems() {
        db.collection("items").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allItems.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ItemModel item = document.toObject(ItemModel.class);
                        if (item != null) {
                            allItems.add(item);
                        }
                    }

                     // ডেটা লোড হওয়ার আগেই টাইপ করা থাকলে সাজেশন আবার হিসাব করা হয়
                    if (isAdded() && !lastQuery.trim().isEmpty()) {
                        showSuggestions(lastQuery);
                    } else if (isAdded() && filter != FILTER_ALL) {
                        showBrowsableResults();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Could not load items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * ট্যাব থেকে ফিরে এলে নতুন পোস্টগুলোও সাজেশন/সার্চে যুক্ত হয়।
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadAllItems();
        }
    }

    private void scheduleSuggestions(String query) {
        if (pendingSuggestionTask != null) {
            debounceHandler.removeCallbacks(pendingSuggestionTask);
        }
        pendingSuggestionTask = () -> showSuggestions(query);
        debounceHandler.postDelayed(pendingSuggestionTask, SUGGESTION_DEBOUNCE_MS);
    }

    /**
     * কোয়েরি অনুযায়ী টাইটেল/ক্যাটাগরি থেকে ডুপ্লিকেট ছাড়া সাজেশন চিপ দেখায়।
     */
    private void showSuggestions(String rawQuery) {
        if (chipGroupSuggestions == null) {
            return;
        }

        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.getDefault());
        chipGroupSuggestions.removeAllViews();

        if (query.isEmpty()) {
            hideSuggestions();
            return;
        }

        Set<String> matches = new LinkedHashSet<>();
        for (ItemModel item : allItems) {
            if (!matchesFilter(item)) {
                continue;
            }
            if (item.getTitle() != null && item.getTitle().toLowerCase(Locale.getDefault()).contains(query)) {
                matches.add(item.getTitle());
            } else if (item.getCategory() != null && item.getCategory().toLowerCase(Locale.getDefault()).contains(query)) {
                matches.add(item.getCategory());
            }
            if (matches.size() >= MAX_SUGGESTIONS) {
                break;
            }
        }

        if (matches.isEmpty()) {
            hideSuggestions();
            return;
        }

        for (String suggestion : matches) {
            Chip chip = new Chip(requireContext());
            chip.setText(suggestion);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setChipCornerRadius(getResources().getDisplayMetrics().density * 18);
            chip.setChipBackgroundColor(getResources().getColorStateList(R.color.colorPrimaryLight, null));
            chip.setTextColor(getResources().getColor(R.color.colorPrimary, null));
            chip.setChipStrokeWidth(0f);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setOnClickListener(v -> {
                etSearchQuery.setText(suggestion);
                etSearchQuery.setSelection(suggestion.length());
                hideSuggestions();
                runSearch(suggestion);
            });
            chipGroupSuggestions.addView(chip);
        }

        tvSuggestionsLabel.setVisibility(View.VISIBLE);
        chipGroupSuggestions.setVisibility(View.VISIBLE);
    }

    private void hideSuggestions() {
        if (tvSuggestionsLabel != null) {
            tvSuggestionsLabel.setVisibility(View.GONE);
        }
        if (chipGroupSuggestions != null) {
            chipGroupSuggestions.setVisibility(View.GONE);
        }
    }

    private void submitSearch() {
        String queryText = etSearchQuery.getText().toString().trim();
        if (queryText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter something to search", Toast.LENGTH_SHORT).show();
            return;
        }
        hideSuggestions();
        runSearch(queryText);
    }

    private void runSearch(String queryText) {
        String query = queryText.trim().toLowerCase(Locale.getDefault());
        searchList.clear();

        for (ItemModel item : allItems) {
            if (matchesFilter(item) && matchesQuery(item, query)) {
                searchList.add(item);
            }
        }
        sortNewestFirst();

        itemAdapter.notifyDataSetChanged();
        updateResultState(queryText, searchList.size());

        // ক্যাশ খালি হলে (এখনো লোড হয়নি) নেটওয়ার্ক থেকে একবার এনে আবার চেষ্টা
        if (allItems.isEmpty()) {
            db.collection("items").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        allItems.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            ItemModel item = document.toObject(ItemModel.class);
                            if (item != null) {
                                allItems.add(item);
                            }
                        }
                        runSearch(queryText);
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Error searching", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * ফিল্টার বদলে খালি কোয়েরিতে শুধু সেই ধরনের রিপোর্টগুলো দেখানো হয়
     * (যেমন "Lost" চেপে সব হারানো জিনিস ব্রাউজ করা)।
     */
    private void showBrowsableResults() {
        searchList.clear();
        for (ItemModel item : allItems) {
            if (matchesFilter(item)) {
                searchList.add(item);
            }
        }
        sortNewestFirst();
        itemAdapter.notifyDataSetChanged();
        updateResultState("", searchList.size());
    }

    private void sortNewestFirst() {
        Collections.sort(searchList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
    }

    private boolean matchesQuery(ItemModel item, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(item.getTitle(), query)
                || containsIgnoreCase(item.getCategory(), query)
                || containsIgnoreCase(item.getLocation(), query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    private boolean matchesFilter(ItemModel item) {
        switch (filter) {
            case FILTER_LOST:
                return "LOST".equalsIgnoreCase(item.getReportType());
            case FILTER_FOUND:
                return "FOUND".equalsIgnoreCase(item.getReportType());
            case FILTER_OFFICE:
                return "AT_OFFICE".equalsIgnoreCase(item.getStatus());
            default:
                return true;
        }
    }

    private String filterLabel() {
        switch (filter) {
            case FILTER_LOST:
                return "lost";
            case FILTER_FOUND:
                return "found";
            case FILTER_OFFICE:
                return "at-office";
            default:
                return "";
        }
    }

    private void updateResultState(String queryText, int count) {
        String type = filterLabel();
        String typePrefix = type.isEmpty() ? "" : type + " ";

        if (count == 0) {
            tvResultCount.setText(queryText.isEmpty()
                    ? "No " + (type.isEmpty() ? "reports" : type + " reports") + " yet"
                    : "No " + typePrefix + "reports match “" + queryText + "”");
            layoutEmptySearch.setVisibility(View.VISIBLE);
            return;
        }

        layoutEmptySearch.setVisibility(View.GONE);
        if (queryText.isEmpty()) {
            tvResultCount.setText("Showing " + count + " " + typePrefix
                    + (count == 1 ? "report" : "reports") + " · newest first");
        } else {
            tvResultCount.setText(count + " " + typePrefix
                    + (count == 1 ? "report matches “" : "reports match “") + queryText + "”");
        }
    }

    @Override
    public void onDestroyView() {
        if (pendingSuggestionTask != null) {
            debounceHandler.removeCallbacks(pendingSuggestionTask);
        }
        super.onDestroyView();
    }
}


