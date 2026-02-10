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

export type BoardState = 'IN_PROGRESS' | 'CHECKMATE' | 'STALEMATE';

// Helper function to convert square to file/rank
function toFileRank(square: Square): [number, number] {
    const file = square % 8;
    const rank = Math.floor(square / 8);
    return [file, rank];
}

// Helper function to get opposite color
function oppositeColour(colour: Colour): Colour {
    return colour === 'WHITE' ? 'BLACK' : 'WHITE';
}

// Generate pawn moves
function generatePawnMoves(board: Board, from: Square, colour: Colour): Move[] {
    const moves: Move[] = [];
    const direction = colour === 'WHITE' ? -8 : 8;
    const startRank = colour === 'WHITE' ? 6 : 1;
    const forward = (from + direction) as Square;

    const [fromFile, fromRank] = toFileRank(from);

    // Move forward 1 square
    if (forward >= 0 && forward <= 63 && board.squares[forward] === null) {
        if ((colour === 'WHITE' && forward >= 0 && forward <= 7) || 
            (colour === 'BLACK' && forward >= 56 && forward <= 63)) {
            // Promotion moves
            moves.push({ from, to: forward, promotion: 'KNIGHT' });
            moves.push({ from, to: forward, promotion: 'BISHOP' });
            moves.push({ from, to: forward, promotion: 'ROOK' });
            moves.push({ from, to: forward, promotion: 'QUEEN' });
        } else {
            moves.push({ from, to: forward, promotion: null });

            // Pawn forward 2 from start
            const doubleForward = (from + 2 * direction) as Square;
            if (fromRank === startRank && board.squares[doubleForward] === null) {
                moves.push({ from, to: doubleForward, promotion: null });
            }
        }
    }

    // Captures
    const captureOffsets = colour === 'WHITE' ? [-9, -7] : [7, 9];
    for (const offset of captureOffsets) {
        const target = from + offset;
        if (target < 0 || target > 63) continue;

        const [toFile, _] = toFileRank(target as Square);
        if (Math.abs(toFile - fromFile) !== 1) continue;

        const piece = board.squares[target];

        // Regular capture
        if (piece !== null && piece.colour !== colour) {
            if ((colour === 'WHITE' && target >= 0 && target <= 7) || 
                (colour === 'BLACK' && target >= 56 && target <= 63)) {
                moves.push({ from, to: target as Square, promotion: 'KNIGHT' });
                moves.push({ from, to: target as Square, promotion: 'BISHOP' });
                moves.push({ from, to: target as Square, promotion: 'ROOK' });
                moves.push({ from, to: target as Square, promotion: 'QUEEN' });
            } else {
                moves.push({ from, to: target as Square, promotion: null });
            }
        }
        // En passant capture
        else if (target === board.enPassantSquare) {
            moves.push({ from, to: target as Square, promotion: null });
        }
    }

    return moves;
}

// Generate knight moves
function generateKnightMoves(board: Board, from: Square, colour: Colour): Move[] {
    const moves: Move[] = [];
    const knightMoves = [-17, -15, -10, -6, 6, 10, 15, 17];

    const [fromFile, fromRank] = toFileRank(from);

    for (const offset of knightMoves) {
        const target = from + offset;
        if (target >= 0 && target <= 63) {
            const [toFile, toRank] = toFileRank(target as Square);
            // Ensure knight doesn't wrap around board
            const fileDiff = Math.abs(toFile - fromFile);
            const rankDiff = Math.abs(toRank - fromRank);

            if ((fileDiff === 2 && rankDiff === 1) || (fileDiff === 1 && rankDiff === 2)) {
                const piece = board.squares[target];
                if (piece === null) {
                    moves.push({ from, to: target as Square, promotion: null });
                } else if (piece.colour !== colour) {
                    moves.push({ from, to: target as Square, promotion: null });
                }
            }
        }
    }

    return moves;
}

