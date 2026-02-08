import type { Board, CastlingRights, Colour, Move, Piece, Square, Squares } from "@/types/Chess.type";
import Black_Pawn from '../assets/chess-pieces/cburnett/Black_Pawn.svg';
import Black_Knight from '../assets/chess-pieces/cburnett/Black_Knight.svg';
import Black_Bishop from '../assets/chess-pieces/cburnett/Black_Bishop.svg';
import Black_Rook from '../assets/chess-pieces/cburnett/Black_Rook.svg';
import Black_Queen from '../assets/chess-pieces/cburnett/Black_Queen.svg';
import Black_King from '../assets/chess-pieces/cburnett/Black_King.svg';
import White_Pawn from '../assets/chess-pieces/cburnett/White_Pawn.svg';
import White_Knight from '../assets/chess-pieces/cburnett/White_Knight.svg';
import White_Bishop from '../assets/chess-pieces/cburnett/White_Bishop.svg';
import White_Rook from '../assets/chess-pieces/cburnett/White_Rook.svg';
import White_Queen from '../assets/chess-pieces/cburnett/White_Queen.svg';
import White_King from '../assets/chess-pieces/cburnett/White_King.svg';

export const STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

export const pieceToSvg: (piece: Piece) => string = (piece) => {
    const { colour, pieceType } = piece;
    const pieceToSvg = {
        'WHITE': {
            'PAWN': White_Pawn,
            'KNIGHT': White_Knight,
            'BISHOP': White_Bishop,
            'ROOK': White_Rook,
            'QUEEN': White_Queen,
            'KING': White_King,
        },
        'BLACK': {
            'PAWN': Black_Pawn,
            'KNIGHT': Black_Knight,
            'BISHOP': Black_Bishop,
            'ROOK': Black_Rook,
            'QUEEN': Black_Queen,
            'KING': Black_King,
        }
    }

    return pieceToSvg[colour][pieceType];
}

export const charToPieceMap: Record<string, Piece> = {
    'P': { pieceType: 'PAWN', colour: 'WHITE' },
    'N': { pieceType: 'KNIGHT', colour: 'WHITE' },
    'B': { pieceType: 'BISHOP', colour: 'WHITE' },
    'R': { pieceType: 'ROOK', colour: 'WHITE' },
    'Q': { pieceType: 'QUEEN', colour: 'WHITE' },
    'K': { pieceType: 'KING', colour: 'WHITE' },
    'p': { pieceType: 'PAWN', colour: 'BLACK' },
    'n': { pieceType: 'KNIGHT', colour: 'BLACK' },
    'b': { pieceType: 'BISHOP', colour: 'BLACK' },
    'r': { pieceType: 'ROOK', colour: 'BLACK' },
    'q': { pieceType: 'QUEEN', colour: 'BLACK' },
    'k': { pieceType: 'KING', colour: 'BLACK' },
};

export const fenToBoard: (fen: string) => Board = (fen) => {
    const fenParts = fen.trim().split(' ');

    const squares: Squares = Array(64).fill(null) as Squares;
    const sideToMove: Colour = fenParts[1].charAt(0) === 'w' ? 'WHITE' : 'BLACK';
    const castlingRights: CastlingRights = {
        whiteKingSide: fenParts[2].includes('K'),
        whiteQueenSide: fenParts[2].includes('Q'),
        blackKingSide: fenParts[2].includes('k'),
        blackQueenSide: fenParts[2].includes('q'),
    };
    let enPassantSquare: Square | null = null;
    const halfMoveClock = parseInt(fenParts[4]);
    const fullMoveNumber = parseInt(fenParts[5]);

    if (fenParts[3].charAt(0) !== '-') {
        enPassantSquare = algebraicToSquare(fenParts[3])
    }

    /**
     * .reversed() is not used here, differing from the implementation in the backend
     * because the frontend fills in the chessboard top-to-bottom instead of bottom-to-top (which the backend does)
     */
    const boardFEN = fenParts[0].trim().split('/').join('');

    let i = 0;
    for (const char of boardFEN) {
        const piece = charToPieceMap[char];

        if (piece !== undefined) {
            squares[i] = piece;
            i++;
        } else { // if piece is undefined then char must be a number representing a series of empty squares
            i += parseInt(char);
        }
    }

    return {
        squares,
        sideToMove,
        castlingRights,
        enPassantSquare,
        halfMoveClock,
        fullMoveNumber
    };
}

