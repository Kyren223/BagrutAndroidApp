package me.kyren223.echomobile.game_history;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import me.kyren223.echomobile.R;

public class GameHolder extends RecyclerView.ViewHolder {

    public final View view;
    public final ImageView pfp;
    public final TextView label;
    public final TextView name;
    public final ImageView result;

    public GameHolder(@NonNull View view) {
        super(view);
        this.view = view;
        pfp = view.findViewById(R.id.opponentPfp);
        label = view.findViewById(R.id.opponentLabel);
        name = view.findViewById(R.id.opponentUsername);
        result = view.findViewById(R.id.result);
    }
}
