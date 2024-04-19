package me.kyren223.echomobile.play;

import com.google.firebase.auth.FirebaseAuth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import me.kyren223.echomobile.utils.Firebase;

public class ChessGameManager {

    public enum Result {
        WHITE_WINS_CHECKMATE,
        WHITE_WINS_BLACK_TIMEOUTS,
        WHITE_WINS_BLACK_RESIGNS,
        BLACK_WINS_CHECKMATE,
        BLACK_WINS_WHITE_TIMEOUTS,
        BLACK_WINS_WHITE_RESIGNS,
        DRAW_INSUFFICIENT_MATERIAL_VS_WHITE_TIMEOUTS,
        DRAW_INSUFFICIENT_MATERIAL_VS_BLACK_TIMEOUTS,
        DRAW_FIFTY_MOVE_RULE,
        DRAW_INSUFFICIENT_MATERIAL,
        DRAW_AGREED,
    }

    private static ChessGameManager instance;

    public static ChessGameManager getInstance() {
        if (instance == null) instance = new ChessGameManager();
        return instance;
    }

    // Pieces
    public static final int EMPTY = 0;
    public static final int WHITE_PAWN = 1;
    public static final int WHITE_KNIGHT = 2;
    public static final int WHITE_BISHOP = 3;
    public static final int WHITE_ROOK = 4;
    public static final int WHITE_QUEEN = 5;
    public static final int WHITE_KING = 6;
    public static final int BLACK_PAWN = 7;
    public static final int BLACK_KNIGHT = 8;
    public static final int BLACK_BISHOP = 9;
    public static final int BLACK_ROOK = 10;
    public static final int BLACK_QUEEN = 11;
    public static final int BLACK_KING = 12;

    public static final int FILE_COUNT = 8;
    public static final int RANK_COUNT = 8;
    public static final int[] STARTING_POSITION = {
            BLACK_ROOK, BLACK_KNIGHT, BLACK_BISHOP, BLACK_QUEEN, BLACK_KING, BLACK_BISHOP, BLACK_KNIGHT, BLACK_ROOK,
            BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN,
            EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
            EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
            EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
            EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,
            WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN,
            WHITE_ROOK, WHITE_KNIGHT, WHITE_BISHOP, WHITE_QUEEN, WHITE_KING, WHITE_BISHOP, WHITE_KNIGHT, WHITE_ROOK
    };

    private boolean isGameActive;
    private boolean isPlayerWhite;
    private boolean isWhiteToPlay;
    private boolean canEnPassant;
    private boolean canWhiteCastleKingside;
    private boolean canWhiteCastleQueenside;
    private boolean canBlackCastleKingside;
    private boolean canBlackCastleQueenside;
    private int fiftyMoveRuleCounter;
    private int[] board;
    private BiConsumer<Integer, Integer> updateClocksCallback;
    private Consumer<int[]> updateBoardCallback;
    private BiConsumer<Result, Boolean> endGameCallback;
    private BiConsumer<Integer, Integer> lastMoveCallback;
    private int playerClock;
    private int opponentClock;
    private int increment;
    private int lastMoveSquareIndex;
    private ScheduledFuture<?> clockFuture;
    private @Nullable String opponentId;

    private ChessGameManager() {
        isGameActive = false;
    }

    public boolean isGameActive() {
        return isGameActive;
    }

