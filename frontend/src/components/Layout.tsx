import type { ReactNode } from "react";
import { Link } from "react-router";

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold">
            <Link to='/'>Checkmate+</Link>
        </h1>
        <hr className="mt-4 mb-8"/>
        {children}
    </div>
  );
}