import {
  Popover,
  PopoverContent,
  PopoverPortal,
  PopoverTrigger,
} from "@radix-ui/react-popover";
import { AnimatePresence, motion } from "framer-motion";
import { CSSProperties, useState } from "react";

import ModuleSetting from "./setting";
import { EnumModuleSetting } from "../../use-modules";

import { ReactComponent as Chevron } from "~/assets/icons/chevron.svg";

import styles from "./setting.module.scss";
import { usePanelContext } from "../panel";

type EnumModuleSettingProps = {
  setting: EnumModuleSetting;
};

export default function EnumModuleSetting({ setting }: EnumModuleSettingProps) {
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState(setting.value);

  const { width } = usePanelContext();

  const style = {
    "--width": `${width}px`,
  } as CSSProperties;

  return (
    <ModuleSetting data-type={setting.type}>
      <Popover open={open} onOpenChange={setOpen}>
        <div className={styles.container}>
          <PopoverTrigger className={styles.trigger}>
            <span className={styles.label}>{value}</span>
            <Chevron className={styles.chevron} />
          </PopoverTrigger>
          <PopoverPortal>
            <AnimatePresence mode="popLayout">
              <PopoverContent align="center" asChild>
                <motion.div
                  className={styles.enumOptions}
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
                  {setting.values.map((option) => (
                    <motion.button
                      key={option}
                      className={styles.option}
                      onClick={() => {
                        setValue(option);
                        setOpen(false);
                      }}
                      role="option"
                      aria-selected={option === value}
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
                      {option}
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