    public void searchForGame(Consumer<Boolean> resultCallback) {
        Firebase.getInstance().createOrJoinGame((opponentId, isPlayerWhite) -> {
            // Very important to set the opponent id before starting the game
            // As the UI will be using that immediately after calling the callback
            // And the game will be started asynchronously
            this.opponentId = opponentId;
            startGame(
                    STARTING_POSITION,
                    isPlayerWhite, 180, 180, 2
            );
            resultCallback.accept(true);
        }, game -> {
            System.out.println("Updating game...");
            assert game != null;
            int fromindex = game.moveFrom;
            int toindex = game.moveTo;
            if (fromindex == -1 || toindex == -1) return;
            System.out.println("Opponent moving piece from " + fromindex + " to " + toindex);
            boolean success = movePiece(getFile(fromindex), getRank(fromindex), getFile(toindex), getRank(toindex), true);
            System.out.println("Updated game " + (success ? "successfully" : "unsuccessfully"));
        }, result -> {
            // If game is not active it means this result came from this client, no need to do anything
            // Otherwise it means the opponent ended the game and we need to end it as well
            if (!isGameActive()) return;
            endGame(result);
        });
    }

    private void startGame(int[] board, boolean isPlayerWhite, int playerTime, int opponentTime, int increment) {
        this.isGameActive = true;
        this.isPlayerWhite = isPlayerWhite;
        this.isWhiteToPlay = true;
        this.canEnPassant = false;
        this.canWhiteCastleKingside = true;
        this.canWhiteCastleQueenside = true;
        this.canBlackCastleKingside = true;
        this.canBlackCastleQueenside = true;
        this.fiftyMoveRuleCounter = 0;
        this.increment = increment;
        this.lastMoveSquareIndex = -1;
        loadPosition(board);
        startClocks(playerTime, opponentTime);
        System.out.println("Game started");
    }

    public void endGame(Result result) {
        System.out.println("Game ended");
        isGameActive = false;
        if (clockFuture != null) {
            clockFuture.cancel(true);
            clockFuture = null;
        }
        endGameCallback.accept(result, isPlayerWhite);
        Firebase.getInstance().endGame(result);
    }

    private int toIndex(int file, int rank) {
        return (7 - rank) * FILE_COUNT + file;
    }

    public int getFile(int index) {
        return index % FILE_COUNT;
    }

    public int getRank(int index) {
        return 7 - (index / FILE_COUNT);
    }

    private void loadPosition(int[] board) {
        this.board = board.clone();
        if (updateBoardCallback != null) updateBoardCallback.accept(board);
    }

    public int getPiece(int file, int rank) {
        return board[toIndex(file, rank)];
    }

    private void setPiece(int file, int rank, int piece) {
        board[toIndex(file, rank)] = piece;
    }

