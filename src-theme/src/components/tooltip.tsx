import * as TooltipPrimitive from "@radix-ui/react-tooltip";

import styles from "./tooltip.module.css";

export const TooltipProvider = TooltipPrimitive.Provider;

type TooltipProps = {
  text: string;
  delay?: number;
  children: React.ReactNode;
};

export default function Tooltip({ text, children, delay = 0 }: TooltipProps) {
  return (
    <TooltipPrimitive.Root delayDuration={delay}>
      <TooltipPrimitive.Trigger asChild>{children}</TooltipPrimitive.Trigger>
      <TooltipPrimitive.Content className={styles.content}>
        {text}
      </TooltipPrimitive.Content>
    </TooltipPrimitive.Root>
  );
}