export const squareToAlgebraic: (square: Square) => string = (square) => {
    const rank = 8 - (Math.floor(square / 8));
    const file = square % 8;
    const fileToLetterMap: Map<number, string> = new Map([
        [0, 'a'],
        [1, 'b'],
        [2, 'c'],
        [3, 'd'],
        [4, 'e'],
        [5, 'f'],
        [6, 'g'],
        [7, 'h'],
    ])

    return fileToLetterMap.get(file)! + rank;
}

export const algebraicToSquare: (square: string) => Square = (square) => {
    const file = square.charCodeAt(0) - 'a'.charCodeAt(0);
    const rank = parseInt(square[1]);
    return (8 - rank) * 8 + file as Square;
}

export const applyMove: (board: Board, move: Move) => Board = (board, move) => {
    const newSquares = [...board.squares] as Squares;
    const piece = newSquares[move.from];

    if (piece === null) {
        throw new Error(`No piece on square ${move.from}`);
    }

    newSquares[move.from] = null;
    newSquares[move.to] = move.promotion !== null
        ? { pieceType: move.promotion, colour: piece.colour }
        : piece;

    // En passant
    if (piece.pieceType === 'PAWN' && move.to === board.enPassantSquare) {
        const capturedPawnSquare = (piece.colour === 'WHITE'
            ? move.to + 8
            : move.to - 8) as Square;
        newSquares[capturedPawnSquare] = null;
    }

    // Castling
    if (piece.pieceType === 'KING' && Math.abs(move.to - move.from) === 2) {
        let rookFrom: Square;
        let rookTo: Square;

        switch (move.to) {
            case 6:
                rookFrom = 7;
                rookTo = 5;
                break;
            case 2:
                rookFrom = 0;
                rookTo = 3;
                break;
            case 62:
                rookFrom = 63;
                rookTo = 61;
                break;
            case 58:
                rookFrom = 56;
                rookTo = 59;
                break;
            default:
                throw new Error('Invalid Castling Move');
        }

        newSquares[rookTo] = newSquares[rookFrom];
        newSquares[rookFrom] = null;
    }

    const newEnPassantSquare = (piece.pieceType === 'PAWN' && Math.abs(move.to - move.from) === 16)
        ? Math.floor((move.from + move.to) / 2) as Square
        : null;

    const newCastlingRights = updateCastlingRights(board, move, piece);

    const isCapture = board.squares[move.to] !== null;
    const newHalfMoveClock = (isCapture || piece.pieceType === 'PAWN')
        ? 0
        : board.halfMoveClock + 1;

    const newFullMoveNumber = (board.sideToMove === 'BLACK')
        ? board.fullMoveNumber + 1
        : board.fullMoveNumber;

    return {
        squares: newSquares,
        sideToMove: getOppositeColour(board.sideToMove),
        castlingRights: newCastlingRights,
        enPassantSquare: newEnPassantSquare,
        halfMoveClock: newHalfMoveClock,
        fullMoveNumber: newFullMoveNumber
    };
}

// Helper function for getting opposite colour
function getOppositeColour(colour: Colour): Colour {
    return colour === 'WHITE' ? 'BLACK' : 'WHITE';
}

function updateCastlingRights(board: Board, move: Move, piece: Piece): CastlingRights {
    let rights = { ...board.castlingRights };

    // King moves - lose castling rights
    if (piece.pieceType === 'KING') {
        if (piece.colour === 'WHITE') {
            rights = { ...rights, whiteKingSide: false, whiteQueenSide: false };
        } else {
            rights = { ...rights, blackKingSide: false, blackQueenSide: false };
        }
    }

    // Rook moves - lose castling rights
    if (piece.pieceType === 'ROOK') {
        switch (move.from) {
            case 56:  // a1
                rights = { ...rights, whiteQueenSide: false };
                break;
            case 63:  // h1
                rights = { ...rights, whiteKingSide: false };
                break;
            case 0: // a8
                rights = { ...rights, blackQueenSide: false };
                break;
            case 7: // h8
                rights = { ...rights, blackKingSide: false };
                break;
        }
    }

    // Rook captured - lose castling rights
    switch (move.to) {
        case 56:
            rights = { ...rights, whiteQueenSide: false };
            break;
        case 63:
            rights = { ...rights, whiteKingSide: false };
            break;
        case 0:
            rights = { ...rights, blackQueenSide: false };
            break;
        case 7:
            rights = { ...rights, blackKingSide: false };
            break;
    }

    return rights;
}