package com.example.findlink;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Firestore-এর সব ইউজার/পোস্ট/চ্যাট/ক্লেইম মুছে ফেলে — নতুন করে শুরু করার জন্য।
 * (Firebase Auth-এর অ্যাকাউন্ট ক্লায়েন্ট থেকে মুছতে যায় না; সেগুলো Console থেকে মোছা যায়।)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WipeAllDataTest {

    private static final String[] COLLECTIONS = {"users", "items", "chats", "claims", "messages"};

    @Test
    public void wipeEverything() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (String collection : COLLECTIONS) {
            wipe(collection);
        }

        for (String collection : COLLECTIONS) {
            QuerySnapshot snapshot = Tasks.await(db.collection(collection).limit(1).get());
            assertEquals(collection + " should be empty", 0, snapshot.size());
        }
    }

    private void wipe(String collection) throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        while (true) {
            QuerySnapshot snapshot = Tasks.await(db.collection(collection).limit(100).get());
            if (snapshot.isEmpty()) {
                return;
            }
            WriteBatch batch = db.batch();
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                if ("chats".equals(collection)) {
                    QuerySnapshot messages = Tasks.await(
                            document.getReference().collection("messages").limit(200).get());
                    for (DocumentSnapshot message : messages.getDocuments()) {
                        batch.delete(message.getReference());
                    }
                }
                batch.delete(document.getReference());
            }
            Tasks.await(batch.commit());
        }
    }
}
