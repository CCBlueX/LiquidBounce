import {
  Popover,
  PopoverContent,
  PopoverPortal,
  PopoverTrigger,
} from "@radix-ui/react-popover";
import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react";

import { ReactComponent as Check } from "~/assets/icons/check.svg";
import { ReactComponent as Chevron } from "~/assets/icons/chevron.svg";

import styles from "./combobox.module.css";

export type Option = {
  label: string;
  value: string;
  checked?: boolean;
};

type ComboboxProps = {
  children: React.ReactNode;
  options: Option[];
  onToggle?: (option: Option) => void;
  closeOnSelect?: boolean;
};

export default function Combobox({
  children,
  options,
  onToggle,
  closeOnSelect = false,
}: ComboboxProps) {
  const [open, setOpen] = useState(false);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <div className={styles.container}>
        <PopoverTrigger className={styles.trigger}>
          <span className={styles.label}>{children}</span>
          <Chevron className={styles.chevron} />
        </PopoverTrigger>
        <PopoverPortal>
          <AnimatePresence mode="popLayout">
            <PopoverContent align="center" asChild>
              <motion.div
                className={styles.options}
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
                transition={{
                  staggerChildren: 0.01,
                }}
              >
                {options.map((option) => (
                  <motion.button
                    key={option.value}
                    className={styles.option}
                    onClick={() => {
                      onToggle?.(option);
                      if (closeOnSelect) {
                        setOpen(false);
                      }
                    }}
                    role="option"
                    aria-selected={option.checked}
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
                    {option.label}
                    {option.checked && (
                      <Check className={styles.check} aria-hidden />
                    )}
                  </motion.button>
                ))}
              </motion.div>
            </PopoverContent>
          </AnimatePresence>
        </PopoverPortal>
      </div>
    </Popover>
  );
}
