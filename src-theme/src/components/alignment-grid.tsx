import { createContext, useContext, useEffect, useState } from "react";
import invariant from "tiny-invariant";

import styles from "./alignment-grid.module.css";

type AlignmentGridContextType = {
  enabled: boolean;
  horizontal: number;
  vertical: number;
};

const alignmentGridContext = createContext<AlignmentGridContextType | null>(
  null
);

export const AlignmentGridProvider = alignmentGridContext.Provider;

export function useAlignmentGrid() {
  const context = useContext(alignmentGridContext);

  invariant(
    context !== null,
    "useAlignmentGrid must be used within an AlignmentGridProvider"
  );

  return context;
}

export default function AlignmentGrid() {
  const { enabled, horizontal, vertical } = useAlignmentGrid();

  const [_, setUpdate] = useState(false);

  useEffect(() => {
    function handleResize() {
      setUpdate((update) => !update);
    }

    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  return (
    <div className={styles.alignmentGrid} data-enabled={enabled}>
      {Array.from({ length: window.innerWidth / horizontal }, (_, i) => (
        <div
          key={i}
          className={styles.line}
          data-direction="horizontal"
          style={{
            left: i * horizontal,
          }}
        />
      ))}
      {Array.from({ length: window.innerHeight / vertical }, (_, i) => (
        <div
          key={i}
          className={styles.line}
          data-direction="vertical"
          style={{
            top: i * vertical,
          }}
        />
      ))}
    </div>
  );
}
