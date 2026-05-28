import { squareToAlgebraic } from "@/lib/chess.helpers"
import { expect, test } from "vitest"

test("squareToAlgebraic on the corner squares", () => {
  expect(squareToAlgebraic(0)).toBe("a8")
  expect(squareToAlgebraic(7)).toBe("h8")
  expect(squareToAlgebraic(56)).toBe("a1")
  expect(squareToAlgebraic(63)).toBe("h1")
})

test("squareToAlgebraic on other squares", () => {
  expect(squareToAlgebraic(35)).toBe("d4")
  expect(squareToAlgebraic(14)).toBe("g7")
  expect(squareToAlgebraic(4)).toBe("e8") // black king on start position
  expect(squareToAlgebraic(60)).toBe("e1") // white king on start position
})
