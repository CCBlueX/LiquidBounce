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
    <DialogOverlay />
    <DialogPrimitive.Content ref={ref} className={styles.content} {...props}>
      {children}
      <DialogPrimitive.Close className={styles.close}>
        <X className={styles.closeIcon} />
        <span className="sr-only">Close</span>
      </DialogPrimitive.Close>
    </DialogPrimitive.Content>
  </DialogPortal>
));
DialogContent.displayName = DialogPrimitive.Content.displayName;

const DialogHeader = (props: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={styles.header} {...props} />
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