    public boolean movePiece(int fromFile, int fromRank, int toFile, int toRank, boolean isOpponent) {
        if (!isOpponent) {
            boolean isValid = isMoveValid(fromFile, fromRank, toFile, toRank);
            if (!isValid) return false; // Invalid move
        }

        int pieceAtFrom = getPiece(fromFile, fromRank);
        int pieceAtTo = getPiece(toFile, toRank);

        // Update last move square index
        setLastMoveSquareIndex(toIndex(fromFile, fromRank));
        System.out.println("Last move square index: " + lastMoveSquareIndex);

        // Fifty move rule
        if (pieceAtFrom == WHITE_PAWN || pieceAtFrom == BLACK_PAWN || pieceAtTo != EMPTY) {
            fiftyMoveRuleCounter = 0;
        }
        fiftyMoveRuleCounter++;

        // Special case: castling
        if ((pieceAtFrom == WHITE_KING && fromRank == 0 && toRank == 0) ||
                (pieceAtFrom == BLACK_KING && fromRank == 7 && toRank == 7) &&
                        fromFile == 4 && Math.abs(fromFile - toFile) == 2) {
            // Note that no need to move the king, just the rook
            // Because the king will be moved to the destination square in the normal move
            int distance = toFile - fromFile;
            if (distance == 2) { // Kingside castling
                setPiece(5, fromRank, getPiece(7, fromRank));
                setPiece(7, fromRank, EMPTY);
            } else if (distance == -2) { // Queenside castling
                setPiece(3, fromRank, getPiece(0, fromRank));
                setPiece(0, fromRank, EMPTY);
            }
        }

        // Special case: promotion
        if (pieceAtFrom == WHITE_PAWN && toRank == 7) {
            setPiece(toFile, toRank, WHITE_QUEEN);
            setPiece(fromFile, fromRank, EMPTY);
            return afterMove(fromFile, fromRank, toFile, toRank, isOpponent);
        } else if (pieceAtFrom == BLACK_PAWN && toRank == 0) {
            setPiece(toFile, toRank, BLACK_QUEEN);
            setPiece(fromFile, fromRank, EMPTY);
            return afterMove(fromFile, fromRank, toFile, toRank, isOpponent);
        }

        // Special case: en passant
        if (canEnPassant &&
                (pieceAtFrom == WHITE_PAWN && fromRank == 4 && toRank == 5 && Math.abs(toFile - fromFile) == 1) ||
                (pieceAtFrom == BLACK_PAWN && fromRank == 3 && toRank == 2 && Math.abs(toFile - fromFile) == 1)) {
            setPiece(toFile, fromRank, EMPTY);
        }

        // Special case: moved pawn two squares
        canEnPassant = (pieceAtFrom == WHITE_PAWN && fromRank == 1 && toRank == 3) ||
                (pieceAtFrom == BLACK_PAWN && fromRank == 6 && toRank == 4);
        if (canEnPassant) System.out.println("En passant possible");

        // Normal move
        setPiece(toFile, toRank, pieceAtFrom);
        setPiece(fromFile, fromRank, EMPTY);

        // Update castling rights (king moves)
        if (pieceAtFrom == WHITE_KING) {
            canWhiteCastleKingside = false;
            canWhiteCastleQueenside = false;
        } else if (pieceAtFrom == BLACK_KING) {
            canBlackCastleKingside = false;
            canBlackCastleQueenside = false;
        }

        // Update castling rights (rook moves)
        if (pieceAtFrom == WHITE_ROOK || pieceAtTo == WHITE_ROOK) {
            if (fromFile == 0) canWhiteCastleQueenside = false;
            if (fromFile == 7) canWhiteCastleKingside = false;
        } else if (pieceAtFrom == BLACK_ROOK || pieceAtTo == BLACK_ROOK) {
            if (fromFile == 0) canBlackCastleQueenside = false;
            if (fromFile == 7) canBlackCastleKingside = false;
        }

        return afterMove(fromFile, fromRank, toFile, toRank, isOpponent);
    }

