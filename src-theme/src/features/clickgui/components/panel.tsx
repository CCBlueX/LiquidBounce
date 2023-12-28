import { AnimatePresence, motion } from "framer-motion";
import {
  type CSSProperties,
  useState,
  createContext,
  useContext,
  useEffect,
} from "react";

import Resizable from "~/components/resizable";

import ModuleItem from "./module";

import useDraggable from "~/hooks/use-draggable.tsx";

import styles from "./panel.module.scss";
import invariant from "tiny-invariant";
import { Module } from "~/utils/api";

type PanelProps = {
  category: string;
  modules: Module[];
  startPosition: [number, number];
};

type PanelContextType = {
  width: number;
  height: number;
};

const PanelContext = createContext<PanelContextType | null>(null);

export function usePanelContext() {
  const context = useContext(PanelContext);

  invariant(
    context,
    "usePanelContext must be used within a PanelContextProvider"
  );

  return context;
}

export default function Panel({
  category,
  modules,
  startPosition,
}: PanelProps) {
  const {
    ref: headerRef,
    position,
    isDragging,
  } = useDraggable<HTMLElement>(`clickgui.panel.${category}`, startPosition);

  const [{ width, height }, setDimensions] = useState(() => {
    const dimensions = localStorage.getItem(
      `clickgui.panel.${category}.dimensions`
    );

    if (dimensions) {
      const [width, height] = dimensions.split(",").map(Number);
      return { width, height };
    }

    return {
      width: 230,
      height: Math.max(100, Math.min(800, modules.length * 40 + 30)),
    };
  });

  const [expanded, setExpanded] = useState(() => {
    const expanded = localStorage.getItem(
      `clickgui.panel.${category}.expanded`
    );
    return expanded === "true";
  });

  const [showResizeHandles, setShowResizeHandles] = useState(false);

  function toggleExpanded() {
    setExpanded(!expanded);
    localStorage.setItem(`clickgui.panel.${category}.expanded`, `${!expanded}`);
  }

  function handleContextMenu(event: React.MouseEvent<HTMLDivElement>) {
    event.preventDefault();
    toggleExpanded();
  }

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Shift") {
        setShowResizeHandles(true);
      }
    }

    function handleKeyUp(event: KeyboardEvent) {
      if (event.key === "Shift") {
        setShowResizeHandles(false);
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
    };
  }, []);

  const style = {
    "--x": `${position[0]}px`,
    "--y": `${position[1]}px`,
    "--width": `${width}px`,
    "--height": `${height}px`,
  } as CSSProperties;

  return (
    <PanelContext.Provider value={{ width, height }}>
      <div
        className={styles.panel}
        style={style}
        data-expanded={expanded}
        data-dragging={isDragging}
        data-resizing={showResizeHandles && expanded}
      >
        <header
          className={styles.header}
          onContextMenu={handleContextMenu}
          ref={headerRef}
        >
          <img
            src={`./icons/${category.toLowerCase()}.svg`}
            aria-hidden="true"
            className={styles.icon}
          />
          <h2 className={styles.title}>{category}</h2>
          <button className={styles.toggle} onClickCapture={toggleExpanded}>
            <div className={styles.toggleIcon} />
          </button>
        </header>
        <AnimatePresence initial={false}>
          {expanded && (
            <Resizable
              className={styles.resizable}
              width={width}
              height={expanded ? height : 42}
              minHeight={100}
              minWidth={230}
              maxHeight={800}
              maxWidth={400}
              resizeHandles={["e", "s", "se"]}
              onResize={(_event, { size }) => {
                setDimensions(() => ({
                  width: size.width,
                  height: size.height,
                }));

                localStorage.setItem(
                  `clickgui.panel.${category}.dimensions`,
                  `${size.width},${size.height}`
                );
              }}
            >
              <motion.div
                className={styles.modules}
                variants={{
                  hidden: {
                    opacity: 0,
                    height: 0,
                  },
                  visible: {
                    opacity: 1,
                    height: "100%",
                  },
                }}
                transition={{
                  bounce: 0,
                  ease: "easeInOut",
                  duration: 0.2,
                }}
                initial="hidden"
                animate="visible"
                exit="hidden"
              >
                <div className={styles.moduleList}>
                  {modules.map((module) => (
                    <ModuleItem module={module} key={module.name} />
                  ))}
                </div>
              </motion.div>
            </Resizable>
          )}
        </AnimatePresence>
      </div>
    </PanelContext.Provider>
  );
}
