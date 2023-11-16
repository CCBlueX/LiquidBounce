import { AnimatePresence, motion } from "framer-motion";
import { type CSSProperties, useState } from "react";

import { Module } from "~/features/clickgui/use-modules";

import styles from "./panel.module.scss";
import ModuleItem from "~/features/clickgui/components/module";
import useDraggable from "~/hooks/use-draggable.tsx";

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
            {modules.map((module) => (
              <ModuleItem module={module} key={module.name} />
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