// Generate bishop moves
function generateBishopMoves(board: Board, from: Square, colour: Colour): Move[] {
    const moves: Move[] = [];
    const directions = [-9, -7, 7, 9];
    const [fromFile, fromRank] = toFileRank(from);

    for (const direction of directions) {
        let target = from + direction;
        let currentFile = fromFile;
        let currentRank = fromRank;

        while (target >= 0 && target <= 63) {
            const [toFile, toRank] = toFileRank(target as Square);
            const fileDiff = Math.abs(toFile - currentFile);
            const rankDiff = Math.abs(toRank - currentRank);
            if (fileDiff !== 1 || rankDiff !== 1) {
                break;
            }
            const piece = board.squares[target];
            if (piece === null) {
                moves.push({ from, to: target as Square, promotion: null });
            } else {
                if (piece.colour !== colour) {
                    moves.push({ from, to: target as Square, promotion: null });
                }
                break;
            }

            currentFile = toFile;
            currentRank = toRank;
            target += direction;
        }
    }
    return moves;
}

// Generate rook moves
function generateRookMoves(board: Board, from: Square, colour: Colour): Move[] {
    const moves: Move[] = [];
    const directions = [-8, -1, 1, 8];

    for (const direction of directions) {
        let target = from + direction;
        const isHorizontal = (direction === 1 || direction === -1);

        while (target >= 0 && target <= 63) {
            // Stop wrapping around
            if (isHorizontal && Math.floor(target / 8) !== Math.floor(from / 8)) break;

            const piece = board.squares[target];
            if (piece === null) {
                moves.push({ from, to: target as Square, promotion: null });
            } else {
                if (piece.colour !== colour) {
                    moves.push({ from, to: target as Square, promotion: null });
                }
                break;
            }
            target += direction;
        }
    }
    return moves;
}

// Generate queen moves
function generateQueenMoves(board: Board, from: Square, colour: Colour): Move[] {
    const moves: Move[] = [];
    moves.push(...generateBishopMoves(board, from, colour));
    moves.push(...generateRookMoves(board, from, colour));
    return moves;
}

// Generate king moves
function generateKingMoves(board: Board, from: Square, colour: Colour, includeCastling: boolean = true): Move[] {
    const moves: Move[] = [];
    const kingMoves = [-9, -8, -7, -1, 1, 7, 8, 9];
    const [fromFile, fromRank] = toFileRank(from);

    for (const offset of kingMoves) {
        const target = from + offset;
        if (target >= 0 && target <= 63) {
            const [toFile, toRank] = toFileRank(target as Square);

            // Check king didn't wrap around board
            const fileDiff = Math.abs(toFile - fromFile);
            const rankDiff = Math.abs(toRank - fromRank);

            if (fileDiff <= 1 && rankDiff <= 1) {
                const piece = board.squares[target];
                if (piece === null || piece.colour !== colour) {
                    moves.push({ from, to: target as Square, promotion: null });
                }
            }
        }
    }

    // Skip castling if not needed
    if (!includeCastling) return moves;

    // Castling - White
    if (colour === 'WHITE' && from === 60) {
        // Kingside
        if (board.castlingRights.whiteKingSide && 
            board.squares[61] === null && 
            board.squares[62] === null &&
            !isSquareAttacked(board, 60, 'BLACK') &&
            !isSquareAttacked(board, 61, 'BLACK') &&
            !isSquareAttacked(board, 62, 'BLACK')) {
            moves.push({ from: 60, to: 62, promotion: null });
        }
        // Queenside
        if (board.castlingRights.whiteQueenSide && 
            board.squares[57] === null && 
            board.squares[58] === null && 
            board.squares[59] === null &&
            !isSquareAttacked(board, 60, 'BLACK') &&
            !isSquareAttacked(board, 59, 'BLACK') &&
            !isSquareAttacked(board, 58, 'BLACK')) {
            moves.push({ from: 60, to: 58, promotion: null });
        }
    }

    // Castling - Black
    if (colour === 'BLACK' && from === 4) {
        // Kingside
        if (board.castlingRights.blackKingSide && 
            board.squares[5] === null && 
            board.squares[6] === null &&
            !isSquareAttacked(board, 4, 'WHITE') &&
            !isSquareAttacked(board, 5, 'WHITE') &&
            !isSquareAttacked(board, 6, 'WHITE')) {
            moves.push({ from: 4, to: 6, promotion: null });
        }
        // Queenside
        if (board.castlingRights.blackQueenSide && 
            board.squares[1] === null && 
            board.squares[2] === null && 
            board.squares[3] === null &&
            !isSquareAttacked(board, 4, 'WHITE') &&
            !isSquareAttacked(board, 3, 'WHITE') &&
            !isSquareAttacked(board, 2, 'WHITE')) {
            moves.push({ from: 4, to: 2, promotion: null });
        }
    }

    return moves;
}