    private boolean afterMove(int fromFile, int fromRank, int toFile, int toRank, boolean isOpponent) {
        if (isWhiteToPlay && isPlayerWhite || !isWhiteToPlay && !isPlayerWhite) {
            playerClock += increment;
        } else {
            opponentClock += increment;
        }
        isWhiteToPlay = !isWhiteToPlay;
        if (updateBoardCallback != null) updateBoardCallback.accept(board);
        if (updateClocksCallback != null) updateClocksCallback.accept(playerClock, opponentClock);

        HashMap<@NotNull Integer, @NotNull Integer> pieceCount = new HashMap<>();
        for (int i = 0; i < FILE_COUNT * RANK_COUNT; i++) {
            int piece = board[i];
            if (piece == EMPTY) continue;
            pieceCount.put(piece, pieceCount.getOrDefault(piece, 0) + 1);
        }

        boolean whiteHasInsufficientMaterial = pieceCount.getOrDefault(WHITE_PAWN, 0) == 0 &&
                pieceCount.getOrDefault(WHITE_ROOK, 0) == 0 &&
                pieceCount.getOrDefault(WHITE_QUEEN, 0) == 0 &&
                !(pieceCount.getOrDefault(WHITE_BISHOP, 0) >= 1 &&
                        pieceCount.getOrDefault(WHITE_KNIGHT, 0) >= 1);
        boolean blackHasInsufficientMaterial =
                pieceCount.getOrDefault(BLACK_PAWN, 0) == 0 &&
                        pieceCount.getOrDefault(BLACK_ROOK, 0) == 0 &&
                        pieceCount.getOrDefault(BLACK_QUEEN, 0) == 0 &&
                        !(pieceCount.getOrDefault(BLACK_BISHOP, 0) >= 1 &&
                                pieceCount.getOrDefault(BLACK_KNIGHT, 0) >= 1);


        if (pieceCount.getOrDefault(WHITE_KING, 0) == 0) {
            endGame(Result.BLACK_WINS_CHECKMATE);
        } else if (pieceCount.getOrDefault(BLACK_KING, 0) == 0) {
            endGame(Result.WHITE_WINS_CHECKMATE);
        } else if (playerClock <= 0) {
            Result result;
            if (isPlayerWhite && blackHasInsufficientMaterial) {
                result = Result.DRAW_INSUFFICIENT_MATERIAL_VS_WHITE_TIMEOUTS;
            } else if (!isPlayerWhite && whiteHasInsufficientMaterial) {
                result = Result.DRAW_INSUFFICIENT_MATERIAL_VS_BLACK_TIMEOUTS;
            } else {
                result = isPlayerWhite ? Result.BLACK_WINS_WHITE_TIMEOUTS : Result.WHITE_WINS_BLACK_TIMEOUTS;
            }
            endGame(result);
        } else if (opponentClock <= 0) {
            Result result;
            if (isPlayerWhite && whiteHasInsufficientMaterial) {
                result = Result.DRAW_INSUFFICIENT_MATERIAL_VS_BLACK_TIMEOUTS;
            } else if (!isPlayerWhite && blackHasInsufficientMaterial) {
                result = Result.DRAW_INSUFFICIENT_MATERIAL_VS_WHITE_TIMEOUTS;
            } else {
                result = isPlayerWhite ? Result.WHITE_WINS_BLACK_TIMEOUTS : Result.BLACK_WINS_WHITE_TIMEOUTS;
            }
            endGame(result);
        } else if (fiftyMoveRuleCounter >= 100) {
            endGame(Result.DRAW_FIFTY_MOVE_RULE);
        } else if (whiteHasInsufficientMaterial && blackHasInsufficientMaterial) {
            endGame(Result.DRAW_INSUFFICIENT_MATERIAL);
        }

        if (isOpponent) return true;

        if (isGameActive()) {
            Firebase.getInstance().updateGame(toIndex(fromFile, fromRank), toIndex(toFile, toRank));
        }

        return true;
    }

