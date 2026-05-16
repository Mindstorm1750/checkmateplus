# Background

On 13th May 2026 between 5 and 8 pm, the University of Kent's chess club kindly let us join one of their meetings so
that we could get feedback on Checkmate+.

# Things users asked for

## Multiple bot strengths and bot rating estimates

User wanted multiple bot strengths (we only had 1 type of Bot).

**Multiple bot options is a possible future development**.

Users also wanted to know the rating of the bot, to give them an idea of how strong an opponent they're playing against.

**Making the bot on the site rated could be a future development. Users would gain and lose rating playing against the
bot. This would give an accurate and meaningful rating to the bot rather than a guess**.

## Match review changes

Users want the ability to export the game as `.pgn` (copy to clipboard style and/or download `.pgn`).

The best move from stockfish suggestion should be in standard algebraic (the usual way chess players communicate)
instead of long algebraic (what stockfish outputs). As in `Nf3` instead of `g1f3`.

Users want the best move suggestion to be shown as an arrow on the board like Lichess and Chess.com do. Also, the
annotation (whether it was "Best", "Good", etc.) to be shown on the board.

# Things users said

- User liked that we "show legal moves correctly". They said other chess site/app attempts sometimes show illegal moves
  as possibilities after clicking on a piece. **This shows correctness of our move preview implementation, though we
  already had test files checking for that**.
- Users says the bot plays too slowly in a bullet game (1+0). **This shows the move time model needs work, especially
  for faster time controls**.
- Users would like buttons to navigate to previous and next moves in the match analysis panel. This is especially
  important for mobile.
- A user said of the fun animation feature "Woah, they're \[the pieces\] going everywhere". This wasn't said in a
  negative tone.

# Things we noticed

## Welcome guest card

A new user quickly pressed "continue as guest" without any confusion or hesitation.

**A good sign for the "welcome guest card"**.

## Registration/account creation difficulty

While one user had no trouble setting up an account, another user needed help. They needed to be told that "your
authenticator app" was the kind of thing you use for MFA'ing into your UKC email, and that they'd need to paste the code
into the app since they can't use the same device to scan the QR code that is displaying the QR code.

**This shows we need to make registration clearer, in particular what to do if you're using your phone to register**.

## Desire for free-form analysis feature

Two users who had played a game with each other over-the-board wanted to analyse their game using our site.

We didn't have a free-form analysis page like lichess.org/analysis or chess.com/analysis/explorer.

What they ended up doing was playing a game on the site and replaying the moves of the game they wanted to analyse, then
using our match review feature.

From this we noticed 3 things:

1. A "challenge specific user" feature would be helpful. The two users didn't have a way to play against just each
   other. It was only because nobody else was in the queue that they were matched against each other.
2. Free-form analysis page. Where one can just make moves from the starting position and have an engine's analysis
   showing live.
3. There should be a button on the game over dialog that takes users to the match review page. The two users had to
   click on their profile and go to their latest match's match review instead.

## Voice command button awkwardness

A user didn't know you had to tap again to stop recording. **This could be worked around by reducing the max recording
time so it shuts off earlier**. **Also, we could think about changing the text while recording. It currently just says "
...listening"**.

A user used the voice command option several times before switching to moving the pieces normally. It was in a 1+0 game
and they switched out of need to make moves quickly. **Improving processing times for the voice commands would help.
Also, perhaps in low time situations the voice command feature could start listening automatically when its the player's
turn so they don't have to press the button every time**.

# Bugs/issues

User experienced an animation glitch where multiple (but not all) white pawns briefly disappeared while a pawn was
capturing another pawn. **The game when this happened is known, so hopefully should be reproducible and able to be
understood/fixed**.

User was unable to move a piece until they selected a different piece and then the piece they originally wanted to move.
**Even though the game this happened in is known, replicating this would mean replaying the game live as the bug relates
to moving pieces during a live game**.

A draw offer on mobile causes buttons to shift off the right side of the screen (as draw offer means new buttons: "
Accept draw" and "Reject draw"). 

