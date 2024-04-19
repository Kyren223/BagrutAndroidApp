package me.kyren223.echomobile.game_history;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.kyren223.echomobile.R;
import me.kyren223.echomobile.utils.GameEntry;

public class GameAdapter extends RecyclerView.Adapter<GameHolder> {
    private final Context context;
    private final List<GameEntry> games;


    @NonNull
    @Override
    public GameHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false);
        return new GameHolder(v);
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull GameHolder holder, int position) {
        GameEntry game = games.get(position);
        holder.pfp.setImageBitmap(game.getPfp());
        holder.label.setText(game.getLabel());
        holder.label.setBackgroundColor(Color.parseColor(game.getLabelColor()));
        holder.name.setText(game.getName() + " " + game.getElo());
        holder.result.setImageResource(game.getResult());
    }


    @Override
    public int getItemCount() {
        return games != null ? games.size() : 0;
    }
    public GameAdapter(Context ctx,  List<GameEntry> games) {
        this.context = ctx;
        this.games = games;
    }
}
