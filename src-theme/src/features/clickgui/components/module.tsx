import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react";

import { ModuleSetting, useModules } from "../use-modules";

import { ReactComponent as Chevron } from "~/assets/icons/chevron.svg";

import BooleanModuleSetting from "./settings/boolean-setting";
import UnknownModuleSetting from "./settings/unknown-setting";
import SliderModuleSetting from "./settings/slider-setting";
import EnumModuleSetting from "./settings/enum-setting";
import StringModuleSetting from "./settings/string-setting";
import ColorModuleSetting from "./settings/color-setting";

import styles from "./module.module.scss";
import { Module } from "~/utils/api";
import ModuleSettings from "./module-setting";

type ModuleProps = {
  module: Module;
};

export default function ModuleItem({ module }: ModuleProps) {
  const { toggleModule } = useModules();

  const [expanded, setExpanded] = useState(() => {
    const expanded = localStorage.getItem(
      `clickgui.module.${module.name}.expanded`
    );
    return expanded === "true";
  });

  function toggleExpanded() {
    setExpanded(!expanded);
    localStorage.setItem(
      `clickgui.module.${module.name}.expanded`,
      `${!expanded}`
    );
  }

  function handleContextMenu(event: React.MouseEvent<HTMLDivElement>) {
    event.preventDefault();
    toggleExpanded();
  }

  return (
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
        data-enabled={module.enabled}
        data-expanded={expanded}
        className={styles.module}
        onContextMenu={handleContextMenu}
        onClickCapture={() => toggleModule(module.name)}
      >
        {module.name}
        <Chevron className={styles.chevron} />
      </div>

      <AnimatePresence initial={false} mode="popLayout">
        {expanded && <ModuleSettings module={module} />}
      </AnimatePresence>
    </motion.div>
  );
}
