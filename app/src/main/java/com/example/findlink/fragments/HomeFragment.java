package com.example.findlink.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.findlink.MainActivity;
import com.example.findlink.R;
import com.example.findlink.activities.InboxActivity;
import com.example.findlink.adapters.ItemAdapter;
import com.example.findlink.models.ItemModel;
import com.example.findlink.util.ChatRepository;
import com.example.findlink.util.WindowInsetsUtil;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * হোম ড্যাশবোর্ড: greeting, লাইভ স্ট্যাটস, কুইক অ্যাকশন, ক্যাটাগরি ফিল্টার,
 * ব্রাউজ কার্ড ও অফিসে জমা আইটেম — সব এক স্ক্রিনে।
 */
public class HomeFragment extends Fragment {

    private static final String STATE_FEED_OPEN = "state_feed_open";
    private static final String STATE_MODE = "state_feed_mode";
    private static final String STATE_CATEGORY = "state_feed_category";

    private static final int MODE_RECENT = 0;
    private static final int MODE_OLDER = 1;
    private static final int MODE_OFFICE = 2;

    private LinearLayout layoutCategoryMenu, layoutFeed;
    private CardView cardRecent, cardOlder;
    private Button btnBack;
    private TextView tvFeedTitle;

    private RecyclerView recyclerViewHome;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyFeed;

    private TextView tvHomeGreeting, tvHomeUserName, tvHomeAvatar, tvHomeUnreadBadge;
    private TextView tvLostCount, tvFoundCount, tvOfficeCount;
    private TextView tvRecentCount, tvOlderCount, tvOfficeSectionCount, tvMessagesHint;
    private ChipGroup chipGroupCategories;

    private ItemAdapter itemAdapter;
    private List<ItemModel> displayList;
    private List<ItemModel> recentList;
    private List<ItemModel> olderList;
    private List<ItemModel> officeList;
    private List<ItemModel> activeItems;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private int feedMode = MODE_RECENT;
    private String categoryFilter = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        WindowInsetsUtil.applyTopPadding(view.findViewById(R.id.headerLayout));
        WindowInsetsUtil.applyTopPadding(view.findViewById(R.id.feedTopBar));

        layoutCategoryMenu = view.findViewById(R.id.layoutCategoryMenu);
        layoutFeed = view.findViewById(R.id.layoutFeed);
        cardRecent = view.findViewById(R.id.cardRecent);
        cardOlder = view.findViewById(R.id.cardOlder);
        btnBack = view.findViewById(R.id.btnBack);
        tvFeedTitle = view.findViewById(R.id.tvFeedTitle);

        recyclerViewHome = view.findViewById(R.id.recyclerViewHome);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tvEmptyFeed = view.findViewById(R.id.tvEmptyFeed);

