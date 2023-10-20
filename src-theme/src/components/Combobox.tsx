import {
  Popover,
  PopoverContent,
  PopoverPortal,
  PopoverTrigger,
} from "@radix-ui/react-popover";
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
};

export default function Combobox({
  children,
  options,
  onToggle,
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
          <PopoverContent align="center" className={styles.options}>
            {options.map((option) => (
              <button
                key={option.value}
                className={styles.option}
                onClick={() => onToggle?.(option)}
                role="option"
                aria-selected={option.checked}
                tabIndex={0}
              >
                {option.label}
                {option.checked && (
                  <Check className={styles.check} aria-hidden />
                )}
              </button>
            ))}
          </PopoverContent>
        </PopoverPortal>
      </div>
    </Popover>
  );
}
