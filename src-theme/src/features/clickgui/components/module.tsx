import { AnimatePresence, motion } from "framer-motion";
import { CSSProperties, useRef, useState } from "react";
import { useMutation } from "react-query";

import ModuleSettings from "./settings";

import { Module } from "~/utils/api";

import { ReactComponent as Chevron } from "~/assets/icons/chevron.svg";

import styles from "./module.module.scss";

type ModuleProps = {
  module: Module;
};

export default function ModuleItem({ module }: ModuleProps) {
  const { mutate: toggleModule } = useMutation((name: string) => {
    return fetch(`/api/module/${name}`, {
      method: "PATCH",
    });
  });

  const [expanded, setExpanded] = useState(() => {
    const expanded = localStorage.getItem(
      `clickgui.module.${module.name}.expanded`
    );
    return expanded === "true";
  });

  const [hovered, setHovered] = useState(false);
  const [mousePosition, setMousePosition] = useState([0, 0]);

  const headerRef = useRef<HTMLDivElement>(null);

  function toggleExpanded() {
    setExpanded(!expanded);
    localStorage.setItem(
      `clickgui.module.${module.name}.expanded`,
      `${!expanded}`
    );

    if (!expanded) {
      // scroll to top of module
      setTimeout(() => {
        headerRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
      }, 25);
    } else {
      // scroll to bottom of module
      setTimeout(() => {
        headerRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "end",
        });
      }, 25);
    }
  }

  function handleContextMenu(event: React.MouseEvent<HTMLDivElement>) {
    event.preventDefault();
    toggleExpanded();
  }

  function handleMouseEnter() {
    setHovered(true);
  }

  function handleMouseLeave() {
    setHovered(false);
  }

  function handleMouseMove(event: React.MouseEvent<HTMLDivElement>) {
    setMousePosition([event.clientX, event.clientY]);
  }

  return (
    <>
      <motion.div
        variants={{
          hidden: {
            opacity: 0,
            y: -10,
          },
          visible: {
            opacity: 1,
            y: 0,
          },
        }}
        transition={{
          bounce: 0,
          ease: "easeOut",
        }}
      >
        <div
          ref={headerRef}
          data-enabled={module.enabled}
          data-expanded={expanded}
          className={styles.module}
          onContextMenu={handleContextMenu}
          onClickCapture={() => toggleModule(module.name)}
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
          onMouseMove={handleMouseMove}
        >
          {module.name}
          <Chevron className={styles.chevron} />
        </div>

        <AnimatePresence initial={false} mode="popLayout">
          {expanded && <ModuleSettings module={module} />}
        </AnimatePresence>
      </motion.div>

      {hovered && (
        <div
          className={styles.description}
          style={
            {
              "--mouse-x": `${mousePosition[0]}px`,
              "--mouse-y": `${mousePosition[1]}px`,
            } as CSSProperties
          }
        >
          {module.description}
        </div>
      )}
    </>
  );
}
