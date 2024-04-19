package me.kyren223.echomobile.game_history;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.kyren223.echomobile.R;
import me.kyren223.echomobile.databinding.FragmentGameHistoryBinding;
import me.kyren223.echomobile.utils.Firebase;
import me.kyren223.echomobile.utils.GameEntry;

public class GameHistoryFragment extends Fragment {
    private RecyclerView games;
    private RecyclerView.Adapter<?> adapter;
    private List<GameEntry> gameHistory;

    private FragmentGameHistoryBinding binding;

    public GameHistoryFragment() {
    }

    @SuppressLint("NotifyDataSetChanged")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGameHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        root.setForceDarkAllowed(false);

        games = root.findViewById(R.id.gameHistory);
        games.setForceDarkAllowed(false);
        RecyclerView.LayoutManager layout = new LinearLayoutManager(root.getContext());
        games.setLayoutManager(layout);

        gameHistory = new ArrayList<>();
        adapter = new GameAdapter(root.getContext(), gameHistory);
        games.setAdapter(adapter);

        System.out.println("Getting games");
        Firebase.getInstance().getGames(games -> {
            if (games == null) return;
            gameHistory.clear();
            gameHistory.addAll(games);
            adapter.notifyDataSetChanged();
            System.out.println("Got games");
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}