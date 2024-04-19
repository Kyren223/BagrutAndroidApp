package me.kyren223.echomobile.core;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import me.kyren223.echomobile.R;
import me.kyren223.echomobile.databinding.ActivityMainBinding;
import me.kyren223.echomobile.play.ChessGameManager;
import me.kyren223.echomobile.settings.SettingsActivity;
import me.kyren223.echomobile.utils.Cache;
import me.kyren223.echomobile.utils.Firebase;
import me.kyren223.echomobile.utils.Label;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private ImageButton settingsButton;
    private ImageView pfp;
    private TextView labelText;
    private TextView usernameText;

    private Button playButton;
    private Button gameHistoryButton;
    private Button statisticsButton;
    private Button achievementsButton;
    private Button selectedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        //binding.appBarMain.fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show());
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_play,
                R.id.nav_game_history,
                R.id.nav_statistics,
                R.id.nav_achievements)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener((v) -> handleButton(playButton, navController));

        gameHistoryButton = findViewById(R.id.gameHistoryButton);
        gameHistoryButton.setOnClickListener((v) -> handleButton(gameHistoryButton, navController));

        statisticsButton = findViewById(R.id.statisticsButton);
        statisticsButton.setOnClickListener((v) -> handleButton(statisticsButton, navController));

        achievementsButton = findViewById(R.id.achievementsButton);
        achievementsButton.setOnClickListener((v) -> handleButton(achievementsButton, navController));

        // Default selected button
        selectedButton = playButton;
        selectedButton.setBackgroundColor(Color.parseColor("#FF00695C"));

        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener((v) ->
                startActivity(new Intent(this, SettingsActivity.class))
        );

        pfp = findViewById(R.id.profilePicture);
        labelText = findViewById(R.id.label);
        usernameText = findViewById(R.id.username);

        updateUI();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        updateUI();
    }

    private void updateUI() {
        Cache.getPfp((bitmap) -> pfp.setImageBitmap(bitmap));
        Cache.getUserProfile((profile) -> {
            usernameText.setText(profile.displayName);
            Cache.getLabels((labels) -> {
                Label label = labels.get(profile.label);
                labelText.setText(label.getName());
                labelText.setBackgroundColor(Color.parseColor(label.getColor()));
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    private void handleButton(Button button, NavController navController) {
        if (selectedButton == button) return;
        if (selectedButton != null) {
            selectedButton.setBackgroundColor(Color.parseColor("#00000000"));
        }
        button.setBackgroundColor(Color.parseColor("#FF00695C"));
        selectedButton = button;

        if (button.getId() == R.id.playButton) {
            navController.navigate(R.id.nav_play);
        } else if (button.getId() == R.id.gameHistoryButton) {
            navController.navigate(R.id.nav_game_history);
        } else if (button.getId() == R.id.statisticsButton) {
            navController.navigate(R.id.nav_statistics);
        } else if (button.getId() == R.id.achievementsButton) {
            navController.navigate(R.id.nav_achievements);
        }
    }
}