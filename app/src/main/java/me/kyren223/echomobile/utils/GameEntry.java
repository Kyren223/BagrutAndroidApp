package me.kyren223.echomobile.utils;

import android.graphics.Bitmap;

import androidx.annotation.DrawableRes;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.function.Consumer;

import me.kyren223.echomobile.R;
import me.kyren223.echomobile.play.ChessGameManager;

public class GameEntry {

    private Bitmap pfp;
    private String label;
    private String labelColor;
    private String name;
    private String elo;
    private @DrawableRes int result;
    private long time; // In seconds, since epoch (1970)

    public static void getGameAsync(DocumentSnapshot document, Consumer<GameEntry> callback) {
        if (document == null) {
            callback.accept(null);
            return;
        }

        assert FirebaseAuth.getInstance().getCurrentUser() != null;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String white = document.getString("white");
        String black = document.getString("black");
        String result = document.getString("result");
        Long timeMaybe = document.getLong("time");
        long time = timeMaybe != null ? timeMaybe : 0;
        assert result != null;
        result = result.toLowerCase();

        GameEntry game = new GameEntry();
        game.time = time;
        if (result.contains("draw")) {
            game.result = R.drawable.chess_draw;
        } else if ((result.contains("white_wins") && userId.equals(white)) ||
                (result.contains("black_wins") && userId.equals(black))) {
            game.result = R.drawable.chess_win;
        } else {
            game.result = R.drawable.chess_lose;
        }

        String opponent = userId.equals(white) ? black : white;
        Firebase.getInstance().getUserProfile(opponent, profile -> {
            game.name = profile.displayName;
            game.elo = "(" + profile.elo + ")";
            Cache.getLabels(labels -> {
                Label label = labels.get(profile.label);
                if (label == null) {
                    label = new Label("Unknown", "#000000");
                }
                game.label = label.getName();
                game.labelColor = label.getColor();
            });
            Firebase.getInstance().getProfilePictureBitmap(profile, bitmap -> {
                game.pfp = bitmap;
                callback.accept(game);
            });
        });
    }

    private GameEntry() {
        this.pfp = null;
        this.label = null;
        this.labelColor = null;
        this.name = null;
        this.elo = null;
        this.result = 0;
    }

    public GameEntry(Bitmap pfp, String label, String labelColor, String name, String elo, int result, int time) {
        this.pfp = pfp;
        this.label = label;
        this.labelColor = labelColor;
        this.name = name;
        this.elo = elo;
        this.result = result;
        this.time = time;
    }

    public Bitmap getPfp() {
        return pfp;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelColor() {
        return labelColor;
    }

    public String getName() {
        return name;
    }

    public String getElo() {
        return elo;
    }

    public @DrawableRes int getResult() {
        return result;
    }

    public long getTime() {
        return time;
    }
}
