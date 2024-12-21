package main;

import piece.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable{
    public static final int WIDTH = 1100;
    public static final int HEIGHT = 800;
    final int FPS = 60;
    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    // pieces
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    ArrayList<Piece> promoPieces = new ArrayList<>();
    Piece activeP;
    Piece checkingP;
    public static Piece castlingP;

    // color
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;

    // booleans
    boolean canMove;
    boolean validSquare;
    boolean promotion;
    boolean gameOver;
    boolean stalemate;
    boolean scoreUpdated = false;
    boolean scoreStart = false;

    // background
    BufferedImage menuBackground;

    // buttons
    Rectangle playAgainButton = new Rectangle(250, 400, 400, 70);
    Rectangle returnButton = new Rectangle(250, 500, 400, 70);

    Rectangle pauseButton = new Rectangle(1000, 20, 80, 40);
    Rectangle continueButton = new Rectangle(400, 250, 300, 70);
    Rectangle restartButton = new Rectangle(400, 350, 300, 70);
    Rectangle returnButton3 = new Rectangle(400, 450, 300, 70);

    Rectangle playButton = new Rectangle(450, 300, 200, 70);
    Rectangle statsButton = new Rectangle(450, 400, 200, 70);
    Rectangle settingsButton = new Rectangle(700, 400,70,70);
    Rectangle exitButton = new Rectangle(450, 500, 200, 70);

    Rectangle resetButton = new Rectangle(450, 550, 200, 70);
    Rectangle returnButton2 = new Rectangle(475, 640, 150, 70);

    Rectangle yesButton = new Rectangle(425, 350, 100, 70);
    Rectangle noButton = new Rectangle(575, 350, 100, 70);

    // stats
    int whiteScore = 0;
    int blackScore = 0;
    int stalemateCount = 0;
    int currentWhiteScore = 0;
    int currentBlackScore = 0;
    int currentStalemateCount = 0;
    long totalPlaytime = 0;
    long startTime = 0;
    File saveFile = new File("save.txt");

    // game state
    enum GameState {MENU, GAME, STATS, PAUSE, WARNING_RESET, SETTINGS}
    GameState currentState = GameState.MENU;

    // sound effect
    private final SoundEffect pieceMoveSound;
    private final SoundEffect checkSound;
    private final SoundEffect castleSound;
    private final SoundEffect buttonSound;
    private final SoundEffect promoteSound;

    // Background music
    private final Clip menuMusic;
    private final Clip gameMusic;
    private final Clip checkMusic;
    private final Clip winMusic;
    private final Clip stalemateMusic;

    // settings
    BufferedImage settingsIcon;
    float bgmVolume = 0.0f;
    float sfxVolume = 0.0f;

    public GamePanel() {
        pieceMoveSound = new SoundEffect("res/sfx/piece_move.wav");
        checkSound = new SoundEffect("res/sfx/check.wav");
        castleSound = new SoundEffect("res/sfx/castle.wav");
        buttonSound = new SoundEffect("res/sfx/button.wav");
        promoteSound = new SoundEffect("res/sfx/promote.wav");

        menuMusic = loadMusic("res/bgm/menu.wav");
        gameMusic = loadMusic("res/bgm/game.wav");
        checkMusic = loadMusic("res/bgm/check.wav");
        winMusic = loadMusic("res/bgm/win.wav");
        stalemateMusic = loadMusic("res/bgm/stalemate.wav");

        setPreferredSize(new Dimension(WIDTH,HEIGHT));
        setBackground(Color.black);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);

        setPieces();
        copyPieces(pieces, simPieces);

        loadGameData();

        try {
            menuBackground = ImageIO.read(new File("res/menu.jpg"));
        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
        }

        try {
            settingsIcon = ImageIO.read(new File("res/settings.png"));
        } catch (IOException e) {
            System.err.println("Error loading settings icon: " + e.getMessage());
        }
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
        if(menuMusic != null) {
            menuMusic.loop(Clip.LOOP_CONTINUOUSLY);
            menuMusic.start();
        }
    }

    private void resetGame() {
        pieces.clear();
        setPieces();
        copyPieces(pieces, simPieces);
        currentColor = WHITE;
        gameOver = false;
        stalemate = false;
        activeP = null;
        checkingP = null;
        promotion = false;
        scoreUpdated = false;
    }

    private void saveGameData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile))) {
            writer.write(blackScore + "\n");
            writer.write(whiteScore + "\n");
            writer.write(stalemateCount + "\n");
            writer.write(totalPlaytime + "\n");
            writer.write(bgmVolume + "\n");
            writer.write(sfxVolume + "\n");
        } catch (IOException e) {
            System.err.println("Error saving game data: " + e.getMessage());
        }
    }

    private void loadGameData() {
        if (saveFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(saveFile))) {
                blackScore = Integer.parseInt(reader.readLine());
                whiteScore = Integer.parseInt(reader.readLine());
                stalemateCount = Integer.parseInt(reader.readLine());
                totalPlaytime = Long.parseLong(reader.readLine());
                bgmVolume = Float.parseFloat(reader.readLine());
                sfxVolume = Float.parseFloat(reader.readLine());
                pieceMoveSound.setVolume(sfxVolume);
                checkSound.setVolume(sfxVolume);
                castleSound.setVolume(sfxVolume);
                promoteSound.setVolume(sfxVolume);
                buttonSound.setVolume(sfxVolume);
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error loading game data: " + e.getMessage());
            }
        }
    }

    private void resetSaveFile() {
        whiteScore = 0;
        blackScore = 0;
        stalemateCount = 0;
        totalPlaytime = 0;

        if(saveFile.exists()) {
            saveFile.delete();
        }
    }

    private Clip loadMusic(String filePath) {
        try {
            File audioFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading music: " + e.getMessage());
            return null;
        }
    }

    private void stopMusic(Clip clip) {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    private void setVolume(Clip clip, float volume) {
        if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = Math.max(-80.0f, Math.min(6.0f, volume - 80)); // Clamp volume between -80 dB and 6 dB
            gainControl.setValue(dB);
        }
    }

    public void setPieces() {
        // white pieces
        pieces.add(new Pawn(WHITE, 0, 6));
        pieces.add(new Pawn(WHITE, 1, 6));
        pieces.add(new Pawn(WHITE, 2, 6));
        pieces.add(new Pawn(WHITE, 3, 6));
        pieces.add(new Pawn(WHITE, 4, 6));
        pieces.add(new Pawn(WHITE, 5, 6));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 7, 6));
        pieces.add(new Rook(WHITE, 0, 7));
        pieces.add(new Knight(WHITE, 1, 7));
        pieces.add(new Bishop(WHITE, 2, 7));
        pieces.add(new Queen(WHITE, 3, 7));
        pieces.add(new King(WHITE, 4, 7));
        pieces.add(new Bishop(WHITE, 5, 7));
        pieces.add(new Knight(WHITE, 6, 7));
        pieces.add(new Rook(WHITE, 7, 7));
        // black pieces
        pieces.add(new Pawn(BLACK, 0, 1));
        pieces.add(new Pawn(BLACK, 1, 1));
        pieces.add(new Pawn(BLACK, 2, 1));
        pieces.add(new Pawn(BLACK, 3, 1));
        pieces.add(new Pawn(BLACK, 4, 1));
        pieces.add(new Pawn(BLACK, 5, 1));
        pieces.add(new Pawn(BLACK, 6, 1));
        pieces.add(new Pawn(BLACK, 7, 1));
        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Knight(BLACK, 1, 0));
        pieces.add(new Bishop(BLACK, 2, 0));
        pieces.add(new Queen(BLACK, 3, 0));
        pieces.add(new King(BLACK, 4, 0));
        pieces.add(new Bishop(BLACK, 5, 0));
        pieces.add(new Knight(BLACK, 6, 0));
        pieces.add(new Rook(BLACK, 7, 0));
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        for(int i = 0; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while(gameThread != null) {
            currentTime = System.nanoTime();

            delta += (currentTime - lastTime)/drawInterval;
            lastTime = currentTime;

            if(delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        if(currentState == GameState.MENU && mouse.pressed) {
            stopMusic(gameMusic);
            stopMusic(checkMusic);
            stopMusic(winMusic);
            stopMusic(stalemateMusic);
            if(playButton.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                currentState = GameState.GAME;
                startTime = System.currentTimeMillis();

                stopMusic(menuMusic);
                if(gameMusic != null) {
                    gameMusic.loop(Clip.LOOP_CONTINUOUSLY);
                    gameMusic.start();
                }
            } else if(statsButton.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                currentState = GameState.STATS;
            } else if (settingsButton.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                currentState = GameState.SETTINGS; // Open settings menu
            }
            else if(exitButton.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                System.exit(0);
            }
            mouse.pressed = false;
        } else if(currentState == GameState.STATS && mouse.pressed) {
            if(returnButton2.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                currentState = GameState.MENU;
            }
            if(resetButton.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                currentState = GameState.WARNING_RESET;
            }
            mouse.pressed = false;
        } else if (currentState == GameState.SETTINGS && mouse.pressed) {
            Rectangle bgmSlider = new Rectangle(300, 220, 300, 20);
            Rectangle sfxSlider = new Rectangle(300, 320, 300, 20);
            if (bgmSlider.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                // bgm
                bgmVolume = (mouse.x - 300) / 3.0f;
                setVolume(menuMusic, bgmVolume);
                setVolume(gameMusic, bgmVolume);
                setVolume(checkMusic, bgmVolume);
                setVolume(winMusic, bgmVolume);
                setVolume(stalemateMusic, bgmVolume);
            } else if (sfxSlider.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                // sfx
                sfxVolume = (mouse.x - 300) / 3.0f;
                pieceMoveSound.setVolume(sfxVolume);
                checkSound.setVolume(sfxVolume);
                castleSound.setVolume(sfxVolume);
                promoteSound.setVolume(sfxVolume);
                buttonSound.setVolume(sfxVolume);
            } else if (returnButton2.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                currentState = GameState.MENU;
            }
            mouse.pressed = false;
        } else if(currentState == GameState.WARNING_RESET && mouse.pressed) {
            if(yesButton.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                resetSaveFile();
                currentState = GameState.STATS;
            }
            if (noButton.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                currentState = GameState.STATS;
            }
            mouse.pressed = false;
        } else if(currentState == GameState.PAUSE && mouse.pressed) {
            if(continueButton.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                currentState = GameState.GAME;
            }
            if(restartButton.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                resetGame();
                currentState = GameState.GAME;
                stopMusic(checkMusic);
                if (gameMusic != null) {
                    gameMusic.loop(Clip.LOOP_CONTINUOUSLY);
                    gameMusic.start();
                }
            }
            if(returnButton3.contains(mouse.x, mouse.y)) {
                buttonSound.play();
                totalPlaytime += System.currentTimeMillis() - startTime;
                saveGameData();
                resetGame();
                currentState = GameState.MENU;
                stopMusic(gameMusic);
                stopMusic(checkMusic);
                if (menuMusic != null) {
                    menuMusic.loop(Clip.LOOP_CONTINUOUSLY);
                    menuMusic.start();
                }
            }
            mouse.pressed = false;
        } else if(currentState == GameState.GAME) {
            if(!scoreStart) {
                currentWhiteScore = 0;
                currentBlackScore = 0;
                currentStalemateCount = 0;
                scoreStart = true;
            }

            if (pauseButton.contains(mouse.x, mouse.y) && mouse.pressed) {
                buttonSound.play();
                currentState = GameState.PAUSE;
            }

            // Replay and return to menu
            if((gameOver || stalemate) && mouse.pressed) {
                totalPlaytime += System.currentTimeMillis() - startTime;
                saveGameData();
                if(playAgainButton.contains(mouse.x, mouse.y)) {
                    buttonSound.play();
                    resetGame();
                    startTime = System.currentTimeMillis();
                    mouse.pressed = false;
                    stopMusic(winMusic);
                    stopMusic(stalemateMusic);
                    if (gameMusic != null) {
                        gameMusic.loop(Clip.LOOP_CONTINUOUSLY);
                        gameMusic.start();
                    }
                }
                if(returnButton.contains(mouse.x, mouse.y)) {
                    buttonSound.play();
                    resetGame();
                    scoreStart = false;
                    currentState = GameState.MENU;
                    mouse.pressed = false;
                    stopMusic(winMusic);
                    stopMusic(stalemateMusic);
                    if (menuMusic != null) {
                        menuMusic.loop(Clip.LOOP_CONTINUOUSLY);
                        menuMusic.start();
                    }
                }
            }

            if(promotion) {
                promoting();
            } else if(!gameOver && !stalemate) {
                // mouse press
                if(mouse.pressed) {
                    if(activeP == null) {
                        // if activeP is null, you can pick up a piece
                        for(Piece piece : simPieces) {
                            // if on an ally piece, pick up as activeP
                            if(piece.color == currentColor &&
                                    piece.col == mouse.x / Board.SQUARE_SIZE &&
                                    piece.row == mouse.y / Board.SQUARE_SIZE) {
                                activeP = piece;
                            }
                        }
                    } else {
                        // simulate move when holding a piece
                        simulate();
                    }
                }
                // mouse release
                if(!mouse.pressed) {
                    if(activeP != null) {
                        if(validSquare) {
                            // move confirmed
                            // update piece list (remove captured piece)
                            copyPieces(simPieces, pieces);
                            activeP.updatePosition();

                            if(inCheck(activeP) && !isCheckmate()) {
                                checkSound.play();
                            }

                            if(castlingP != null) {
                                castleSound.play();
                                castlingP.updatePosition();
                            }

                            if(!inCheck(activeP)) {
                                stopMusic(checkMusic);
                                if (gameMusic != null && !gameMusic.isRunning()) {
                                    gameMusic.loop(Clip.LOOP_CONTINUOUSLY);
                                    gameMusic.start();
                                }
                            }

                            if(inCheck(activeP) && isCheckmate()) {
                                changePlayer();
                                checkSound.play();
                                gameOver = true;
                                if(!scoreUpdated) {
                                    if (currentColor == BLACK) {
                                        currentWhiteScore++;
                                        whiteScore++;
                                    } else {
                                        currentBlackScore++;
                                        blackScore++;
                                    }
                                    scoreUpdated = true;
                                }
                                stopMusic(gameMusic);
                                stopMusic(checkMusic);
                                if(winMusic != null) {
                                    winMusic.start();
                                }
                            } else if(isStalemate() && !inCheck(activeP)) {
                                changePlayer();
                                stalemate = true;
                                if(!scoreUpdated) {
                                    currentStalemateCount++;
                                    stalemateCount++;
                                    scoreUpdated = true;
                                }
                                stopMusic(gameMusic);
                                stopMusic(checkMusic);
                                if(stalemateMusic != null) {
                                    stalemateMusic.start();
                                }
                            } else { // game still continue
                                if(canPromote()) {
                                    promotion = true;
                                } else {
                                    pieceMoveSound.play();
                                    changePlayer();
                                }
                            }
                        } else {
                            // reset due to invalid move
                            copyPieces(pieces, simPieces);
                            activeP.resetPosition();
                            activeP = null;
                        }
                    }
                }
            }
        }
    }

    private void simulate() {
        canMove = false;
        validSquare = false;

        // reset piece list
        copyPieces(pieces, simPieces);

        // reset castling piece
        if(castlingP != null) {
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }

        // update piece position
        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        // valid move check
        if(activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;

            // capture validation
            if(activeP.hittingP != null) {
                simPieces.remove(activeP.hittingP.getIndex());
            }

            checkCastling();

            if(!isIllegal(activeP) && !canCaptureKing()) {
                validSquare = true;
            }
        }
    }

    private boolean isIllegal(Piece king) {
        if(king.type == Type.KING) {
            for(Piece piece : simPieces) {
                if(piece != king && piece.color != king.color && piece.canMove(king.col, king.row)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canCaptureKing() {
        Piece king = getKing(false);

        for(Piece piece : simPieces) {
            if(piece.color != king.color && piece.canMove(king.col, king.row)) {
                return true;
            }
        }
        return false;
    }

    private boolean inCheck(Piece activeP) {
        Piece king = getKing(true);

        // Check if the given piece puts the opponent's king in check
        if(activeP != null && activeP.canMove(king.col, king.row)) {
            checkingP = activeP;
            stopMusic(gameMusic);
            if (checkMusic != null) {
                checkMusic.loop(Clip.LOOP_CONTINUOUSLY);
                checkMusic.start();
            }
            return true;
        } else {
            checkingP = null;
        }
        return false;
    }

    private Piece getKing(boolean opponent) {
        Piece king = null;

        for(Piece piece : simPieces) {
            if(opponent) {
                if(piece.type == Type.KING && piece.color != currentColor) {
                    king = piece;
                }
            } else {
                if(piece.type == Type.KING && piece.color == currentColor) {
                    king = piece;
                }
            }
        }
        return king;
    }

    private boolean isCheckmate() {
        Piece king = getKing(true);

        if(kingCanMove(king)) {
            return false;
        } else {
            // check if attack can be blocked

            // check position of checking pieces and king in check
            int colDiff = Math.abs(checkingP.col - king.col);
            int rowDiff = Math.abs(checkingP.row - king.row);

            if(colDiff == 0) {
                // vertical check
                if(checkingP.row < king.row) {
                    // above
                    for(int row = checkingP.row; row < king.row; row++) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }
                if(checkingP.row > king.row) {
                    // below
                    for(int row = checkingP.row; row > king.row; row--) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if(rowDiff == 0) {
                // horizontal check
                if(checkingP.col < king.col) {
                    // left
                    for(int col = checkingP.col; col < king.col; col++) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }
                if(checkingP.col > king.col) {
                    // right
                    for(int col = checkingP.col; col > king.col; col--) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if(colDiff == rowDiff) {
                // diagonal check
                if(checkingP.row < king.row) {
                    // above
                    if(checkingP.col < king.col) {
                        // up left
                        for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row++) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    if(checkingP.col > king.col) {
                        // up right
                        for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row++) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                if(checkingP.row > king.row) {
                    // below
                    if(checkingP.col < king.col) {
                        // down left
                        for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row--) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    if(checkingP.col > king.col) {
                        // down right
                        for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row--) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        // no knight check can be blocked
        return true;
    }

    private boolean kingCanMove(Piece king) {
        // simulate possible king moves
        if(isValidMove(king, -1, -1)) return true;
        if(isValidMove(king, 0, -1)) return true;
        if(isValidMove(king, 1, -1)) return true;
        if(isValidMove(king, -1, 0)) return true;
        if(isValidMove(king, 1, 0)) return true;
        if(isValidMove(king, -1, 1)) return true;
        if(isValidMove(king, 0, 1)) return true;
        if(isValidMove(king, 1, 1)) return true;
        return false;
    }

    private boolean isValidMove(Piece king, int colPlus, int rowPlus) {
        boolean isValidMove = false;
        // update king position temporarily
        king.col += colPlus;
        king.row += rowPlus;

        if(king.canMove(king.col, king.row)) {
            if(king.hittingP != null) {
                simPieces.remove(king.hittingP.getIndex());
            }
            if(!isIllegal(king)) {
                isValidMove = true;
            }
        }

        // reset king position
        king.resetPosition();
        copyPieces(pieces, simPieces);

        return isValidMove;
    }

    private boolean isStalemate() {
        int count = 0;
        // count pieces
        for(Piece piece : simPieces) {
            if(piece.color != currentColor) {
                count++;
            }
        }

        // only king left
        if(count == 1) {
            if(!kingCanMove(getKing(true))) {
                return true;
            }
        }
        return false;
    }

    private void checkCastling() {
        if(castlingP != null) {
            if(castlingP.col == 0) {
                castlingP.col += 3;
            } else if (castlingP.col == 7) {
                castlingP.col -= 2;
            }
            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    private void changePlayer() {
        if(currentColor == WHITE) {
            currentColor = BLACK;
            // reset black two stepped status
            for(Piece piece : pieces) {
                if(piece.color == BLACK) {
                    piece.twoStepped = false;
                }
            }
        } else {
            currentColor = WHITE;
            // reset white two stepped status
            for(Piece piece : pieces) {
                if(piece.color == WHITE) {
                    piece.twoStepped = false;
                }
            }
        }
        activeP = null;
    }

    private boolean canPromote() {
        if(activeP.type == Type.PAWN) {
            if(currentColor == WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7) {
                promoPieces.clear();
                promoPieces.add(new Rook(currentColor,9,2));
                promoPieces.add(new Knight(currentColor,9,3));
                promoPieces.add(new Bishop(currentColor,9,4));
                promoPieces.add(new Queen(currentColor,9,5));
                return true;
            }
        }
        return false;
    }

    private void promoting() {
        if(mouse.pressed) {
            for(Piece piece : promoPieces) {
                if(piece.col == mouse.x / Board.SQUARE_SIZE && piece.row == mouse.y / Board.SQUARE_SIZE) {
                    Piece promotedPiece = null; // Keep track of the new promoted piece

                    // Add the promoted piece to the simulation
                    switch (piece.type) {
                        case ROOK:
                            promotedPiece = new Rook(currentColor, activeP.col, activeP.row);
                            break;
                        case KNIGHT:
                            promotedPiece = new Knight(currentColor, activeP.col, activeP.row);
                            break;
                        case BISHOP:
                            promotedPiece = new Bishop(currentColor, activeP.col, activeP.row);
                            break;
                        case QUEEN:
                            promotedPiece = new Queen(currentColor, activeP.col, activeP.row);
                            break;
                        default:
                            break;
                    }

                    if(promotedPiece != null) {
                        simPieces.add(promotedPiece);
                        simPieces.remove(activeP.getIndex()); // Remove the pawn being promoted
                        copyPieces(simPieces, pieces);        // Update the game state

                        activeP = null; // Clear active piece
                        promotion = false; // End promotion state

                        // Check for game-over conditions
                        if(inCheck(promotedPiece) && isCheckmate()) {
                            checkSound.play();
                            gameOver = true;
                        } else if(isStalemate() && !inCheck(promotedPiece)) {
                            stalemate = true;
                        } else {
                            promoteSound.play();
                            changePlayer(); // Continue the game
                        }
                    }
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        if(currentState == GameState.MENU) {
            if (menuBackground != null) {
                g2.drawImage(menuBackground, 0, 0, WIDTH, HEIGHT, null);
            }
            if (settingsIcon != null) {
                g2.drawImage(settingsIcon, settingsButton.x, settingsButton.y, settingsButton.width, settingsButton.height, null);
            }

            g2.setFont(new Font("Arial", Font.BOLD, 90));
            g2.setColor(Color.yellow);
            g2.drawString("Chess Game", 300, 200);

            g2.setFont(new Font("Arial", Font.PLAIN, 50));
            g2.setColor(Color.white);
            g2.drawString("Play", playButton.x + 50, playButton.y + 50);
            g2.drawRect(playButton.x, playButton.y, playButton.width, playButton.height);

            g2.drawString("Stats", statsButton.x + 45, statsButton.y + 50);
            g2.drawRect(statsButton.x, statsButton.y, statsButton.width, statsButton.height);

            g2.drawString("Exit", exitButton.x + 55, exitButton.y + 50);
            g2.drawRect(exitButton.x, exitButton.y, exitButton.width, exitButton.height);

        } else if(currentState == GameState.GAME) {
            board.draw(g2);

            for(Piece p : simPieces) {
                p.draw(g2);
            }

            if(!gameOver || !stalemate) {
                g2.setFont(new Font("Arial", Font.PLAIN, 20));
                g2.setColor(Color.white);
                g2.drawString("Pause", pauseButton.x + 10, pauseButton.y + 30);
                g2.drawRect(pauseButton.x, pauseButton.y, pauseButton.width, pauseButton.height);
            }

            if(activeP != null) {
                if(canMove) {
                    if(isIllegal(activeP) || canCaptureKing()) {
                        g2.setColor(Color.gray);
                    } else {
                        g2.setColor(Color.white);
                    }

                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
                activeP.draw(g2);
            }

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(new Font("Palatino Linotype", Font.PLAIN, 40));
            g2.setColor(Color.white);

            if(promotion) {
                g2.drawString("Promote to: ", 840, 150);
                for(Piece piece : promoPieces) {
                    g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE, null);
                }
            } else {
                if(currentColor == WHITE) {
                    g2.drawString("White's Turn", 840, 550);
                } else {
                    g2.drawString("Black's Turn", 840, 250);
                }

                g2.setFont(new Font("Palatino Linotype", Font.PLAIN, 40));
                g2.setColor(Color.white);
                g2.drawString("Scores:", 840, 330);
                g2.drawString("Black: " + currentBlackScore, 840, 380);
                g2.drawString("White: " + currentWhiteScore, 840, 430);
                g2.drawString("Stalemates: " + currentStalemateCount, 840, 480);
            }

            if (checkingP != null) {
                Piece king = getKing(false);
                if (king != null) {
                    int kingCol = king.preCol;
                    int kingRow = king.preRow;
                    g2.setColor(new Color(255, 0, 0, 128));
                    g2.fillRect(kingCol * Board.SQUARE_SIZE, kingRow * Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                }
            }

            if(gameOver || stalemate) {
                g2.setFont(new Font("Arial", Font.PLAIN, 90));
                g2.setColor(gameOver ? Color.green : Color.gray);
                g2.drawString(gameOver ? (currentColor == WHITE ? "Black Wins!" : "White Wins!") : "Stalemate", 210, 350);

                g2.setFont(new Font("Arial", Font.PLAIN, 50));
                g2.setColor(Color.blue);
                g2.drawString("Play Again", playAgainButton.x + 80, playAgainButton.y + 50);
                g2.drawRect(playAgainButton.x, playAgainButton.y, playAgainButton.width, playAgainButton.height);

                g2.drawString("Return", returnButton.x + 125, returnButton.y + 50);
                g2.drawRect(returnButton.x, returnButton.y, returnButton.width, returnButton.height);
            }
        } else if (currentState == GameState.PAUSE) {
            board.draw(g2);

            for(Piece p : simPieces) {
                p.draw(g2);
            }

            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.setColor(Color.white);
            g2.drawString("Pause", pauseButton.x + 10, pauseButton.y + 30);
            g2.drawRect(pauseButton.x, pauseButton.y, pauseButton.width, pauseButton.height);

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(new Font("Palatino Linotype", Font.PLAIN, 40));
            g2.setColor(Color.white);

            if(promotion) {
                g2.drawString("Promote to: ", 840, 150);
                for(Piece piece : promoPieces) {
                    g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE, null);
                }
            } else {
                if(currentColor == WHITE) {
                    g2.drawString("White's Turn", 840, 550);
                } else {
                    g2.drawString("Black's Turn", 840, 250);
                }

                g2.setFont(new Font("Palatino Linotype", Font.PLAIN, 40));
                g2.setColor(Color.white);
                g2.drawString("Scores:", 840, 330);
                g2.drawString("Black: " + currentBlackScore, 840, 380);
                g2.drawString("White: " + currentWhiteScore, 840, 430);
                g2.drawString("Stalemates: " + currentStalemateCount, 840, 480);
            }

            if (checkingP != null) {
                Piece king = getKing(false);
                if (king != null) {
                    int kingCol = king.preCol;
                    int kingRow = king.preRow;
                    g2.setColor(new Color(255, 0, 0, 128));
                    g2.fillRect(kingCol * Board.SQUARE_SIZE, kingRow * Board.SQUARE_SIZE,
                            Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                }
            }

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.setColor(Color.black);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            g2.setFont(new Font("Arial", Font.BOLD, 90));
            g2.setColor(Color.white);
            g2.drawString("Pause", 425, 200);

            g2.setFont(new Font("Arial", Font.PLAIN, 50));
            g2.drawString("Continue", continueButton.x + 50, continueButton.y + 50);
            g2.drawRect(continueButton.x, continueButton.y, continueButton.width, continueButton.height);

            g2.drawString("Restart", restartButton.x + 70, restartButton.y + 50);
            g2.drawRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height);

            g2.drawString("Exit", returnButton3.x + 105, returnButton3.y + 50);
            g2.drawRect(returnButton3.x, returnButton3.y, returnButton3.width, returnButton3.height);
        } else if(currentState == GameState.STATS) {
            g2.setFont(new Font("Arial", Font.BOLD, 90));
            g2.setColor(Color.white);
            g2.drawString("Stats", 450, 200);

            g2.setFont(new Font("Arial", Font.PLAIN, 30));
            g2.drawString("Black Wins: " + blackScore, 300, 350);
            g2.drawString("White Wins: " + whiteScore, 300, 400);
            g2.drawString("Stalemates: " + stalemateCount, 300, 450);

            long minutes = (totalPlaytime / 1000) / 60;
            long seconds = (totalPlaytime / 1000) % 60;
            g2.drawString("Total Playtime: " + minutes + " min " + seconds + " sec", 300, 500);

            g2.setColor(Color.red);
            g2.drawString("Reset Data", resetButton.x + 25, resetButton.y + 50);
            g2.drawRect(resetButton.x, resetButton.y, resetButton.width, resetButton.height);

            g2.setColor(Color.white);
            g2.drawString("Back", returnButton2.x + 40, returnButton2.y + 45);
            g2.drawRect(returnButton2.x, returnButton2.y, returnButton2.width, returnButton2.height);
        } else if(currentState == GameState.WARNING_RESET) {
            g2.setFont(new Font("Arial", Font.BOLD, 50));
            g2.setColor(Color.red);
            g2.drawString("Are you sure?", 400, 300);
            g2.setFont(new Font("Arial", Font.PLAIN, 40));

            g2.drawString("Yes", yesButton.x + 15, yesButton.y + 50);
            g2.drawRect(yesButton.x, yesButton.y, yesButton.width, yesButton.height);

            g2.setColor(Color.white);
            g2.drawString("No", noButton.x + 25, noButton.y + 50);
            g2.drawRect(noButton.x, noButton.y, noButton.width, noButton.height);
        } else if (currentState == GameState.SETTINGS) {
            g2.setFont(new Font("Arial", Font.BOLD, 60));
            g2.setColor(Color.white);
            g2.drawString("Settings", 400, 100);

            g2.setFont(new Font("Arial", Font.PLAIN, 30));
            g2.drawString("BGM Volume:", 300, 200);
            g2.drawRect(300, 220, 300, 20);
            g2.fillRect(300, 220, (int) (bgmVolume * 3), 20);

            g2.drawString("SFX Volume:", 300, 300);
            g2.drawRect(300, 320, 300, 20);
            g2.fillRect(300, 320, (int) (sfxVolume * 3), 20);

            g2.drawString("Back", returnButton2.x + 40, returnButton2.y + 45);
            g2.drawRect(returnButton2.x, returnButton2.y, returnButton2.width, returnButton2.height);
        }
    }
}