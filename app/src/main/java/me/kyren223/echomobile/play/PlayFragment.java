package me.kyren223.echomobile.play;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import me.kyren223.echomobile.R;
import me.kyren223.echomobile.databinding.FragmentPlayBinding;
import me.kyren223.echomobile.utils.Cache;
import me.kyren223.echomobile.utils.Firebase;
import me.kyren223.echomobile.utils.Label;

public class PlayFragment extends Fragment {

    private FragmentPlayBinding binding;
    private ChessGameManager chess;
    private boolean searching = false;
    private GridLayout board;
    private int selectedSquare = -1;

    private TextView playerUsernameText;
    private TextView playerLabelText;
    private ImageView playerProfilePicture;
    private TextView playerTimerText;

    private TextView opponentUsernameText;
    private TextView opponentLabelText;
    private ImageView opponentProfilePicture;
    private TextView opponentTimerText;

    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        chess = ChessGameManager.getInstance();

        binding = FragmentPlayBinding.inflate(inflater, container, false);
        ConstraintLayout root = binding.getRoot();

        if (chess.isGameActive()) {
            createGameView();
        } else {
            constructButton(R.string.play, R.style.Button_Primary);
        }

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chess.isGameActive()) return;
        Firebase.getInstance().cancelSearch();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void onPlayClicked() {
        if (!searching) {
            searching = true;
            constructButton(R.string.searching, R.style.Button_PrimaryDisabled);
            chess.searchForGame(this::handleSearchResult);
        } else {
            searching = false;
            Firebase.getInstance().cancelSearch();
            constructButton(R.string.play, R.style.Button_Primary);
        }
    }

    private void updateEndGame(ChessGameManager.Result result, boolean isWhite) {
        String message = switch (result) {
            case WHITE_WINS_CHECKMATE -> isWhite ? "You win by checkmate" : "You lose by checkmate";
            case WHITE_WINS_BLACK_TIMEOUTS ->
                    isWhite ? "You win, opponent timed out" : "You lose, you timed out";
            case WHITE_WINS_BLACK_RESIGNS ->
                    isWhite ? "You win, opponent resigned" : "You lose, you resigned";
            case BLACK_WINS_CHECKMATE -> isWhite ? "You lose by checkmate" : "You win by checkmate";
            case BLACK_WINS_WHITE_TIMEOUTS ->
                    isWhite ? "You lose, you timed out" : "You win, opponent timed out";
            case BLACK_WINS_WHITE_RESIGNS ->
                    isWhite ? "You lose, you resigned" : "You win, opponent resigned";
            case DRAW_INSUFFICIENT_MATERIAL_VS_WHITE_TIMEOUTS ->
                    isWhite ? "Draw, insufficient material vs time out" : "Draw, opponent timed out";
            case DRAW_INSUFFICIENT_MATERIAL_VS_BLACK_TIMEOUTS ->
                    isWhite ? "Draw, opponent timed out" : "Draw, insufficient material vs time out";
            case DRAW_FIFTY_MOVE_RULE -> "Draw, fifty move rule";
            case DRAW_INSUFFICIENT_MATERIAL -> "Draw, insufficient material";
            case DRAW_AGREED -> "Draw, offer accepted";
        };

        getActivity().runOnUiThread(() -> {
            selectedSquare = -1;
            if (getContext() == null) return;
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        });

        // Reset the game after a delay
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            System.out.println("Resetting game");
            constructButton(R.string.play, R.style.Button_Primary);
            System.out.println("Button constructed");
        }, 5, TimeUnit.SECONDS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int percentToPxWidth(double percent) {
        return (int) (percent * getResources().getDisplayMetrics().widthPixels / 100.0);
    }

    private int percentToPxHeight(double percent) {
        return (int) (percent * getResources().getDisplayMetrics().heightPixels / 100.0);
    }

    private void handleSearchResult(boolean success) {
        if (success) {
            createGameView();
        } else {
            constructButton(R.string.play, R.style.Button_Primary);
        }
    }

    private void createGameView() {
        chess.setUpdateBoardCallback(this::updateBoard);
        chess.setUpdateClocksCallback(this::updateTimers);
        chess.setUpdateLastMoveCallback(this::updateLastMove);
        chess.setEndGameCallback(this::updateEndGame);

        LinearLayout root = new LinearLayout(getContext());
        root.setForceDarkAllowed(false);
        root.setOrientation(LinearLayout.VERTICAL);

        // Opponent's view
        LinearLayout opponent = createPlayer(chess.getOpponentId(), true);
        root.addView(opponent);

        // Board view
        board = new GridLayout(getContext());
        board.setMinimumWidth(percentToPxWidth(100));
        board.setMinimumHeight(percentToPxWidth(100));
        board.setBackgroundTintList(ContextCompat.getColorStateList(board.getContext(), R.color.chess_dark_square));
        board.setRowCount(8);
        board.setColumnCount(8);

        // Add chess board squares
        for (int i = 0; i < 64; i++) {
            ImageButton square = new ImageButton(getContext());
            square.setMinimumWidth(percentToPxWidth(12.5));
            square.setMinimumHeight(percentToPxWidth(12.5));
            square.setMaxWidth(percentToPxWidth(12.5));
            square.setMaxHeight(percentToPxWidth(12.5));
            square.setLayoutParams(new LinearLayout.LayoutParams(percentToPxWidth(12.5), percentToPxWidth(12.5)));
            square.setCropToPadding(true);
            square.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            square.setScaleType(ImageView.ScaleType.CENTER);

            int id = chess.isSquareLight(i) ? R.color.chess_light_square : R.color.chess_dark_square;
            if (chess.getLastMoveSquareIndex() == i) {
                id = R.color.chess_last_move_square;
            }
            square.setBackgroundColor(ContextCompat.getColor(square.getContext(), id));
            final int index = i; // Final variable for lambda
            square.setOnClickListener(v -> handleClickedSquare((ImageButton) v, index));
            board.addView(square, i);
        }

        root.addView(board);

        // Player's view
        LinearLayout player = createPlayer(FirebaseAuth.getInstance().getCurrentUser().getUid(), false);
        root.addView(player);

        // Resign and draw buttons
        LinearLayout buttons = new LinearLayout(getContext());
        buttons.setMinimumWidth(percentToPxWidth(100));
        buttons.setMinimumHeight(percentToPxHeight(10));
        buttons.setBackgroundTintList(ContextCompat.getColorStateList(buttons.getContext(), R.color.dark_background));

        Button drawButton = new Button(getContext(), null, 0, R.style.Button_Secondary);
        drawButton.setText(R.string.draw);
        drawButton.setWidth(percentToPxWidth(50));
        drawButton.setHeight(percentToPxHeight(10));
        drawButton.setOnClickListener(this::onDrawClicked);
        buttons.addView(drawButton);

        Button resignButton = new Button(getContext(), null, 0, R.style.Button_Secondary);
        resignButton.setText(R.string.resign);
        resignButton.setWidth(percentToPxWidth(50));
        resignButton.setHeight(percentToPxHeight(10));
        resignButton.setOnClickListener(this::onResignClicked);
        buttons.addView(resignButton);

        root.addView(buttons);

        // Set the view
        if (binding == null) {
            if (getActivity() == null) Firebase.getInstance().cancelSearch();
            else {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_play);
                NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
                navigationView.setCheckedItem(R.id.nav_play);
            }
            return;
        }

        binding.getRoot().removeAllViews();
        binding.getRoot().addView(root);

        chess.forceUpdate();
        updateChessHUD();
    }

    private void onDrawClicked(View view) {
        // TODO
        // Should switch to "Cancel draw" if a draw offer is already made
        // If both players are on "cancel draw" then they both agree to a draw
        // And one of them should call endGame with a DRAW_AGREED result
    }

    private void onResignClicked(View view) {
        if (!chess.isGameActive()) return;
        chess.endGame(chess.isPlayerWhite() ?
                ChessGameManager.Result.BLACK_WINS_WHITE_RESIGNS :
                ChessGameManager.Result.WHITE_WINS_BLACK_RESIGNS
        );
    }

    private void updateLastMove(int oldIndex, int newIndex) {
        boolean isWhite = chess.isPlayerWhite();
        int actualOldIndex = isWhite ? oldIndex : 63 - oldIndex;
        int actualNewIndex = isWhite ? newIndex : 63 - newIndex;
        if (oldIndex != -1) {
            System.out.println("Setting old move square: " + oldIndex + "|" + actualOldIndex);
            ImageButton oldSquare = (ImageButton) board.getChildAt(actualOldIndex);
            int color = chess.isSquareLight(oldIndex) ? R.color.chess_light_square : R.color.chess_dark_square;
            oldSquare.setBackgroundColor(ContextCompat.getColor(oldSquare.getContext(), color));
        }
        if (newIndex != -1) {
            System.out.println("Setting last move square: " + newIndex + "|" + actualNewIndex);
            ImageButton newSquare = (ImageButton) board.getChildAt(actualNewIndex);
            newSquare.setBackgroundColor(ContextCompat.getColor(newSquare.getContext(), R.color.chess_last_move_square));
        }
    }

    private void constructButton(int textId, int styleId) {
        getActivity().runOnUiThread(() -> {
            if (binding == null) return;
            ConstraintLayout root = binding.getRoot();
            root.removeAllViews();

            Button button = new Button(root.getContext(), null, 0, styleId);
            button.setText(textId);
            button.setWidth(dpToPx(200));
            button.setHeight(dpToPx(60));
            button.setOnClickListener(v -> onPlayClicked());
            button.setId(View.generateViewId());
            root.addView(button);

            ConstraintSet set = new ConstraintSet();
            set.clone(root);
            set.connect(button.getId(), ConstraintSet.TOP, root.getId(), ConstraintSet.TOP, 0);
            set.connect(button.getId(), ConstraintSet.BOTTOM, root.getId(), ConstraintSet.BOTTOM, 0);
            set.connect(button.getId(), ConstraintSet.START, root.getId(), ConstraintSet.START, 0);
            set.connect(button.getId(), ConstraintSet.END, root.getId(), ConstraintSet.END, 0);
            set.applyTo(root);
        });
    }

    private void updateChessHUD() {
        updateChessHUDforUser(false);
        updateChessHUDforUser(true);
    }

    private void updateChessHUDforUser(boolean isOpponent) {
        assert FirebaseAuth.getInstance().getCurrentUser() != null;
        String id = isOpponent ? chess.getOpponentId() : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (id == null) {
            System.out.println("ID is null");
            return;
        }
        Firebase.getInstance().getUserProfile(id, profile -> {
            if (profile == null) return;

            Firebase.getInstance().getProfilePictureBitmap(profile, bitmap -> {
                if (isOpponent) {
                    opponentProfilePicture.setImageBitmap(bitmap);
                } else {
                    playerProfilePicture.setImageBitmap(bitmap);
                }
            });
            if (isOpponent) {
                opponentUsernameText.setText(profile.displayName);
                Cache.getLabels(labels -> {
                    Label label = labels.get(profile.label);
                    if (label == null) return;
                    opponentLabelText.setText(label.getName());
                    opponentLabelText.setBackgroundColor(Color.parseColor(label.getColor()));
                });
            } else {
                playerUsernameText.setText(profile.displayName);
                Cache.getLabels(labels -> {
                    Label label = labels.get(profile.label);
                    if (label == null) return;
                    playerLabelText.setText(label.getName());
                    playerLabelText.setBackgroundColor(Color.parseColor(label.getColor()));
                });
            }
        });
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private LinearLayout createPlayer(String id, boolean isOpponent) {
        String username = "Loading...";
        int elo = 0;
        int timeInSeconds = 0;
        Label label = null;
        updateChessHUDforUser(isOpponent);

        int playerHeightInPixels = percentToPxHeight(17);
        LinearLayout player = new LinearLayout(getContext());
        player.setForceDarkAllowed(false);
        player.setMinimumWidth(percentToPxWidth(100));
        player.setMinimumHeight(playerHeightInPixels);
        player.setBackgroundTintList(ContextCompat.getColorStateList(player.getContext(), R.color.dark_primary));

        // Player profile
        LinearLayout playerProfile = new LinearLayout(getContext());
        playerProfile.setOrientation(LinearLayout.HORIZONTAL);
        playerProfile.setMinimumWidth(percentToPxWidth(50));
        playerProfile.setMinimumHeight(playerHeightInPixels);
        playerProfile.setBackgroundTintList(ContextCompat.getColorStateList(player.getContext(), R.color.dark_primary));
        playerProfile.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), 0);

        CardView profilePicture = new CardView(playerProfile.getContext());
        profilePicture.setMinimumWidth(dpToPx(72));
        profilePicture.setMinimumHeight(dpToPx(72));
        profilePicture.setRadius(dpToPx(250));
        profilePicture.setBackgroundTintList(ContextCompat.getColorStateList(player.getContext(), R.color.dark_accent_secondary));
        CardView.LayoutParams layoutParams = new CardView.LayoutParams(
                CardView.LayoutParams.WRAP_CONTENT,
                CardView.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.topMargin = dpToPx(15);
        profilePicture.setLayoutParams(layoutParams);


        CardView nestedProfilePicture = new CardView(profilePicture.getContext());
        nestedProfilePicture.setRadius(dpToPx(250));
        nestedProfilePicture.setMinimumWidth(dpToPx(67));
        nestedProfilePicture.setMinimumHeight(dpToPx(67));
        layoutParams = new CardView.LayoutParams(
                CardView.LayoutParams.WRAP_CONTENT,
                CardView.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        nestedProfilePicture.setLayoutParams(layoutParams);

        ImageView profileImage = new ImageView(nestedProfilePicture.getContext());
        profileImage.setCropToPadding(true);
        profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        profileImage.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(66), dpToPx(66)));

        nestedProfilePicture.addView(profileImage);
        profilePicture.addView(nestedProfilePicture);

        LinearLayout profileText = new LinearLayout(playerProfile.getContext());
        profileText.setOrientation(LinearLayout.VERTICAL);
        profileText.setPaddingRelative(dpToPx(10), 0, 0, 0);
        profileText.setMinimumWidth(0);
        profileText.setMinimumHeight(1);
        profileText.setBackgroundTintList(ContextCompat.getColorStateList(player.getContext(), android.R.color.transparent));
        profileText.setPadding(percentToPxWidth(2), percentToPxWidth(7), 0, percentToPxWidth(2));

        TextView labelView = new TextView(profileText.getContext(), null, 0, R.style.AuthenticationLabel);
        if (label != null) labelView.setText(label.getName());
        labelView.setWidth(percentToPxWidth(40));
        labelView.setMaxWidth(percentToPxWidth(40));
        labelView.setHeight(dpToPx(28));
        labelView.setPadding(dpToPx(5), 0, dpToPx(5), 0);
        labelView.setGravity(Gravity.CENTER_VERTICAL);
        labelView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        labelView.setTextColor(ContextCompat.getColor(labelView.getContext(), R.color.dark_text_primary));
        labelView.setTextSize(16);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setPaddingRelative(dpToPx(4), 0, 0, dpToPx(4));
        labelView.setMaxLines(1);

        TextView usernameView = new TextView(profileText.getContext(), null, 0, R.style.AuthenticationLabel);
        usernameView.setText(username);
        labelView.setWidth(percentToPxWidth(40));
        labelView.setMaxWidth(percentToPxWidth(40));
        usernameView.setHeight(dpToPx(52));
        usernameView.setPadding(dpToPx(5), 0, dpToPx(5), 0);
        usernameView.setGravity(Gravity.TOP);
        usernameView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        usernameView.setTextColor(ContextCompat.getColor(usernameView.getContext(), R.color.dark_text_primary));
        usernameView.setTextSize(24);
        usernameView.setTypeface(Typeface.DEFAULT_BOLD);
        usernameView.setPaddingRelative(dpToPx(4), 0, 0, dpToPx(4));
        usernameView.setBackgroundTintList(ContextCompat.getColorStateList(usernameView.getContext(), android.R.color.transparent));
        usernameView.setMaxLines(1);

        profileText.addView(labelView);
        profileText.addView(usernameView);

        playerProfile.addView(profilePicture);
        playerProfile.addView(profileText);

        // Player timer and elo
        LinearLayout playerTimerElo = new LinearLayout(getContext());
        playerTimerElo.setPadding(percentToPxWidth(5), percentToPxWidth(7), percentToPxWidth(5), percentToPxWidth(7));
        playerTimerElo.setOrientation(LinearLayout.VERTICAL);
        playerTimerElo.setMinimumWidth(percentToPxWidth(50));
        playerTimerElo.setMinimumHeight(percentToPxHeight(17));
        playerTimerElo.setBackgroundTintList(ContextCompat.getColorStateList(player.getContext(), R.color.dark_primary));

        // Add player timer and elo
        TextView playerTimer = new TextView(getContext(), null, 0, R.style.AuthenticationLabel);
        int minutes = timeInSeconds / 60;
        int seconds = timeInSeconds % 60;
        playerTimer.setText(String.format("%02d:%02d", minutes, seconds));
        playerTimer.setTextSize(24);
        playerTimer.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        playerTimer.setMaxLines(1);
        playerTimerElo.addView(playerTimer);

        TextView playerElo = new TextView(getContext(), null, 0, R.style.AuthenticationLabel);
        playerElo.setText("(" + elo + ")");
        playerElo.setTextSize(24);
        playerElo.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        playerElo.setBackgroundColor(ContextCompat.getColor(playerElo.getContext(), android.R.color.transparent));
        playerElo.setMaxLines(1);
        playerTimerElo.addView(playerElo);

        player.addView(playerProfile);
        player.addView(playerTimerElo);

        if (isOpponent) {
            opponentUsernameText = usernameView;
            opponentLabelText = labelView;
            opponentProfilePicture = profileImage;
            opponentTimerText = playerTimer;
        } else {
            playerUsernameText = usernameView;
            playerLabelText = labelView;
            playerProfilePicture = profileImage;
            playerTimerText = playerTimer;
        }

        if (id == null) {
            System.out.println("ID is null!!!!!!");
            return player;
        }
        Firebase.getInstance().getUserProfile(id, profile -> {
            if (profile == null) return;
            playerElo.setText("(" + profile.elo + ")");
        });


        return player;
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void updateTimers(int playerTime, int opponentTime) {
        int playerMinutes = playerTime / 60;
        int playerSeconds = playerTime % 60;
        if (playerTimerText != null) {
            playerTimerText.setText(String.format("%02d:%02d", playerMinutes, playerSeconds));
        }

        int opponentMinutes = opponentTime / 60;
        int opponentSeconds = opponentTime % 60;
        if (opponentTimerText != null) {
            opponentTimerText.setText(String.format("%02d:%02d", opponentMinutes, opponentSeconds));
        }
    }

    private void updateBoard(int[] board) {
        for (int i = 0; i < 64; i++) {
            ImageButton square = (ImageButton) this.board.getChildAt(chess.isPlayerWhite() ? i : 63 - i);
            square.setImageResource(getPieceResource(board[i], chess.isSquareLight(i)));
        }
    }

    private int getPieceResource(int piece, boolean isLight) {
        return switch (piece) {
            case ChessGameManager.EMPTY -> android.R.color.transparent;
            case ChessGameManager.WHITE_PAWN ->
                    isLight ? R.drawable.chess_pll : R.drawable.chess_pld;
            case ChessGameManager.WHITE_KNIGHT ->
                    isLight ? R.drawable.chess_nll : R.drawable.chess_nld;
            case ChessGameManager.WHITE_BISHOP ->
                    isLight ? R.drawable.chess_bll : R.drawable.chess_bld;
            case ChessGameManager.WHITE_ROOK ->
                    isLight ? R.drawable.chess_rll : R.drawable.chess_rld;
            case ChessGameManager.WHITE_QUEEN ->
                    isLight ? R.drawable.chess_qll : R.drawable.chess_qld;
            case ChessGameManager.WHITE_KING ->
                    isLight ? R.drawable.chess_kll : R.drawable.chess_kld;

            case ChessGameManager.BLACK_PAWN ->
                    isLight ? R.drawable.chess_pdl : R.drawable.chess_pdd;
            case ChessGameManager.BLACK_KNIGHT ->
                    isLight ? R.drawable.chess_ndl : R.drawable.chess_ndd;
            case ChessGameManager.BLACK_BISHOP ->
                    isLight ? R.drawable.chess_bdl : R.drawable.chess_bdd;
            case ChessGameManager.BLACK_ROOK ->
                    isLight ? R.drawable.chess_rdl : R.drawable.chess_rdd;
            case ChessGameManager.BLACK_QUEEN ->
                    isLight ? R.drawable.chess_qdl : R.drawable.chess_qdd;
            case ChessGameManager.BLACK_KING ->
                    isLight ? R.drawable.chess_kdl : R.drawable.chess_kdd;

            default -> throw new IllegalStateException("Unexpected value: " + piece);
        };
    }

    private void handleClickedSquare(ImageButton square, int index) {
        if (!chess.isGameActive()) return;
        int actualIndex = chess.isPlayerWhite() ? index : 63 - index;

        int file = chess.getFile(actualIndex);
        int rank = chess.getRank(actualIndex);

        System.out.println("Clicked square at " + file + ", " + rank);
        if (selectedSquare == -1) {
            int piece = chess.getPiece(file, rank);
            if (piece == ChessGameManager.EMPTY) return;
            if (chess.getColor(piece) != (chess.isPlayerWhite() ? 0 : 1)) return;

            selectedSquare = index;
            square.setBackgroundColor(ContextCompat.getColor(square.getContext(), R.color.chess_selected_square));
        } else if (selectedSquare == index) {
            selectedSquare = -1;
            int color = chess.isSquareLight(actualIndex) ? R.color.chess_light_square : R.color.chess_dark_square;
            square.setBackgroundColor(ContextCompat.getColor(square.getContext(), color));
        } else {
            int actualSelectedSquare = chess.isPlayerWhite() ? selectedSquare : 63 - selectedSquare;
            int fromFile = chess.getFile(actualSelectedSquare);
            int fromRank = chess.getRank(actualSelectedSquare);

            ImageButton selectedSquareButton = (ImageButton) board.getChildAt(this.selectedSquare);
            int selectedSquareColor = chess.isSquareLight(actualSelectedSquare) ? R.color.chess_light_square : R.color.chess_dark_square;

            selectedSquareButton.setBackgroundColor(ContextCompat.getColor(square.getContext(), selectedSquareColor));
            selectedSquare = -1;

            if (!chess.movePiece(fromFile, fromRank, file, rank, false)) {
                Toast.makeText(getContext(), "Invalid move", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }
}