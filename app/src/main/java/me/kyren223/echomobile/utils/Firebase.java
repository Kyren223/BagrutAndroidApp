package me.kyren223.echomobile.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import me.kyren223.echomobile.play.ChessGameManager;

public class Firebase {

    private static Firebase instance;

    public static Firebase getInstance() {
        if (instance == null) instance = new Firebase();
        return instance;
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final FirebaseStorage storage;
    private final CollectionReference users;
    private final CollectionReference labels;
    private final CollectionReference activeGames;
    private final CollectionReference games;

    private Firebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        users = db.collection("users");
        labels = db.collection("labels");
        activeGames = db.collection("activeGames");
        games = db.collection("games");
        addLabelsUpdateListener();
    }

    public void getUserProfile(String userId, Consumer<UserProfile> callback) {
        Task<DocumentSnapshot> documentSnapshotTask = users.document(userId).get();
        documentSnapshotTask.addOnCompleteListener((task) -> {
            if (!task.isSuccessful()) {
                callback.accept(null);
                return;
            }
            DocumentSnapshot document = task.getResult();
            if (document == null || !document.exists()) {
                callback.accept(null);
                return;
            }

            HashMap<String, Object> data = (HashMap<String, Object>) document.getData();
            if (data == null) {
                callback.accept(null);
                return;
            }

            UserProfile profile = new UserProfile(
                    userId,
                    (String) data.get("displayName"),
                    (String) data.get("pfpUri"),
                    (String) data.get("label"),
                    (long) data.get("elo"),
                    (List<String>) data.get("labels")
            );
            if (auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(userId)) {
                // Cache the user's profile if it's the current user
                Cache.updateUserProfileCache(profile);
            }
            callback.accept(profile);
        });
    }

