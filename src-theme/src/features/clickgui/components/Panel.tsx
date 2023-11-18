import { AnimatePresence, motion } from "framer-motion";
import { type CSSProperties, useState } from "react";

import Resizable from "~/components/resizable";

import { Module } from "~/features/clickgui/use-modules";

import ModuleItem from "./module";

import useDraggable from "~/hooks/use-draggable.tsx";

import styles from "./panel.module.scss";

type PanelProps = {
  category: string;
  modules: Module[];
  startPosition: [number, number];
};

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
      width: 225,
      height: Math.max(100, Math.min(500, modules.length * 40 + 30)),
    };
  });

  const [expanded, setExpanded] = useState(() => {
    const expanded = localStorage.getItem(
      `clickgui.panel.${category}.expanded`
    );
    return expanded === "true";
  });

  function toggleExpanded() {
    setExpanded(!expanded);
    localStorage.setItem(`clickgui.panel.${category}.expanded`, `${!expanded}`);
  }

  function handleContextMenu(event: React.MouseEvent<HTMLDivElement>) {
    event.preventDefault();
    toggleExpanded();
  }

  const style = {
    "--x": `${position[0]}px`,
    "--y": `${position[1]}px`,
    "--width": `${width}px`,
    "--height": `${height}px`,
  } as CSSProperties;

  return (
    <div
      className={styles.panel}
      style={style}
      data-expanded={expanded}
      data-dragging={isDragging}
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
          <motion.div
            className={styles.modules}
            variants={{
              hidden: {
                opacity: 0,
                height: 0,
              },
              visible: {
                opacity: 1,
                height: "auto",
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
            <Resizable
              className={styles.resizable}
              width={width}
              height={height}
              minHeight={100}
              minWidth={200}
              maxHeight={500}
              maxWidth={400}
              resizeHandles={["e", "s"]}
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
              <>
                {modules.map((module) => (
                  <ModuleItem module={module} key={module.name} />
                ))}
              </>
            </Resizable>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
