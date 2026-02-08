import { algebraicToSquare, applyMove, fenToBoard } from '@/lib/chess.helpers';
import { expect, test } from 'vitest'

test('white kingside castle', () => {
    const board = fenToBoard("r1bq1rk1/pp1nbppp/4pn2/2pp2B1/2PP4/2NBPN2/PP3PPP/R2QK2R w KQ - 0 8");
    const expectedBoardAfterCastle = fenToBoard("r1bq1rk1/pp1nbppp/4pn2/2pp2B1/2PP4/2NBPN2/PP3PPP/R2Q1RK1 b - - 1 8");
    const boardAfterCastle = applyMove(board, { from: 60, to: 62, promotion: null });
    expect(boardAfterCastle).toEqual(expectedBoardAfterCastle);
});

test('white queenside castle', () => {
    const board = fenToBoard("r2qk2r/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/R3KB1R w KQkq - 6 10");
    const expectedBoardAfterCastle = fenToBoard("r2qk2r/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/2KR1B1R b kq - 7 10");
    const boardAfterCastle = applyMove(board, { from: 60, to: 58, promotion: null });
    expect(boardAfterCastle).toEqual(expectedBoardAfterCastle);
});

test('black kingside castle', () => {
    const board = fenToBoard("r2qk2r/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/2KR1B1R b kq - 7 10");
    const expectedBoardAfterCastle = fenToBoard("r2q1rk1/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/2KR1B1R w - - 8 11");
    const boardAfterCastle = applyMove(board, { from: 4, to: 6, promotion: null });
    expect(boardAfterCastle).toEqual(expectedBoardAfterCastle);
});

test('black queenside castle', () => {
    const board = fenToBoard("r3kbnr/1ppb1ppp/p1p5/8/3NP3/8/PPP2PPP/RNB2RK1 b kq - 2 8");
    const expectedBoardAfterCastle = fenToBoard("2kr1bnr/1ppb1ppp/p1p5/8/3NP3/8/PPP2PPP/RNB2RK1 w - - 3 9");
    const boardAfterCastle = applyMove(board, { from: 4, to: 2, promotion: null });
    expect(boardAfterCastle).toEqual(expectedBoardAfterCastle);
});

test('apply move capture', () => {
    const board = fenToBoard("rnbq1rk1/ppp1bppp/4p3/3p2B1/2PPn3/2N1PN2/PP3PPP/R2QKB1R w KQ - 1 7");
    const expectedBoardAfterMove = fenToBoard("rnbq1rk1/ppp1bppp/4p3/3p2B1/2PPN3/4PN2/PP3PPP/R2QKB1R b KQ - 0 7");
    const boardAfterMove = applyMove(board, { from: algebraicToSquare('c3'), to: algebraicToSquare('e4'), promotion: null });
    expect(boardAfterMove).toEqual(expectedBoardAfterMove);
});

test('apply move non-capture', () => {
    const board = fenToBoard("rnbq1rk1/ppp1bppp/4p3/6B1/2PPp3/4PN2/PP3PPP/R2QKB1R w KQ - 0 8");
    const expectedBoardAfterMove = fenToBoard("rnbq1rk1/ppp1bppp/4p3/4N1B1/2PPp3/4P3/PP3PPP/R2QKB1R b KQ - 1 8");
    const boardAfterMove = applyMove(board, { from: algebraicToSquare('f3'), to: algebraicToSquare('e5'), promotion: null });
    expect(boardAfterMove).toEqual(expectedBoardAfterMove);
});

test('apply move pawn move', () => {
    const board = fenToBoard("rnbq1rk1/ppp1bppp/4p3/4N1B1/2PPp3/4P3/PP3PPP/R2QKB1R b KQ - 1 8");
    const expectedBoardAfterMove = fenToBoard("rnbq1rk1/ppp1b1pp/4pp2/4N1B1/2PPp3/4P3/PP3PPP/R2QKB1R w KQ - 0 9");
    const boardAfterMove = applyMove(board, { from: algebraicToSquare('f7'), to: algebraicToSquare('f6'), promotion: null });
    expect(boardAfterMove).toEqual(expectedBoardAfterMove);
});

test('apply move pawn two squares move', () => {
    const board = fenToBoard("rnbq1rk1/ppp1bppp/4p3/4N1B1/2PPp3/4P3/PP3PPP/R2QKB1R b KQ - 1 8");
    const expectedBoardAfterMove = fenToBoard("rnbq1rk1/ppp1b1pp/4p3/4NpB1/2PPp3/4P3/PP3PPP/R2QKB1R w KQ f6 0 9");
    const boardAfterMove = applyMove(board, { from: algebraicToSquare('f7'), to: algebraicToSquare('f5'), promotion: null });
    expect(boardAfterMove).toEqual(expectedBoardAfterMove);
});

test('apply move en passant', () => {
    const board = fenToBoard("rnbqkbnr/pp2pppp/8/2ppP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3");
    const expectedBoardAfterMove = fenToBoard("rnbqkbnr/pp2pppp/3P4/2p5/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 3");
    const boardAfterMove = applyMove(board, { from: algebraicToSquare('e5'), to: algebraicToSquare('d6'), promotion: null });
    expect(boardAfterMove).toEqual(expectedBoardAfterMove);
});

test('applies move promotion', () => {
    const board = fenToBoard("8/k2PN3/1nK5/8/8/8/8/8 w - - 0 1");
    const expectedBoard = fenToBoard("3Q4/k3N3/1nK5/8/8/8/8/8 b - - 0 1");
    const boardAfterPromotion = applyMove(board, { from: algebraicToSquare('d7'), to: algebraicToSquare('d8'), promotion: 'QUEEN' });
    expect(boardAfterPromotion).toEqual(expectedBoard)
})

test('applies move underpromotion', () => {
    const board = fenToBoard("8/8/8/1p6/1P6/4Q3/2kp1K2/8 b - - 0 1")
    const expectedBoard = fenToBoard("8/8/8/1p6/1P6/4Q3/2k2K2/3n4 w - - 0 2")
    const boardAfterUnderpromotion = applyMove(board, { from: algebraicToSquare('d2'), to: algebraicToSquare('d1'), promotion: 'KNIGHT' })
    expect(boardAfterUnderpromotion).toEqual(expectedBoard)
})