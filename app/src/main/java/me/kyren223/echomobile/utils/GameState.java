package me.kyren223.echomobile.utils;

import com.google.firebase.firestore.DocumentSnapshot;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import me.kyren223.echomobile.play.ChessGameManager;

public class GameState {

    public @Nullable String white;
    public @Nullable String black;
    public String current;
    public int moveFrom;
    public int moveTo;
    public boolean drawWhite;
    public boolean drawBlack;

    public static GameState deserialize(DocumentSnapshot document) {
        GameState state = new GameState();
        state.white = document.getString("white");
        state.black = document.getString("black");
        state.current = document.getString("current");
        state.moveFrom = document.getLong("moveFrom").intValue();
        state.moveTo = document.getLong("moveTo").intValue();
        state.drawWhite = document.getBoolean("drawWhite");
        state.drawBlack = document.getBoolean("drawBlack");
        return state;
    }

    public HashMap<String, Object> serialize() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("white", white);
        map.put("black", black);
        map.put("current", current);
        map.put("moveFrom", moveFrom);
        map.put("moveTo", moveTo);
        map.put("drawWhite", drawWhite);
        map.put("drawBlack", drawBlack);
        return map;
    }
}
