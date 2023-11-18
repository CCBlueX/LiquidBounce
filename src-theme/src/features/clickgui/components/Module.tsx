import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react";

import { Module, ModuleSetting, useModules } from "../use-modules";

import { ReactComponent as Chevron } from "~/assets/icons/chevron.svg";

import BooleanModuleSetting from "./settings/boolean-setting";
import UnknownModuleSetting from "./settings/unknown-setting";
import SliderModuleSetting from "./settings/slider-setting";
import EnumModuleSetting from "./settings/enum-setting";
import StringModuleSetting from "./settings/string-setting";
import ColorModuleSetting from "./settings/color-setting";

import styles from "./module.module.scss";

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

  function renderModuleSetting(setting: ModuleSetting) {
    let component;

    switch (setting.type) {
      case "boolean":
        component = (
          <BooleanModuleSetting setting={setting} key={setting.name} />
        );
        break;
      case "number":
      case "range":
        component = (
          <SliderModuleSetting setting={setting} key={setting.name} />
        );
        break;
      case "enum":
        component = <EnumModuleSetting setting={setting} key={setting.name} />;
        break;
      case "string":
        component = (
          <StringModuleSetting setting={setting} key={setting.name} />
        );
        break;
      case "color":
        component = <ColorModuleSetting setting={setting} key={setting.name} />;
        break;
      default:
        component = <UnknownModuleSetting setting={setting} />;
        break;
    }

    return component;
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
        {expanded && (
          <motion.div
            className={styles.settings}
            variants={{
              hidden: {
                opacity: 0,
                height: 0,
              },
              visible: {
                opacity: 1,
                height: "auto",
                transition: {
                  staggerChildren: 0.05,
                  bounce: 0,
                  ease: "easeInOut",
                  duration: 0.2,
                },
              },
            }}
            initial="hidden"
            animate="visible"
            exit="hidden"
          >
            {module.settings.map(renderModuleSetting)}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
