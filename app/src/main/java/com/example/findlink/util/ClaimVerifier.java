package com.example.findlink.util;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * দাবি যাচাইয়ের একক জায়গা — যাতে অ্যাপ আর টেস্ট একই নিয়ম ব্যবহার করে।
 *
 * ক্রম:
 * ১. পোস্টে থাকা হ্যান্ডওভার কোড (যাঁর LOST পোস্ট নেই তিনিও এটা দিয়ে যাচাই হতে পারেন)
 * ২. না মিললে দাবিদারের নিজের LOST পোস্টের সিকিউরিটি কোড
 * কোনোটাই না মিললে claim UNVERIFIED হিসেবে যায়।
 */
public final class ClaimVerifier {

    public static final String MATCHED_HANDOVER = "handover";
    public static final String MATCHED_SECURITY = "security";
    public static final String NO_MATCH = "none";
    public static final String ERROR = "error";

    public interface Callback {
        void onResult(boolean verified, String matchedBy);
    }

    private ClaimVerifier() {
    }

    public static void verify(String itemId,
                              String myUid,
                              String handoverCode,
                              String securityCode,
                              Callback callback) {
        if (itemId == null || itemId.isEmpty()) {
            callback.onResult(false, ERROR);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("items").document(itemId).get()
                .addOnSuccessListener(document -> {
                    String storedHandover = document != null && document.exists()
                            ? document.getString("claimCode") : null;

                    if (ClaimCode.matches(storedHandover, handoverCode)) {
                        callback.onResult(true, MATCHED_HANDOVER);
                        return;
                    }
                    if (!SecurityCode.isValid(securityCode)) {
                        callback.onResult(false, NO_MATCH);
                        return;
                    }
                    verifyOwnLostReports(db, myUid, securityCode, callback);
                })
                .addOnFailureListener(e -> callback.onResult(false, ERROR));
    }

    private static void verifyOwnLostReports(FirebaseFirestore db,
                                             String myUid,
                                             String securityCode,
                                             Callback callback) {
        if (myUid == null || myUid.isEmpty()) {
            callback.onResult(false, NO_MATCH);
            return;
        }

        db.collection("items").whereEqualTo("userId", myUid).get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String storedHash = doc.getString("securityHash");
                        String lostItemId = doc.getString("itemId");
                        if (storedHash != null && !storedHash.isEmpty()
                                && SecurityCode.matches(storedHash, lostItemId, securityCode)) {
                            callback.onResult(true, MATCHED_SECURITY);
                            return;
                        }
                    }
                    callback.onResult(false, NO_MATCH);
                })
                .addOnFailureListener(e -> callback.onResult(false, ERROR));
    }

    /** টেস্ট/ডায়াগনস্টিকের জন্য পোস্টের কোডটি পড়া। */
    public static void readHandoverCode(String itemId, CodeCallback callback) {
        FirebaseFirestore.getInstance().collection("items").document(itemId).get()
                .addOnSuccessListener((DocumentSnapshot doc) ->
                        callback.onCode(doc.exists() ? doc.getString("claimCode") : null))
                .addOnFailureListener(e -> callback.onCode(null));
    }

    public interface CodeCallback {
        void onCode(String code);
    }
}
