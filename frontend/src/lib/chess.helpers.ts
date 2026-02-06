import type { Board, CastlingRights, Colour, Piece, Square, Squares } from "@/types/Chess.type";
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

/**
 * 
 * @param file number between 0 to 7 inclusive
 * @param rank number between 0 to 7 inclusive
 * @returns Square (number between 0 to 63 inclusive)
 */
export const fileRankToSquare: (file: number, rank: number) => Square = (file, rank) => {
    return (rank * 8 + file) as Square
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
        const epSquareFile = fenParts[3].charCodeAt(0) - 97; // 'a' is 0, 'b' is 1, 'c' is 2, ... 'h' is 7
        const epSquareRank = parseInt(fenParts[3][1]) - 1; // -1 because of 0-indexing
        enPassantSquare = fileRankToSquare(epSquareFile, epSquareRank);
    }

    // fen starts from the 8th rank, we want to iterate starting from the 1st rank, hence reversed()
    const boardFEN = fenParts[0].trim().split('/').reverse().join('');

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