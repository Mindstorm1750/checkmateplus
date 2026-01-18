import type { ReactNode } from "react";

/** 
 * TODO: Make "Checkmate+" a link to the main page. 
 * If user clicks this link while in a game, we need to think about what behaviour should be.
 * Should we make a dialog asking if they are sure they want to quit their game and go to the main page?
 * */ 
export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold">
            Checkmate+
        </h1>
        <hr className="mt-4 mb-8"/>
        {children}
    </div>
  );
}