import { useState } from "react";
import { useOutlet } from "react-router-dom";
import invariant from "tiny-invariant";

export default function AnimatedOutlet() {
  const o = useOutlet();
  const [outlet] = useState(o);

  invariant(outlet, "AnimatedOutlet needs to be rendered within a RouterProvider");

  return outlet;
}
