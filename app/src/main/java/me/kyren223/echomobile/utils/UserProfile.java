package me.kyren223.echomobile.utils;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public class UserProfile {
    public String userId;
    public String displayName;
    public String pfpUri;
    public String label;
    public List<String> labels;
    public long elo;

    public UserProfile(String userId, String displayName, String pfpUri, String label, long elo, List<String> labels) {
        this.userId = userId;
        this.displayName = displayName;
        this.pfpUri = pfpUri;
        this.label = label;
        this.labels = labels;
        this.elo = elo;
    }
}