    private boolean isMoveValid(int fromFile, int fromRank, int toFile, int toRank) {
        int pieceAtFrom = getPiece(fromFile, fromRank);
        int pieceAtTo = getPiece(toFile, toRank);

        System.out.println("Reached1");
        if (pieceAtFrom == EMPTY) return false; // No piece to move
        if (getColor(pieceAtFrom) != (isWhiteToPlay ? 0 : 1)) {
            System.out.println("Not your turn");
            return false; // Not your turn
        }
        System.out.println("Reached2");
        if (getColor(pieceAtFrom) == getColor(pieceAtTo)) return false; // Cannot capture own piece
        System.out.println("Reached3");

        // Check pawn moves
        if (pieceAtFrom == WHITE_PAWN || pieceAtFrom == BLACK_PAWN) {
            if (Math.abs(toFile - fromFile) == 1 && Math.abs(toRank - fromRank) == 1) {
                if (pieceAtTo != EMPTY) return true; // Capture diagonally

                // En passant
                if (canEnPassant &&
                        (pieceAtFrom == WHITE_PAWN && fromRank == 4 && toRank == 5 && Math.abs(toFile - fromFile) == 1) ||
                        (pieceAtFrom == BLACK_PAWN && fromRank == 3 && toRank == 2 && Math.abs(toFile - fromFile) == 1)) {
                    return true;
                }
            }

            if (pieceAtTo == EMPTY && toFile == fromFile) {
                // Moving forward
                if (pieceAtFrom == WHITE_PAWN && toRank == fromRank + 1) return true;
                if (pieceAtFrom == BLACK_PAWN && toRank == fromRank - 1) return true;
                if (pieceAtFrom == WHITE_PAWN && fromRank == 1 && toRank == 3 && getPiece(fromFile, 2) == EMPTY)
                    return true;
                if (pieceAtFrom == BLACK_PAWN && fromRank == 6 && toRank == 4 && getPiece(fromFile, 5) == EMPTY)
                    return true;
            }

            return false;
        }

        // Check knight moves
        if (pieceAtFrom == WHITE_KNIGHT || pieceAtFrom == BLACK_KNIGHT) {
            if (Math.abs(toFile - fromFile) == 2 && Math.abs(toRank - fromRank) == 1) return true;
            if (Math.abs(toFile - fromFile) == 1 && Math.abs(toRank - fromRank) == 2) return true;
            return false;
        }

        // Check bishop moves
        if (pieceAtFrom == WHITE_BISHOP || pieceAtFrom == BLACK_BISHOP) {
            if (Math.abs(toFile - fromFile) != Math.abs(toRank - fromRank)) return false;
            int fileDirection = toFile > fromFile ? 1 : -1;
            int rankDirection = toRank > fromRank ? 1 : -1;
            for (int file = fromFile + fileDirection, rank = fromRank + rankDirection;
                 file != toFile && rank != toRank;
                 file += fileDirection, rank += rankDirection) {
                if (getPiece(file, rank) != EMPTY) return false;
            }
            return true;
        }

        // Check rook moves
        if (pieceAtFrom == WHITE_ROOK || pieceAtFrom == BLACK_ROOK) {
            if (toFile != fromFile && toRank != fromRank) return false;
            if (toFile == fromFile) {
                int direction = toRank > fromRank ? 1 : -1;
                for (int rank = fromRank + direction; rank != toRank; rank += direction) {
                    if (getPiece(fromFile, rank) != EMPTY) return false;
                }
            } else {
                int direction = toFile > fromFile ? 1 : -1;
                for (int file = fromFile + direction; file != toFile; file += direction) {
                    if (getPiece(file, fromRank) != EMPTY) return false;
                }
            }
            return true;
        }

        // Check queen moves
        if (pieceAtFrom == WHITE_QUEEN || pieceAtFrom == BLACK_QUEEN) {
            if (Math.abs(toFile - fromFile) == Math.abs(toRank - fromRank)) {
                int fileDirection = toFile > fromFile ? 1 : -1;
                int rankDirection = toRank > fromRank ? 1 : -1;
                for (int file = fromFile + fileDirection, rank = fromRank + rankDirection;
                     file != toFile && rank != toRank;
                     file += fileDirection, rank += rankDirection) {
                    if (getPiece(file, rank) != EMPTY) return false;
                }
                return true;
            }

            if (toFile == fromFile || toRank == fromRank) {
                if (toFile == fromFile) {
                    int direction = toRank > fromRank ? 1 : -1;
                    for (int rank = fromRank + direction; rank != toRank; rank += direction) {
                        if (getPiece(fromFile, rank) != EMPTY) return false;
                    }
                } else {
                    int direction = toFile > fromFile ? 1 : -1;
                    for (int file = fromFile + direction; file != toFile; file += direction) {
                        if (getPiece(file, fromRank) != EMPTY) return false;
                    }
                }
                return true;
            }

            return false;
        }

        // Check king moves (pseudo-legal)
        if (pieceAtFrom == WHITE_KING || pieceAtFrom == BLACK_KING) {
            if (Math.abs(toFile - fromFile) <= 1 && Math.abs(toRank - fromRank) <= 1) return true;

            // Castling
            if (!(fromFile == 4 && (fromRank == 0 || fromRank == 7) && toRank == fromRank)) {
                return false;
            }

            if (toFile != 2 && toFile != 6) return false;

            // Kingside castling
            boolean canCastleKingside = isWhiteToPlay ? canWhiteCastleKingside : canBlackCastleKingside;
            if (canCastleKingside && toFile == 6 && getPiece(5, fromRank) == EMPTY && getPiece(6, fromRank) == EMPTY) {
                return true;
            }

            // Queenside castling
            boolean canCastleQueenside = isWhiteToPlay ? canWhiteCastleQueenside : canBlackCastleQueenside;
            if (canCastleQueenside && toFile == 2 && getPiece(3, fromRank) == EMPTY && getPiece(2, fromRank) == EMPTY &&
                    getPiece(1, fromRank) == EMPTY) {
                return true;
            }

            return false;
        }


        return false;
    }

