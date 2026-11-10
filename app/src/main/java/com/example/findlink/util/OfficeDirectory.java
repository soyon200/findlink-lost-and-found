package com.example.findlink.util;

import com.example.findlink.models.Office;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * অ্যাপে দেখানো Lost & Found ডেস্কগুলোর তালিকা।
 * (আসল অফিসের ঠিকানা/সময় পরে সহজে বদলানো যাবে — শুধু এই তালিকা সম্পাদনা করলেই হবে।)
 */
public final class OfficeDirectory {

    private static final List<Office> OFFICES = Collections.unmodifiableList(Arrays.asList(
            new Office("campus_security", "Campus Security Office", "Campus",
                    "Main gate, ground floor", "Open 24/7"),
            new Office("student_affairs", "Student Affairs Office", "Campus",
                    "Admin building, 2nd floor", "Sun–Thu, 9:00 AM – 5:00 PM"),
            new Office("police_desk", "Police Lost & Found Desk", "Police",
                    "Local police station, front desk", "Open 24/7"),
            new Office("city_lost_property", "City Lost Property Office", "City",
                    "City corporation annex building", "Sun–Thu, 10:00 AM – 4:00 PM")
    ));

    private OfficeDirectory() {
    }

    public static List<Office> all() {
        return OFFICES;
    }

    public static Office findById(String id) {
        if (id == null) {
            return null;
        }
        for (Office office : OFFICES) {
            if (office.getId().equals(id)) {
                return office;
            }
        }
        return null;
    }
}
