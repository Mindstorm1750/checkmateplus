export const BOARD_THEMES = {
  BROWN: {
    lightSquare: "bg-amber-300",
    darkSquare: "bg-amber-700",
    lightSquareHighlighted: "bg-orange-500",
    darkSquareHighlighted: "bg-orange-800",
    lightSquareDot: "bg-orange-600", // might seem redundant to have light and dark square dots specified...
    darkSquareDot: "bg-orange-600", // ...since they're the same value now, but we may want to go back to them being different
  },
  BLUE: {
    lightSquare: "bg-sky-300",
    darkSquare: "bg-sky-700",
    lightSquareHighlighted: "bg-blue-500",
    darkSquareHighlighted: "bg-blue-800",
    lightSquareDot: "bg-sky-600",
    darkSquareDot: "bg-sky-600",
  },
  GREEN: {
    lightSquare: "bg-lime-300",
    darkSquare: "bg-lime-700",
    lightSquareHighlighted: "bg-green-500",
    darkSquareHighlighted: "bg-green-800",
    lightSquareDot: "bg-lime-600",
    darkSquareDot: "bg-lime-600",
  },
}