    public int getColor(int piece) {
        if (piece == EMPTY) return -1;
        return piece < BLACK_PAWN ? 0 : 1;
        // 0 for white, 1 for black, -1 for empty
    }

    // Async (repeating task) to update clocks
    private void startClocks(int playerTime, int opponentTime) {
        this.playerClock = playerTime;
        this.opponentClock = opponentTime;

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        clockFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
//                System.out.println("Clock tick");

                // Waiting for white to play and player is white
                if (isWhiteToPlay && isPlayerWhite) {
                    playerClock--;
                }
                // Waiting for black to play and player is black
                else if (!isWhiteToPlay && !isPlayerWhite) {
                    playerClock--;
                }
                // Must be opponent's turn
                else {
                    opponentClock--;
                }

//                System.out.println("Updating clocks with " + playerClock + " and " + opponentClock);
                if (updateClocksCallback != null)
                    updateClocksCallback.accept(playerClock, opponentClock);

                if (playerClock <= 0) {
                    endGame(isPlayerWhite ? Result.BLACK_WINS_WHITE_TIMEOUTS : Result.WHITE_WINS_BLACK_TIMEOUTS);
                } else if (opponentClock <= 0) {
                    endGame(isPlayerWhite ? Result.WHITE_WINS_BLACK_TIMEOUTS : Result.BLACK_WINS_WHITE_TIMEOUTS);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public boolean isSquareLight(int index) {
        int file = getFile(index);
        int rank = getRank(index);
        return (file + rank) % 2 == 0;
    }

    public void setUpdateClocksCallback(BiConsumer<Integer, Integer> updateClocksCallback) {
        this.updateClocksCallback = updateClocksCallback;
    }

    public void setUpdateBoardCallback(Consumer<int[]> updateBoardCallback) {
        this.updateBoardCallback = updateBoardCallback;
    }

    public void setEndGameCallback(BiConsumer<Result, Boolean> endGameCallback) {
        this.endGameCallback = endGameCallback;
    }

    public void forceUpdate() {
        if (!isGameActive) return;
        if (updateBoardCallback != null) updateBoardCallback.accept(board);
        if (updateClocksCallback != null) updateClocksCallback.accept(playerClock, opponentClock);
        if (lastMoveCallback != null) lastMoveCallback.accept(-1, lastMoveSquareIndex);
    }

    public @Nullable String getOpponentId() {
        return opponentId;
    }

    public boolean isPlayerWhite() {
        return isPlayerWhite;
    }

    public int getLastMoveSquareIndex() {
        return lastMoveSquareIndex;
    }

    private void setLastMoveSquareIndex(int lastMoveSquareIndex) {
        int oldLastMoveSquareIndex = this.lastMoveSquareIndex;
        this.lastMoveSquareIndex = lastMoveSquareIndex;
        if (lastMoveCallback != null) lastMoveCallback.accept(oldLastMoveSquareIndex, lastMoveSquareIndex);
    }

    public void setUpdateLastMoveCallback(BiConsumer<Integer, Integer> lastMoveCallback) {
        this.lastMoveCallback = lastMoveCallback;
    }
}