        tvHomeGreeting = view.findViewById(R.id.tvHomeGreeting);
        tvHomeUserName = view.findViewById(R.id.tvHomeUserName);
        tvHomeAvatar = view.findViewById(R.id.tvHomeAvatar);
        tvHomeUnreadBadge = view.findViewById(R.id.tvHomeUnreadBadge);
        tvLostCount = view.findViewById(R.id.tvLostCount);
        tvFoundCount = view.findViewById(R.id.tvFoundCount);
        tvOfficeCount = view.findViewById(R.id.tvOfficeCount);
        tvRecentCount = view.findViewById(R.id.tvRecentCount);
        tvOlderCount = view.findViewById(R.id.tvOlderCount);
        tvOfficeSectionCount = view.findViewById(R.id.tvOfficeSectionCount);
        tvMessagesHint = view.findViewById(R.id.tvMessagesHint);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);

        recyclerViewHome.setHasFixedSize(true);
        recyclerViewHome.setLayoutManager(new LinearLayoutManager(getContext()));

        displayList = new ArrayList<>();
        recentList = new ArrayList<>();
        olderList = new ArrayList<>();
        officeList = new ArrayList<>();
        activeItems = new ArrayList<>();

        itemAdapter = new ItemAdapter(displayList);
        recyclerViewHome.setAdapter(itemAdapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupHeader(view);
        setupQuickActions(view);
        setupBrowseCards(view);
        setupCategoryChips();

        btnBack.setOnClickListener(v -> closeFeed());

        if (savedInstanceState != null) {
            feedMode = savedInstanceState.getInt(STATE_MODE, MODE_RECENT);
            categoryFilter = savedInstanceState.getString(STATE_CATEGORY);
            if (savedInstanceState.getBoolean(STATE_FEED_OPEN, false)) {
                openFeed(computeTitle(), false);
            }
        }

        loadDashboard();
        swipeRefreshLayout.setOnRefreshListener(this::loadDashboard);

        return view;
    }

    // ------------------------------------------------------------------ header

    private void setupHeader(View view) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good morning";
        } else if (hour < 17) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }
        tvHomeGreeting.setText(greeting);

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded()) {
                            return;
                        }
                        String name = doc != null && doc.exists() ? doc.getString("name") : null;
                        if (name == null || name.trim().isEmpty()) {
                            String email = mAuth.getCurrentUser().getEmail();
                            name = email != null && email.contains("@")
                                    ? email.substring(0, email.indexOf('@')) : "FindLink user";
                        }
                        tvHomeUserName.setText(name);
                        tvHomeAvatar.setText(ChatRepository.getInitials(name));
                    });
        }

        view.findViewById(R.id.btnHomeInbox).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), InboxActivity.class)));
        view.findViewById(R.id.cardHomeSearch).setOnClickListener(v -> openTab(R.id.nav_search));
    }

    private void loadUnread() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }
        ChatRepository.countUnread(mAuth.getCurrentUser().getUid(), unread -> {
            if (!isAdded()) {
                return;
            }
            tvHomeUnreadBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
            tvHomeUnreadBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
            tvMessagesHint.setText(unread > 0 ? unread + " new message" + (unread == 1 ? "" : "s")
                    : "Chat with users");
        });
    }

    // ------------------------------------------------------------------ actions

    private void setupQuickActions(View view) {
        view.findViewById(R.id.cardReportLost).setOnClickListener(v -> openTab(R.id.nav_add));
        view.findViewById(R.id.cardReportFound).setOnClickListener(v -> openTab(R.id.nav_add));
        view.findViewById(R.id.cardMyPosts).setOnClickListener(v -> openTab(R.id.nav_profile));
        view.findViewById(R.id.cardMessages).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), InboxActivity.class)));
    }

    private void setupBrowseCards(View root) {
        cardRecent.setOnClickListener(v -> {
            feedMode = MODE_RECENT;
            categoryFilter = null;
            openFeed("Recent reports", true);
        });
        cardOlder.setOnClickListener(v -> {
            feedMode = MODE_OLDER;
            categoryFilter = null;
            openFeed("Older reports", true);
        });

        root.findViewById(R.id.cardOfficeItems).setOnClickListener(v -> {
            feedMode = MODE_OFFICE;
            categoryFilter = null;
            openFeed("At lost & found desks", true);
        });
    }

    private void setupCategoryChips() {
        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            String category = null;
            if (id == R.id.chipElectronics) {
                category = "Electronics";
            } else if (id == R.id.chipDocuments) {
                category = "Documents";
            } else if (id == R.id.chipBags) {
                category = "Bags";
            } else if (id == R.id.chipKeys) {
                category = "Keys";
            } else if (id == R.id.chipCards) {
                category = "Cards";
            }

            if (category == null) {
                closeFeed();
                return;
            }
            categoryFilter = category;
            feedMode = MODE_RECENT;
            openFeed(category, true);
        });
    }

    private void openTab(int menuId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).selectTab(menuId);
        }
    }

    // ------------------------------------------------------------------ data

    private void loadDashboard() {
        loadActiveItems();
        loadOfficeItems();
        loadUnread();
    }

    private void loadActiveItems() {
        db.collection("items").whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) {
                        return;
                    }
                    recentList.clear();
                    olderList.clear();
                    activeItems.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ItemModel item = document.toObject(ItemModel.class);
                        activeItems.add(item);
                    }

                    Collections.sort(activeItems, (a, b) ->
                            Long.compare(b.getTimestamp(), a.getTimestamp()));

                    long currentTime = System.currentTimeMillis();
                    long tenMinutes = 10 * 60 * 1000;
                    int lostCount = 0;
                    int foundCount = 0;

                    for (ItemModel item : activeItems) {
                        if ("LOST".equalsIgnoreCase(item.getReportType())) {
                            lostCount++;
                        } else if ("FOUND".equalsIgnoreCase(item.getReportType())) {
                            foundCount++;
                        }

                        boolean isRecent = item.getTimestamp() != 0
                                && currentTime - item.getTimestamp() <= tenMinutes;
                        if (isRecent) {
                            recentList.add(item);
                        } else {
                            olderList.add(item);
                        }
                    }

                    tvLostCount.setText(String.valueOf(lostCount));
                    tvFoundCount.setText(String.valueOf(foundCount));
                    tvRecentCount.setText(recentList.size() == 1
                            ? "1 report in the last 10 minutes"
                            : recentList.size() + " reports in the last 10 minutes");
                    tvOlderCount.setText(olderList.size() == 1
                            ? "1 older report"
                            : olderList.size() + " older reports");

                    if (layoutFeed.getVisibility() == View.VISIBLE) {
                        updateRecyclerViewData();
                    }
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(getContext(), "Error loading posts: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void loadOfficeItems() {
        db.collection("items").whereEqualTo("status", "AT_OFFICE")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) {
                        return;
                    }
                    officeList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        officeList.add(document.toObject(ItemModel.class));
                    }
                    Collections.sort(officeList, (a, b) ->
                            Long.compare(b.getTimestamp(), a.getTimestamp()));

                    tvOfficeCount.setText(String.valueOf(officeList.size()));
                    tvOfficeSectionCount.setText(officeList.isEmpty()
                            ? "Nothing deposited right now"
                            : officeList.size() + (officeList.size() == 1
                                    ? " item is waiting at a desk" : " items are waiting at desks"));
                });
    }

    // ------------------------------------------------------------------ feed

    private void openFeed(String title, boolean updateData) {
        tvFeedTitle.setText(title);
        layoutCategoryMenu.setVisibility(View.GONE);
        layoutFeed.setVisibility(View.VISIBLE);
        if (updateData) {
            updateRecyclerViewData();
        }
    }

    private void closeFeed() {
        layoutFeed.setVisibility(View.GONE);
        layoutCategoryMenu.setVisibility(View.VISIBLE);
    }

    private String computeTitle() {
        if (feedMode == MODE_OLDER) {
            return "Older reports";
        }
        if (feedMode == MODE_OFFICE) {
            return "At lost & found desks";
        }
        return categoryFilter == null ? "Recent reports" : categoryFilter;
    }

    private void updateRecyclerViewData() {
        displayList.clear();

        List<ItemModel> source;
        if (feedMode == MODE_OLDER) {
            source = olderList;
        } else if (feedMode == MODE_OFFICE) {
            source = officeList;
        } else if (categoryFilter != null) {
            source = activeItems;
        } else {
            source = recentList;
        }

        for (ItemModel item : source) {
            if (categoryFilter == null || matchesCategory(item, categoryFilter)) {
                displayList.add(item);
            }
        }

        itemAdapter.notifyDataSetChanged();
        if (tvEmptyFeed != null) {
            tvEmptyFeed.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmptyFeed.setText(feedMode == MODE_OFFICE
                    ? "No items are at a lost & found desk right now."
                    : "No reports here yet.\nTap “Post” to add the first one.");
        }
    }

    private boolean matchesCategory(ItemModel item, String category) {
        if (item.getCategory() == null) {
            return false;
        }
        String value = item.getCategory().trim().toLowerCase(Locale.getDefault());
        String target = category.trim().toLowerCase(Locale.getDefault());
        return value.contains(target) || target.contains(value);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadDashboard();
        }
    }

    /**
     * ব্যাক চাপলে ফিড খোলা থাকলে ড্যাশবোর্ডে ফিরে যায়।
     *
     * @return true হলে ব্যাক ইভেন্ট এখানেই হ্যান্ডেল হয়েছে
     */
    public boolean handleBackPress() {
        if (layoutFeed != null && layoutFeed.getVisibility() == View.VISIBLE) {
            closeFeed();
            return true;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_FEED_OPEN,
                layoutFeed != null && layoutFeed.getVisibility() == View.VISIBLE);
        outState.putInt(STATE_MODE, feedMode);
        outState.putString(STATE_CATEGORY, categoryFilter);
    }
}
