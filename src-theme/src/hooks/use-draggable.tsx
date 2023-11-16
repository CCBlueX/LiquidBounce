import { type MutableRefObject, useEffect, useRef, useState } from "react";

import { useAlignmentGrid } from "~/components/alignment-grid";

export default function useDraggable<TElement extends HTMLElement>(
  storageKey: string,
  startPosition: [number, number] = [0, 0]
): {
  ref: MutableRefObject<TElement | null>;
  position: [number, number];
  isDragging: boolean;
} {
  const elementRef = useRef<TElement>(null);
  const { enabled: gridEnabled, horizontal, vertical } = useAlignmentGrid();

  const [isDragging, setIsDragging] = useState(false);
  const [position, setPosition] = useState<[number, number]>(() => {
    const x = localStorage.getItem(`${storageKey}.x`);
    const y = localStorage.getItem(`${storageKey}.y`);

    // If there is a saved position, use it
    if (x && y) {
      return [parseInt(x), parseInt(y)];
    }

    // If not, use the default position
    return startPosition;
  });

  useEffect(() => {
    if (elementRef.current === null) return;
    const element = elementRef.current;

    function handleMouseDown(event: MouseEvent) {
      if (event.button !== 0) return;

      setIsDragging(true);

      window.addEventListener("mousemove", handleMouseMove);
      window.addEventListener("mouseup", handleMouseUp);
    }

    function handleMouseMove(event: MouseEvent) {
      const width = elementRef.current?.offsetWidth ?? 0;
      const height = elementRef.current?.offsetHeight ?? 0;

      let x = event.clientX - width / 2;
      let y = event.clientY - height / 2;

      if (x < 0) x = 0;
      if (y < 0) y = 0;

      if (x > window.innerWidth - width) x = window.innerWidth - width;
      if (y > window.innerHeight - height) y = window.innerHeight - height;

      if (gridEnabled) {
        x = Math.round(x / horizontal) * horizontal;
        y = Math.round(y / vertical) * vertical;
      }

      setPosition([x, y]);

      localStorage.setItem(`${storageKey}.x`, x.toString());
      localStorage.setItem(`${storageKey}.y`, y.toString());
    }

    function handleMouseUp() {
      setIsDragging(false);

      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    }

    element.addEventListener("mousedown", handleMouseDown);

    return () => {
      element.removeEventListener("mousedown", handleMouseDown);
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, [storageKey, gridEnabled, horizontal, vertical]);

  return {
    ref: elementRef,
    position,
    isDragging,
  };
}
