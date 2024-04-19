package me.kyren223.echomobile.utils;

import android.graphics.Bitmap;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.function.Consumer;

public class Cache {
    private static Bitmap pfp;
    public static void getUserProfile(Consumer<UserProfile> callback) {
        if (userProfile != null) callback.accept(userProfile);
        else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            Firebase.getInstance().getUserProfile(user.getUid(), (profile) -> {
                userProfile = profile;
                callback.accept(profile);
            });
        }
    }
    public static void getPfp(Consumer<Bitmap> callback) {
        if (pfp != null) callback.accept(pfp);
        else {
            getUserProfile((profile) -> {
                if (profile == null) return;
                Firebase.getInstance().getProfilePictureBitmap(profile, (bitmap) -> {
                    pfp = bitmap;
                    callback.accept(bitmap);
                });
            });
        }
    }

    private static UserProfile userProfile;
    public static void updateUserProfileCache(UserProfile profile) {
        userProfile = profile;
    }
    public static void updatePfpCache(Bitmap bitmap) {
        pfp = bitmap;
    }

    private static HashMap<String, Label> labels;
    public static void getLabels(Consumer<HashMap<String, Label>> callback) {
        if (labels != null) callback.accept(labels);
        else {
            Firebase.getInstance().getLabels((labels) -> {
                Cache.labels = labels;
                callback.accept(labels);
            });
        }
    }
    public static void updateLabelsCache(HashMap<String, Label> labels) {
        Cache.labels = labels;
    }
}
