import { test, expect } from 'vitest';
import type { Move, Board } from '@/types/Chess.type';
import { algebraicToSquare, applyMove, fenToBoard, getBoardState, legalMoves } from '@/lib/chess.helpers';

/**
 * Extensively testing that legalMoves() works by recursively calling legalMoves()
 * until a fixed depth and comparing the total number of positions reached
 * with a known correct number.
 * The position used is not the starting position, but instead a famous position
 * for engine testing called "kiwipete".
 * This recursive function is called "perft".
 */
test('legal moves perft', () => {
    const board = fenToBoard("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
    const expectedResultForDepthFive = 193690690;

    function perft(board: Board, depth: number): number {
        const moves = legalMoves(board);
        if (depth === 1) {
            return moves.length;
        }
        let count = 0;
        for (const move of moves) {
            count += perft(applyMove(board, move), depth - 1);
        }
        return count;
    }

    expect(perft(board, 5)).toBe(expectedResultForDepthFive);
}, 999999);

/**
 * Test that legalMoves() works on a near-middlegame position
 */
test('legal moves middle game', () => {
    const board = fenToBoard("r3k2r/2qbbppp/p1nppn2/1p3P2/3NP3/2N1B2P/PPPQB1P1/R3K2R w KQkq - 0 12");
    const expectedLegalMoves: Move[] = [
        { from: algebraicToSquare('a2'), to: algebraicToSquare('a3'), promotion: null },
        { from: algebraicToSquare('b2'), to: algebraicToSquare('b3'), promotion: null },
        { from: algebraicToSquare('g2'), to: algebraicToSquare('g3'), promotion: null },
        { from: algebraicToSquare('h3'), to: algebraicToSquare('h4'), promotion: null },
        { from: algebraicToSquare('e4'), to: algebraicToSquare('e5'), promotion: null },
        { from: algebraicToSquare('a2'), to: algebraicToSquare('a4'), promotion: null },
        { from: algebraicToSquare('b2'), to: algebraicToSquare('b4'), promotion: null },
        { from: algebraicToSquare('g2'), to: algebraicToSquare('g4'), promotion: null },
        { from: algebraicToSquare('f5'), to: algebraicToSquare('e6'), promotion: null },
        { from: algebraicToSquare('c3'), to: algebraicToSquare('b1'), promotion: null },
        { from: algebraicToSquare('c3'), to: algebraicToSquare('d1'), promotion: null },
        { from: algebraicToSquare('c3'), to: algebraicToSquare('a4'), promotion: null },
        { from: algebraicToSquare('c3'), to: algebraicToSquare('b5'), promotion: null },
        { from: algebraicToSquare('c3'), to: algebraicToSquare('d5'), promotion: null },
        { from: algebraicToSquare('d4'), to: algebraicToSquare('b3'), promotion: null },
        { from: algebraicToSquare('d4'), to: algebraicToSquare('f3'), promotion: null },
        { from: algebraicToSquare('d4'), to: algebraicToSquare('b5'), promotion: null },
        { from: algebraicToSquare('d4'), to: algebraicToSquare('c6'), promotion: null },
        { from: algebraicToSquare('d4'), to: algebraicToSquare('e6'), promotion: null },
        { from: algebraicToSquare('e2'), to: algebraicToSquare('d1'), promotion: null },
        { from: algebraicToSquare('e2'), to: algebraicToSquare('f1'), promotion: null },
        { from: algebraicToSquare('e2'), to: algebraicToSquare('d3'), promotion: null },
        { from: algebraicToSquare('e2'), to: algebraicToSquare('f3'), promotion: null },
        { from: algebraicToSquare('e2'), to: algebraicToSquare('c4'), promotion: null },
        { from: algebraicToSquare('e2'), to: algebraicToSquare('g4'), promotion: null },
        { from: algebraicToSquare('e2'), to: algebraicToSquare('b5'), promotion: null },
        { from: algebraicToSquare('e2'), to: algebraicToSquare('h5'), promotion: null },
        { from: algebraicToSquare('e3'), to: algebraicToSquare('g1'), promotion: null },
        { from: algebraicToSquare('e3'), to: algebraicToSquare('f2'), promotion: null },
        { from: algebraicToSquare('e3'), to: algebraicToSquare('f4'), promotion: null },
        { from: algebraicToSquare('e3'), to: algebraicToSquare('g5'), promotion: null },
        { from: algebraicToSquare('e3'), to: algebraicToSquare('h6'), promotion: null },
        { from: algebraicToSquare('a1'), to: algebraicToSquare('b1'), promotion: null },
        { from: algebraicToSquare('a1'), to: algebraicToSquare('c1'), promotion: null },
        { from: algebraicToSquare('a1'), to: algebraicToSquare('d1'), promotion: null },
        { from: algebraicToSquare('h1'), to: algebraicToSquare('f1'), promotion: null },
        { from: algebraicToSquare('h1'), to: algebraicToSquare('g1'), promotion: null },
        { from: algebraicToSquare('h1'), to: algebraicToSquare('h2'), promotion: null },
        { from: algebraicToSquare('d2'), to: algebraicToSquare('c1'), promotion: null },
        { from: algebraicToSquare('d2'), to: algebraicToSquare('d1'), promotion: null },
        { from: algebraicToSquare('d2'), to: algebraicToSquare('d3'), promotion: null },
        { from: algebraicToSquare('e1'), to: algebraicToSquare('d1'), promotion: null },
        { from: algebraicToSquare('e1'), to: algebraicToSquare('f1'), promotion: null },
        { from: algebraicToSquare('e1'), to: algebraicToSquare('f2'), promotion: null },
        { from: algebraicToSquare('e1'), to: algebraicToSquare('g1'), promotion: null },
        { from: algebraicToSquare('e1'), to: algebraicToSquare('c1'), promotion: null }
    ];

    const actualMoves = legalMoves(board);
    
    // Compare as sets to ignore order
    expect(actualMoves).toEqual(expect.arrayContaining(expectedLegalMoves));
    expect(actualMoves.length).toBe(expectedLegalMoves.length);
});

/**
 * Test that legalMoves() works with pawn promotion moves as white
 */
test('legal moves white promotion', () => {
    const board = fenToBoard("8/1P6/7k/8/7K/8/8/8 w - - 0 1");
    const expectedLegalMoves: Move[] = [
        { from: algebraicToSquare('b7'), to: algebraicToSquare('b8'), promotion: 'QUEEN' },
        { from: algebraicToSquare('b7'), to: algebraicToSquare('b8'), promotion: 'ROOK' },
        { from: algebraicToSquare('b7'), to: algebraicToSquare('b8'), promotion: 'BISHOP' },
        { from: algebraicToSquare('b7'), to: algebraicToSquare('b8'), promotion: 'KNIGHT' },
        { from: algebraicToSquare('h4'), to: algebraicToSquare('g3'), promotion: null },
        { from: algebraicToSquare('h4'), to: algebraicToSquare('h3'), promotion: null },
        { from: algebraicToSquare('h4'), to: algebraicToSquare('g4'), promotion: null }
    ];

    const actualMoves = legalMoves(board);
    
    expect(actualMoves).toEqual(expect.arrayContaining(expectedLegalMoves));
    expect(actualMoves.length).toBe(expectedLegalMoves.length);
});

/**
 * Test that legalMoves() works with pawn promotion moves as black
 */
test('legal moves black promotion', () => {
    const board = fenToBoard("8/8/7k/8/7K/8/1p6/8 b - - 0 1");
    const expectedLegalMoves: Move[] = [
        { from: algebraicToSquare('b2'), to: algebraicToSquare('b1'), promotion: 'QUEEN' },
        { from: algebraicToSquare('b2'), to: algebraicToSquare('b1'), promotion: 'ROOK' },
        { from: algebraicToSquare('b2'), to: algebraicToSquare('b1'), promotion: 'BISHOP' },
        { from: algebraicToSquare('b2'), to: algebraicToSquare('b1'), promotion: 'KNIGHT' },
        { from: algebraicToSquare('h6'), to: algebraicToSquare('h7'), promotion: null },
        { from: algebraicToSquare('h6'), to: algebraicToSquare('g7'), promotion: null },
        { from: algebraicToSquare('h6'), to: algebraicToSquare('g6'), promotion: null }
    ];

    const actualMoves = legalMoves(board);
    
    expect(actualMoves).toEqual(expect.arrayContaining(expectedLegalMoves));
    expect(actualMoves.length).toBe(expectedLegalMoves.length);
});

test('get board state', () => {
    const boardInProgress = fenToBoard("rn1q1rk1/ppp1bppp/3pp3/4Pb2/3P4/2P2NP1/P1P1BP1P/R1BQ1RK1 b - - 0 9");
    const boardStalemate = fenToBoard("2k5/2P5/2K5/8/8/8/8/8 b - - 0 1");
    const boardCheckmate = fenToBoard("kr6/pp6/1N6/8/8/8/8/R1K5 b - - 0 1");

    expect(getBoardState(boardInProgress)).toBe('IN_PROGRESS');
    expect(getBoardState(boardStalemate)).toBe('STALEMATE');
    expect(getBoardState(boardCheckmate)).toBe('CHECKMATE');
});