import { motion } from "framer-motion";
import { forwardRef } from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";

import { ReactComponent as X } from "~/assets/icons/x.svg";

import styles from "./dialog.module.css";

const Dialog = DialogPrimitive.Root;

const DialogTrigger = DialogPrimitive.Trigger;

const DialogPortal = DialogPrimitive.Portal;

const DialogClose = DialogPrimitive.Close;

const DialogOverlay = forwardRef<
  React.ElementRef<typeof DialogPrimitive.Overlay>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Overlay>
>((props, ref) => (
  <DialogPrimitive.Overlay ref={ref} className={styles.overlay} {...props} />
));
DialogOverlay.displayName = DialogPrimitive.Overlay.displayName;

const DialogContent = forwardRef<
  React.ElementRef<typeof DialogPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Content>
>(({ children, ...props }, ref) => (
  <DialogPortal>
    <DialogOverlay>
      <DialogPrimitive.Close className={styles.close}>
        <X className={styles.closeIcon} />
        <span className="sr-only">Close</span>
      </DialogPrimitive.Close>
    </DialogOverlay>

    <motion.div
      className={styles.dialog}
      variants={{
        show: {
          opacity: 1,
          y: 0,
        },
        hide: {
          opacity: 0,
          y: 10,
        },
      }}
      transition={{
        type: "spring",
        stiffness: 500,
        damping: 30,
        duration: 0.2,
      }}
      initial="hide"
      animate="show"
      exit="hide"
    >
      <DialogPrimitive.Content ref={ref} className={styles.content} {...props}>
        {children}
      </DialogPrimitive.Content>
    </motion.div>
  </DialogPortal>
));
DialogContent.displayName = DialogPrimitive.Content.displayName;

const DialogHeader = ({
  children,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={styles.header} {...props}>
    {children}
    <div className={styles.divider} />
  </div>
);
DialogHeader.displayName = "DialogHeader";

const DialogTitle = forwardRef<
  React.ElementRef<typeof DialogPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Title>
>((props, ref) => (
  <DialogPrimitive.Title ref={ref} className={styles.title} {...props} />
));
DialogTitle.displayName = DialogPrimitive.Title.displayName;

export {
  Dialog,
  DialogPortal,
  DialogOverlay,
  DialogClose,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
};