    public void addLabelsUpdateListener() {
        labels.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null) return;
            for (DocumentChange dc : snapshot.getDocumentChanges()) {
                switch (dc.getType()) {
                    case ADDED:
                    case MODIFIED: {
                        Label label = new Label(
                                (String) dc.getDocument().get("name"),
                                (String) dc.getDocument().get("color")
                        );
                        Cache.getLabels((labels) -> labels.put(dc.getDocument().getId(), label));
                    }
                    break;
                    case REMOVED: {
                        Cache.getLabels((labels) -> labels.remove(dc.getDocument().getId()));
                    }
                    break;
                }
            }
        });
    }

    public void getLabels(Consumer<HashMap<String, Label>> callback) {
        Task<QuerySnapshot> documentSnapshotTask = labels.get();
        documentSnapshotTask.addOnCompleteListener((task) -> {
            if (!task.isSuccessful()) return;
            List<DocumentSnapshot> documents = task.getResult().getDocuments();

            HashMap<String, Label> labels = new HashMap<>();
            for (DocumentSnapshot document : documents) {
                Label label = new Label(
                        (String) document.get("name"),
                        (String) document.get("color")
                );
                labels.put(document.getId(), label);
            }
            Cache.updateLabelsCache(labels);
            callback.accept(labels);
        });
    }

    public void signIn(String email, String password, Consumer<Boolean> callback) {
        Task<AuthResult> signInTask = auth.signInWithEmailAndPassword(email, password);
        signInTask.addOnCompleteListener((task) -> callback.accept(task.isSuccessful()));
        updateUser();
    }

    public void signUp(String email, String password, String displayName, Consumer<Boolean> callback) {
        Task<AuthResult> signUpTask = auth.createUserWithEmailAndPassword(email, password);
        signUpTask.addOnCompleteListener((task) -> {
            if (!task.isSuccessful()) {
                callback.accept(false);
                return;
            }
            FirebaseUser user = task.getResult().getUser();
            if (user == null) {
                callback.accept(false);
                return;
            }
            String userId = user.getUid();
            HashMap<String, Object> data = new HashMap<>();
            data.put("displayName", displayName);
            data.put("pfpUri", "profiles/default.png");
            data.put("label", "rookie");
            data.put("elo", 1000);
            data.put("labels", List.of("rookie"));
            users.document(userId).set(data);
            callback.accept(true);
            updateUser();
        });
    }

    public void signOut() {
        auth.signOut();
        Cache.updateUserProfileCache(null);
        Cache.updatePfpCache(null);
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    public void updateProfilePicture(Bitmap bitmap) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();

        if (bitmap == null) {
            StorageReference ref = storage.getReference("profiles/default.png");
            users.document(userId).update("pfpUri", ref.getPath());
            return;
        }

        StorageReference storageRef = storage.getReference("profiles/" + userId + ".png");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        storageRef.putBytes(byteArray);
        users.document(userId).update("pfpUri", storageRef.getPath());
    }

    public void getProfilePictureBitmap(UserProfile profile, Consumer<Bitmap> callback) {
        if (profile == null || profile.pfpUri == null) return;

        StorageReference storageRef = storage.getReference(profile.pfpUri);
        Task<byte[]> bytesTask = storageRef.getBytes(1024 * 1024);
        bytesTask.addOnCompleteListener((task) -> {
            if (!task.isSuccessful()) {
                callback.accept(null);
                return;
            }
            byte[] bytes = task.getResult();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            callback.accept(bitmap);
        });
    }

    public void updateDisplayName(String toString) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        users.document(userId).update("displayName", toString);
    }

    public void updateLabel(String label) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        users.document(userId).update("label", label);
    }

    public void updateLabels(String[] labels) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        users.document(userId).update("labels", labels);
    }

    private void updateUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        getUserProfile(userId, (profile) -> getProfilePictureBitmap(profile, (bitmap) -> {
        }));
    }

    public void createOrJoinGame(BiConsumer<String, Boolean> onStart, Consumer<GameState> onUpdate, Consumer<ChessGameManager.Result> onEnd) {
        assert auth.getCurrentUser() != null;
        System.out.println("Creating or joining game");

        activeGames.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                System.out.println("Failed to get active games");
                return;
            }

            List<DocumentSnapshot> documents = task.getResult().getDocuments();
            for (DocumentSnapshot document : documents) {
                GameState game = GameState.deserialize(document);
                if (game.white != null && game.black != null) continue;
                if ((game.white != null && game.white.equals(auth.getCurrentUser().getUid())) ||
                        (game.black != null && game.black.equals(auth.getCurrentUser().getUid()))) {
                    continue;
                }

                // Join the game
                String userId = auth.getCurrentUser().getUid();
                String opponentId = game.white != null ? game.white : game.black;
                System.out.println("Joining game " + document.getId() + " with opponent " + opponentId);
                boolean isPlayerWhite = game.white == null;
                if (game.white == null) {
                    game.white = userId;
                } else if (game.black == null) {
                    game.black = userId;
                }

                document.getReference().update(game.serialize()).addOnSuccessListener(t -> {
                    gameId = document.getId();
                    listenForGame(opponentId, isPlayerWhite, document, onStart, onUpdate, onEnd);
                });
                return;
            }

            // No games to join, create a new game
            DocumentReference document = createGame();
            gameId = document.getId();
            System.out.println("Created game " + document.getId());
            final boolean[] stop = {false};
            document.addSnapshotListener((snapshot, error) -> {
                if (stop[0]) return;
                if (error != null || snapshot == null || !snapshot.exists()) return;
                if (gameId == null) return;
                GameState game = GameState.deserialize(snapshot);
                if (game.white == null || game.black == null) return;
                String userId = auth.getCurrentUser().getUid();
                String opponentId = game.white.equals(userId) ? game.black : game.white;
                boolean isPlayerWhite = game.white.equals(userId);
                System.out.println("Opponent joined game " + snapshot.getId() + ", opponent: " + opponentId);
                stop[0] = true;
                listenForGame(opponentId, isPlayerWhite, snapshot, onStart, onUpdate, onEnd);
            });
        });
    }

    private String gameId;

    public void cancelSearch() {
        if (gameId == null) return;
        activeGames.document(gameId).delete();
        gameId = null;
    }

    private void listenForGame(String opponentId, boolean isPlayerWhite,  DocumentSnapshot snapshot, BiConsumer<String, Boolean> onStart, Consumer<GameState> onUpdate, Consumer<ChessGameManager.Result> onEnd) {
        games.addSnapshotListener((querySnapshot, error) -> {
            if (querySnapshot == null || querySnapshot.isEmpty()) return;
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                if (!document.getId().equals(gameId)) continue;
                String resultString = document.getString("result");
                ChessGameManager.Result result;
                try {
                    result = ChessGameManager.Result.valueOf(resultString);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return;
                }
                gameId = null;
                onEnd.accept(result);
                return;
            }
        });

        System.out.println("Listening for game " + snapshot.getId());
        onStart.accept(opponentId, isPlayerWhite);
        snapshot.getReference().addSnapshotListener((document, error) -> {
            if (error != null || document == null || !document.exists()) return;
            GameState game = GameState.deserialize(document);
            // game.current value is white when it should be black
            assert auth.getCurrentUser() != null;
            String userId = auth.getCurrentUser().getUid();
            String current = game.current.equals("white") ? game.white : game.black;
            if (!userId.equals(current)) return;
            System.out.println("Current: " + game.current + ", white: " + game.white + ", black: " + game.black);
            System.out.println("Current user: " + userId + ", current: " + current);
            // If the updated snapshot has white, it means it's waiting for white
            // So we need to update this user
            onUpdate.accept(game);
        });
    }

    private DocumentReference createGame() {
        assert auth.getCurrentUser() != null;
        boolean white = new Random().nextBoolean();
        GameState state = new GameState();
        state.white = white ? auth.getCurrentUser().getUid() : null;
        state.black = white ? null : auth.getCurrentUser().getUid();
        state.current = "white";
        state.moveFrom = -1;
        state.moveTo = -1;
        state.drawWhite = false;
        state.drawBlack = false;
        System.out.println("Creating game with white: " + state.white + ", black: " + state.black);
        DocumentReference document = activeGames.document();
        document.set(state.serialize());
        return document;
    }

    public void updateGame(int fromIndex, int toIndex) {
        if (gameId == null) return;
        System.out.println("Updating game " + gameId + " with move from " + fromIndex + " to " + toIndex);
        AtomicBoolean updated = new AtomicBoolean(false);
        activeGames.document(gameId).get().addOnSuccessListener(document -> {
            if (updated.get()) return;
            assert document != null && document.exists();
            assert auth.getCurrentUser() != null;

            GameState game = GameState.deserialize(document);
            game.moveFrom = fromIndex;
            game.moveTo = toIndex;
            game.current = game.current.equals("white") ? "black" : "white";
            System.out.println("Made client move, new current: " + game.current + ", move from: " + game.moveFrom + ", move to: " + game.moveTo);
            updated.set(true);
            document.getReference().update(game.serialize()).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    System.out.println("Failed to update game");
                    return;
                }
                System.out.println("Made SERVER MOVE Updated game " + gameId);
            });
        });
    }

    public void endGame(ChessGameManager.Result result) {
        if (gameId == null) return;
        activeGames.document(gameId).get().addOnSuccessListener(document -> {
            assert document != null && document.exists();
            GameState game = GameState.deserialize(document);
            HashMap<String, Object> data = new HashMap<>();
            data.put("time", System.currentTimeMillis());
            data.put("result", result.toString());
            data.put("white", game.white);
            data.put("black", game.black);
            data.put("score", switch (result) {
                case WHITE_WINS_CHECKMATE, WHITE_WINS_BLACK_TIMEOUTS,
                        WHITE_WINS_BLACK_RESIGNS -> "1-0";

                case BLACK_WINS_CHECKMATE, BLACK_WINS_WHITE_TIMEOUTS,
                        BLACK_WINS_WHITE_RESIGNS -> "0-1";

                case DRAW_INSUFFICIENT_MATERIAL_VS_WHITE_TIMEOUTS,
                        DRAW_INSUFFICIENT_MATERIAL_VS_BLACK_TIMEOUTS,
                        DRAW_FIFTY_MOVE_RULE,
                        DRAW_INSUFFICIENT_MATERIAL,
                        DRAW_AGREED -> "½-½";
            });
            games.document(gameId).set(data);

            activeGames.document(gameId).delete();
            gameId = null;
        });
    }

    public void getGames(Consumer<List<GameEntry>> callback) {
        games.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.accept(null);
                return;
            }

            List<DocumentSnapshot> documents = task.getResult().getDocuments();
            List<GameEntry> games = new ArrayList<>();
            AtomicInteger counter = new AtomicInteger(documents.size());

            for (DocumentSnapshot document : documents) {
                if (document == null || !document.exists()) {
                    counter.decrementAndGet();
                    continue;
                }

                Map<String, Object> data = document.getData();
                if (data == null) {
                    counter.decrementAndGet();
                    continue;
                }

                assert auth.getCurrentUser() != null;
                String userId = auth.getCurrentUser().getUid();
                // Not equal to either white or black, don't show other games that
                // the user is not a part of
                if (!userId.equals(data.get("white")) && !userId.equals(data.get("black"))) {
                    counter.decrementAndGet();
                    continue;
                }

                GameEntry.getGameAsync(document, game -> {
                    if (game != null) {
                        System.out.println("GAMEGAME GAMEGAME Label: " + game.getLabel() + ", name: " + game.getName() + ", result: " + game.getResult() + ", time: " + game.getTime());
                        games.add(game);
                    }

                    // If all games have been processed, call the callback
                    // This guarantees that the callback is called only once
                    // and that all games appear immediately
                    if (counter.decrementAndGet() == 0) {
                        // Sort games by time from newest to oldest
                        // Note: difference cannot be greater than Integer.MAX_VALUE
                        games.sort((a, b) -> (int) (b.getTime() - a.getTime()));
                        callback.accept(games);
                    }
                });
            }
        });
    }
}
