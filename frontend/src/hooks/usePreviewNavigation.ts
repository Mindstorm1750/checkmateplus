// hooks/usePreviewNavigation.ts
import { useRef, useState } from "react"
import { algebraicToSquare, fenToBoard, isInCheck } from "@/lib/chess.helpers"
import { applyForwardStep, applyBackwardStep } from "@/lib/pieceOrigins.helpers"
import {
  EMPTY_PIECE_ORIGINS,
  type Square,
  type Move,
  type Board,
} from "@/types/Chess.type"
import type { MoveHistoryEntry } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage"

export function usePreviewNavigation(
  moveHistory: MoveHistoryEntry[],
  livePieceOrigins: Map<Square, Square>,
  liveBoard?: Board | null
) {
  const [previewIndex, setPreviewIndex] = useState<number | null>(null)
  const [previewPieceOrigins, setPreviewPieceOrigins] =
    useState<Map<Square, Square>>(EMPTY_PIECE_ORIGINS)
  const [lastMove, setLastMove] = useState<Move>()
  const priorDisplayedIndexRef = useRef<number | null>(null)

  const handleSelect = (index: number | null) => {
    const move = moveHistory[index ?? moveHistory.length - 1]
    // !index means the final/live move is being previewed/shown. See also: the 2 "?? liveIndex" below
    if (move) {
      setLastMove({
        from: algebraicToSquare(move.from),
        to: algebraicToSquare(move.to),
      })
    } else {
      setLastMove(undefined)
    }

    const liveIndex = moveHistory.length - 1
    const priorRaw = priorDisplayedIndexRef.current
    const priorIndex = priorRaw ?? liveIndex
    const nextIndex = index ?? liveIndex

    priorDisplayedIndexRef.current = index

    if (nextIndex !== priorIndex) {
      const baseMap = priorRaw === null ? livePieceOrigins : previewPieceOrigins
      // priorRaw being null means we weren't previewing
      if (nextIndex === priorIndex + 1 && moveHistory[nextIndex]) {
        const move = moveHistory[nextIndex]
        setPreviewPieceOrigins(
          applyForwardStep(baseMap, {
            from: algebraicToSquare(move.from),
            to: algebraicToSquare(move.to),
          })
        )
      } else if (nextIndex === priorIndex - 1 && moveHistory[priorIndex]) {
        const move = moveHistory[priorIndex]
        setPreviewPieceOrigins(
          applyBackwardStep(baseMap, {
            from: algebraicToSquare(move.from),
            to: algebraicToSquare(move.to),
          })
        )
      } else {
        setPreviewPieceOrigins(EMPTY_PIECE_ORIGINS)
      }
    }

    setPreviewIndex(index)
  }

  const isPreviewing = previewIndex !== null
  const displayedPieceOrigins = isPreviewing
    ? previewPieceOrigins
    : livePieceOrigins

  const isCheck =
    isPreviewing && moveHistory[previewIndex]
      ? (() => {
          const previewBoard = fenToBoard(moveHistory[previewIndex].fenAfter)
          return isInCheck(previewBoard, previewBoard.sideToMove)
        })()
      : liveBoard
        ? isInCheck(liveBoard, liveBoard.sideToMove)
        : false

  return {
    previewIndex,
    displayedPieceOrigins,
    lastMove,
    setLastMove,
    handleSelect,
    isCheck,
  }
}
