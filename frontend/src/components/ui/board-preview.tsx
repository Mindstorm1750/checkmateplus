import { BOARD_THEMES } from "@/lib/board.theme.ts"

/**
 * 2x2 board preview for showing colour options
 */
export function BoardPreview({ theme }: { theme: keyof typeof BOARD_THEMES }) {
  const { lightSquare, darkSquare } = BOARD_THEMES[theme]
  return (
    <span className="grid size-6 shrink-0 grid-cols-2 grid-rows-2">
      <span className={lightSquare} />
      <span className={darkSquare} />
      <span className={darkSquare} />
      <span className={lightSquare} />
    </span>
  )
}
