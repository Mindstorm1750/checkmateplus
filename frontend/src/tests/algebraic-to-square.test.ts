import { algebraicToSquare } from "@/lib/chess.helpers";
import { expect, test } from "vitest";

test('algebraicToSquare on the corner squares', () => {
    expect(algebraicToSquare('a8')).toBe(0)
    expect(algebraicToSquare('h8')).toBe(7)
    expect(algebraicToSquare('a1')).toBe(56)
    expect(algebraicToSquare('h1')).toBe(63)
})

test('algebraicToSquare on other squares', () => {
    expect(algebraicToSquare('d4')).toBe(35)
    expect(algebraicToSquare('g7')).toBe(14)
    expect(algebraicToSquare('e8')).toBe(4)
    expect(algebraicToSquare('e1')).toBe(60)
})