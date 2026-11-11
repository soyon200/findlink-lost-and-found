package com.example.findlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.findlink.util.UserProfileSync;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ProfileSyncTest {

    private static final String PASSWORD = "Test1234!";

    @Test
    public void missingProfileGetsNameAndEmail() throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth.signOut();

        String email = "rahim.uddin99." + System.currentTimeMillis() + "@findlink.test";
        FirebaseUser user = Tasks.await(
                auth.createUserWithEmailAndPassword(email, PASSWORD)).getUser();
        assertNotNull(user);

        // users ডকুমেন্ট নেই — sync নিজেই নাম + ইমেইল বানাবে
        UserProfileSync.ensureProfile();

        DocumentSnapshot doc = null;
        for (int i = 0; i < 20; i++) {
            doc = Tasks.await(db.collection("users").document(user.getUid()).get());
            if (doc.exists() && doc.getString("name") != null) {
                break;
            }
            Thread.sleep(500);
        }

        assertNotNull(doc);
        assertTrue(doc.getString("name").startsWith("Rahim Uddin99"));
        assertEquals(email, doc.getString("email"));
    }
}
