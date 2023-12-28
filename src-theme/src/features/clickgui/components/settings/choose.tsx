import {
  Popover,
  PopoverContent,
  PopoverPortal,
  PopoverTrigger,
} from "@radix-ui/react-popover";
import { AnimatePresence, motion } from "framer-motion";
import { type CSSProperties, useState } from "react";

import ModuleSetting from "./setting";

import { usePanelContext } from "../panel";

import { type Module, type ChooseValue } from "~/utils/api";

import { ReactComponent as Chevron } from "~/assets/icons/chevron.svg";

import styles from "./settings.module.scss";

type ModuleChooseSettingProps = {
  setting: ChooseValue;
  module: Module;
};

export default function ModuleChooseSetting({
  setting,
  module,
}: ModuleChooseSettingProps) {
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState(setting.value);

  const { width } = usePanelContext();

  const style = {
    "--width": `${width}px`,
  } as CSSProperties;

  return (
    <ModuleSetting data-type="choose">
      <Popover open={open} onOpenChange={setOpen}>
        <div className={styles.container}>
          <PopoverTrigger className={styles.trigger}>
            <span className={styles.label}>
              {setting.name}: {value}
            </span>
            <Chevron className={styles.chevron} />
          </PopoverTrigger>
          <PopoverPortal>
            <AnimatePresence mode="popLayout">
              <PopoverContent align="center" asChild>
                <motion.div
                  className={styles.chooseOptions}
                  variants={{
                    show: {
                      height: "auto",
                    },
                    hide: {
                      height: 0,
                    },
                  }}
                  initial="hide"
                  animate="show"
                  exit="hide"
                  style={style}
                  transition={{
                    staggerChildren: 0.01,
                  }}
                >
                  {setting.choices.map((choice) => (
                    <motion.button
                      key={choice}
                      className={styles.option}
                      onClick={() => {
                        setValue(choice);
                        setOpen(false);
                      }}
                      role="option"
                      aria-selected={choice === value}
                      tabIndex={0}
                      variants={{
                        show: {
                          opacity: 1,
                          y: 0,
                        },
                        hide: {
                          opacity: 0,
                          y: -200,
                        },
                      }}
                      transition={{ duration: 0.2 }}
                    >
                      {choice}
                    </motion.button>
                  ))}
                </motion.div>
              </PopoverContent>
            </AnimatePresence>
          </PopoverPortal>
        </div>
      </Popover>
    </ModuleSetting>
  );
}
