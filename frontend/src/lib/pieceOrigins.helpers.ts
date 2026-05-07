import type { Square } from "@/types/Chess.type"

type Step = { from: Square; to: Square }

const setCastlingRook = (
  next: Map<Square, Square>,
  rookTo: Square,
  rookFrom: Square
) => {
  const rookOrigin = next.get(rookFrom) ?? rookFrom
  next.set(rookTo, rookOrigin)
  next.delete(rookFrom)
}

export const applyForwardStep = (
  prev: Map<Square, Square>,
  move: Step
): Map<Square, Square> => {
  const originOfFrom = prev.get(move.from)
  if (originOfFrom === undefined || originOfFrom === null) {
    return prev
  }
  const next = new Map(prev)
  const origin = originOfFrom
  if (origin === 60 && move.to === 62) setCastlingRook(next, 61, 63)
  if (origin === 60 && move.to === 58) setCastlingRook(next, 59, 56)
  if (origin === 4 && move.to === 6) setCastlingRook(next, 5, 7)
  if (origin === 4 && move.to === 2) setCastlingRook(next, 3, 0)
  next.set(move.to, origin)
  next.delete(move.from)
  return next
}

export const applyBackwardStep = (
  prev: Map<Square, Square>,
  move: Step
): Map<Square, Square> => {
  const originOfTo = prev.get(move.to)
  if (originOfTo === undefined || originOfTo === null) {
    return prev
  }
  const next = new Map(prev)
  const origin = originOfTo
  if (origin === 60 && move.to === 62) setCastlingRook(next, 63, 61)
  if (origin === 60 && move.to === 58) setCastlingRook(next, 56, 59)
  if (origin === 4 && move.to === 6) setCastlingRook(next, 7, 5)
  if (origin === 4 && move.to === 2) setCastlingRook(next, 0, 3)
  next.set(move.from, origin)
  next.delete(move.to)
  return next
}
