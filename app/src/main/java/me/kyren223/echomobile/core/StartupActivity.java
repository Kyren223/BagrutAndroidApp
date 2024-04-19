package me.kyren223.echomobile.core;

import android.content.Intent;
import android.os.Bundle;

import me.kyren223.echomobile.authentication.SignInActivity;

public class StartupActivity extends MasterActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Class<?> activity = getStartActivity();
        startActivity(new Intent(this, activity));
        finish();
    }

    private Class<?> getStartActivity() {
        if (db.isSignedIn()) return MainActivity.class;
        return SignInActivity.class;
    }
}