// Generate all pseudo-legal moves
function generateAllMoves(board: Board): Move[] {
    const moves: Move[] = [];
    for (let square = 0; square <= 63; square++) {
        const piece = board.squares[square];
        if (piece === null) continue;
        if (piece.colour !== board.sideToMove) continue;

        switch (piece.pieceType) {
            case 'PAWN':
                moves.push(...generatePawnMoves(board, square as Square, piece.colour));
                break;
            case 'KNIGHT':
                moves.push(...generateKnightMoves(board, square as Square, piece.colour));
                break;
            case 'BISHOP':
                moves.push(...generateBishopMoves(board, square as Square, piece.colour));
                break;
            case 'ROOK':
                moves.push(...generateRookMoves(board, square as Square, piece.colour));
                break;
            case 'QUEEN':
                moves.push(...generateQueenMoves(board, square as Square, piece.colour));
                break;
            case 'KING':
                moves.push(...generateKingMoves(board, square as Square, piece.colour));
                break;
        }
    }
    return moves;
}

// Check if a square is attacked by a given color
function isSquareAttacked(board: Board, square: Square, byColour: Colour): boolean {
    for (let sqr = 0; sqr <= 63; sqr++) {
        const piece = board.squares[sqr];
        if (piece === null) continue;
        if (piece.colour !== byColour) continue;

        switch (piece.pieceType) {
            case 'PAWN': {
                // Pawns only attack diagonally
                const direction = piece.colour === 'WHITE' ? -8 : 8;
                const [pawnFile, _] = toFileRank(sqr as Square);
                const [targetFile, __] = toFileRank(square);
                const captureSquares = [sqr + direction - 1, sqr + direction + 1];
                if (captureSquares.includes(square) && Math.abs(targetFile - pawnFile) === 1) {
                    return true;
                }
                break;
            }
            case 'KNIGHT':
                if (generateKnightMoves(board, sqr as Square, piece.colour).some(m => m.to === square)) {
                    return true;
                }
                break;
            case 'BISHOP':
                if (generateBishopMoves(board, sqr as Square, piece.colour).some(m => m.to === square)) {
                    return true;
                }
                break;
            case 'ROOK':
                if (generateRookMoves(board, sqr as Square, piece.colour).some(m => m.to === square)) {
                    return true;
                }
                break;
            case 'QUEEN':
                if (generateQueenMoves(board, sqr as Square, piece.colour).some(m => m.to === square)) {
                    return true;
                }
                break;
            case 'KING':
                if (generateKingMoves(board, sqr as Square, piece.colour, false).some(m => m.to === square)) {
                    return true;
                }
                break;
        }
    }
    return false;
}

// Check if a given color's king is in check
function isInCheck(board: Board, colour: Colour): boolean {
    const kingSquare = board.squares.findIndex(
        piece => piece?.pieceType === 'KING' && piece.colour === colour
    );
    if (kingSquare === -1) return false;
    return isSquareAttacked(board, kingSquare as Square, oppositeColour(colour));
}

// Generate all legal moves (filters out moves that leave king in check)
export function legalMoves(board: Board): Move[] {
    const allMoves = generateAllMoves(board);
    return allMoves.filter(move => {
        const newBoard = applyMove(board, move);
        return !isInCheck(newBoard, board.sideToMove);
    });
}

// Get the current board state
export function getBoardState(board: Board): BoardState {
    const moves = legalMoves(board);

    if (moves.length > 0) {
        return 'IN_PROGRESS';
    }

    return isInCheck(board, board.sideToMove) ? 'CHECKMATE' : 'STALEMATE';
}