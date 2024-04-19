package me.kyren223.echomobile.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import me.kyren223.echomobile.R;
import me.kyren223.echomobile.authentication.AuthUtils;
import me.kyren223.echomobile.authentication.SignInActivity;
import me.kyren223.echomobile.utils.Cache;
import me.kyren223.echomobile.utils.Firebase;
import me.kyren223.echomobile.utils.Label;

public class SettingsActivity extends AppCompatActivity {
    private Button logOutButton;
    private ProgressBar progressBar;
    private ImageView pfp;
    private TextView labelText;
    private TextView usernameAndEloText;
    private String username;
    private String elo;
    private ActivityResultLauncher activityResultLauncher;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        progressBar = findViewById(R.id.progressBar);
        logOutButton = findViewById(R.id.logOutButton);
        logOutButton.setOnClickListener((v) -> {
            startWait();
            Firebase.getInstance().signOut();
            finishWait();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });

        pfp = findViewById(R.id.imageView);
        pfp.setOnClickListener((v) -> activityResultLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));
        registerCamera();

        labelText = findViewById(R.id.label);
        labelText.setOnClickListener((v) -> selectLabel());
        usernameAndEloText = findViewById(R.id.username);
        usernameAndEloText.setOnClickListener((v) -> renameUsername());

        Cache.getPfp((bitmap) -> pfp.setImageBitmap(bitmap));
        Cache.getUserProfile((profile) -> {
            username = profile.displayName;
            elo = String.valueOf(profile.elo);
            usernameAndEloText.setText(username + " (" + elo + ")");
            Cache.getLabels((labels) -> {
                Label label = labels.get(profile.label);
                labelText.setText(label.getName());
                labelText.setBackgroundColor(Color.parseColor(label.getColor()));
            });
        });
    }

    private void startWait() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        logOutButton.setEnabled(false);
        logOutButton.setText("");
        logOutButton.setBackgroundTintList(ColorStateList.valueOf(
                AuthUtils.getColorFromAttr(this, R.attr.primaryButtonPressedColor)));
    }

    private void finishWait() {
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        logOutButton.setEnabled(true);
        logOutButton.setText(R.string.log_out);
        logOutButton.setBackgroundTintList(ColorStateList.valueOf(
                AuthUtils.getColorFromAttr(this, R.attr.primaryButtonColor)));
    }

    private void registerCamera() {
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                Cache.updatePfpCache(bitmap);
                Firebase.getInstance().updateProfilePicture(bitmap);
                pfp.setImageBitmap(bitmap);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void renameUsername() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Display Name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(username);
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String displayName = input.getText().toString();
            Firebase.getInstance().updateDisplayName(displayName);
            Cache.getUserProfile((profile) -> {
                if (profile == null) return;
                username = displayName;
                elo = String.valueOf(profile.elo);
                usernameAndEloText.setText(username + " (" + elo + ")");
                profile.displayName = displayName;
                Cache.updateUserProfileCache(profile);
            });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();

    }

    private void selectLabel() {
        Cache.getUserProfile((profile) -> Cache.getLabels((labels) -> {
            String[] unlockedLabels = new String[profile.labels.size()];
            for (int i = 0; i < profile.labels.size(); i++) {
                unlockedLabels[i] = labels.get(profile.labels.get(i)).getName();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Label");

            builder.setItems(unlockedLabels, (dialog, which) -> {
                String name = unlockedLabels[which];
                for (String id : labels.keySet()) {
                    if (!labels.get(id).getName().equals(name)) continue;

                    Firebase.getInstance().updateLabel(id);
                    profile.label = id;

                    Label label = labels.get(id);
                    assert label != null;
                    this.labelText.setText(label.getName());
                    this.labelText.setBackgroundColor(Color.parseColor(label.getColor()));
                }
            });

            builder.show();
        }));
    }
}