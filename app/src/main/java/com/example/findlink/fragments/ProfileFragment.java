package com.example.findlink.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.findlink.R;
import com.example.findlink.activities.InboxActivity;
import com.example.findlink.activities.LoginActivity;
import com.example.findlink.adapters.ItemAdapter;
import com.example.findlink.models.ItemModel;
import com.example.findlink.util.ChatRepository;
import com.example.findlink.util.UserProfileSync;
import com.example.findlink.util.WindowInsetsUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail, tvProfileInitials, tvMyPostsCount;
    private Button btnLogout, btnOpenInbox;
    private RecyclerView recyclerViewMyPosts;
    private View tvEmptyMyPosts;

    private ItemAdapter itemAdapter;
    private List<ItemModel> myPostsList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        WindowInsetsUtil.applyTopPadding(view);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileInitials = view.findViewById(R.id.tvProfileInitials);
        tvMyPostsCount = view.findViewById(R.id.tvMyPostsCount);
        tvEmptyMyPosts = view.findViewById(R.id.tvEmptyMyPosts);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnOpenInbox = view.findViewById(R.id.btnOpenInbox);
        recyclerViewMyPosts = view.findViewById(R.id.recyclerViewMyPosts);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // RecyclerView সেটআপ
        recyclerViewMyPosts.setHasFixedSize(true);
        recyclerViewMyPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        myPostsList = new ArrayList<>();
        itemAdapter = new ItemAdapter(myPostsList);
        recyclerViewMyPosts.setAdapter(itemAdapter);

        loadProfileData();

        // ইনবক্স বাটন ক্লিক করার লজিক
        btnOpenInbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), InboxActivity.class);
                startActivity(intent);
            }
        });

        // লগআউট বাটন
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                // লগআউটের পর ব্যাক চাপলে যেন পুরনো (লগইন করা) স্ক্রিনে ফিরে না যায়
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });

        return view;
    }

    /**
     * ট্যাব থেকে ফিরে এলে ডেটা রিফ্রেশ করা হয় (ফ্র্যাগমেন্ট ক্যাশে থাকায় view নতুন করে তৈরি হয় না)।
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadProfileData();
        }
    }

    private void loadProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String userId = currentUser.getUid();

        // পুরোনো/অসম্পূর্ণ অ্যাকাউন্টেও নাম + জিমেইল নিশ্চিত করা
        UserProfileSync.ensureProfile();

        // ১. ফায়ারবেস থেকে ইউজারের নাম ও ইমেইল আনা
        db.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!isAdded()) {
                            return;
                        }
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            String name = document != null && document.exists() ? document.getString("name") : null;
                            String email = document != null && document.exists() ? document.getString("email") : null;

                            String authEmail = currentUser.getEmail() == null ? "" : currentUser.getEmail();
                            String finalEmail = (email == null || email.trim().isEmpty()) ? authEmail : email;
                            String display = UserProfileSync.displayName(name, finalEmail);

                            tvProfileName.setText(display);
                            tvProfileEmail.setText(finalEmail);
                            tvProfileInitials.setText(UserProfileSync.initialsOf(display));
                        }
                    }
                });

        // ২. নিজের করা পোস্টগুলো লোড করা
        loadMyPosts(userId);

        // ৩. অপঠিত মেসেজ থাকলে Inbox বাটনে সংখ্যা দেখানো
        ChatRepository.countUnread(userId, unread -> {
            if (!isAdded()) {
                return;
            }
            btnOpenInbox.setText(unread > 0 ? "Inbox · " + unread + " new" : "Inbox");
        });
    }

    private void loadMyPosts(String userId) {
        db.collection("items").whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            myPostsList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ItemModel item = document.toObject(ItemModel.class);
                                myPostsList.add(item);
                            }
                            itemAdapter.notifyDataSetChanged();

                            tvMyPostsCount.setText(myPostsList.size() + (myPostsList.size() == 1 ? " item" : " items"));
                            tvEmptyMyPosts.setVisibility(myPostsList.isEmpty() ? View.VISIBLE : View.GONE);

                            if (myPostsList.isEmpty()) {
                                Toast.makeText(getContext(), "You haven't posted anything yet.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Error loading posts", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * নাম থেকে অ্যাভাটারের জন্য প্রথম দুটি অক্ষর তৈরি করে।
     */
    private String buildInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "FL";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.length() == 0 ? "FL" : initials.toString();
    }
}

