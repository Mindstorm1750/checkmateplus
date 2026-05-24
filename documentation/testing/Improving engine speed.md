# NOTE

**All this is non-native JVM testing**

# Move-ordering

Before: 13 min 44 sec to run `EngineTests`. 13 min 37 sec on another to see how variable it is.

After some move ordering: 12 min 40 sec. This doesn't seem so drastic because the perft test isn't effected by the
changes and takes up most of the duration anyways; I should compare before and after with the perft test disabled.

**Comparing now without perft:**

With move-ordering: 8 min 48 sec
Without move-ordering: 9 min 40 sec
With move-ordering fixed: 2 min 43 sec and another run at 2 min 33 sec just to confirm it wasn't some fluke.

So like 3.67 times faster.

> I was doing
> `pieceValue of attacked piece - pieceValue of attacker`
> but I changed it to
> `2 * pieceValue of attacked piece - pieceValue of attacker`
>
> The difference is that with the first version, moves where a piece captures another piece of the same type (e.g. pawn
> takes pawn, knight takes knight, etc.) aren't prioritised over non-capture moves (even though they're often worth
> considering first).

# Transposition table

4 min 3 seconds without
3 min 3 seconds with

# Iterative deepening and history heuristic (tt move-ordering)

3 min 3 seconds without
2 min 15 seconds with

# Profiling

Using intelliJ profiler with perft commented out.

`isInCheck()` is a rather expensive function. It gets called every time we try to generate legal moves (
`generateLegalMoves()`) (because moves that put us in check aren't a legal move) and it also gets called when we check
if the position is in checkmate or stalemate or neither (`getBoardState()`).

## Before

The total sum memory allocation throughout the tests for `findBestMoves()` was `346.63 GB`.

Running gameserver
`70 to 90 MB` RAM usage baseline.
`4%` CPU baseline.
`174 MB` peak while playing
`16%` CPU peak while playing

## After (transposition, tt move ordering, iterative deepening)

The total sum memory allocation throughout the tests for `findBestMoves()` was `137.15 GB`.

The total sum memory allocation throughout the tests for the transposition table was `14 MB`.

Running gameserver
`70 to 90 MB` RAM usage baseline
`4%` CPU baseline.
`231 MB` peak while playing (so a `+57 MB` increase)
`30%` peak while playing

## Conclusion

While memory and cpu usage increases, the bot spends less time, reducing resource usage overall (as seen by reduced
total sum memory allocation).