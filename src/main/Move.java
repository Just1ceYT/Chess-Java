package main;

import piece.Piece;

public class Move {
    public Piece piece; // The piece to move
    public int targetCol; // Target column
    public int targetRow; // Target row
    public Piece capturedPiece; // Piece to be captured, if any

    public Move(Piece piece, int targetCol, int targetRow, Piece capturedPiece) {
        this.piece = piece;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
        this.capturedPiece = capturedPiece;
    }
}
