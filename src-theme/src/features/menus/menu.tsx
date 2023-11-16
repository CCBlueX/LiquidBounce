import { Variant, motion } from "framer-motion";

import styles from "./menu.module.css";

type MenuProps = {
  children: React.ReactNode;
};

export default function Menu({ children }: MenuProps) {
  return <div className={styles.menu}>{children}</div>;
}

type MenuContentProps = {
  className?: string;
  children: React.ReactNode;
  variants?: {
    show: Variant;
    hide: Variant;
  };
};

function MenuContent({ className, children, variants }: MenuContentProps) {
  return (
    <motion.div
      variants={
        variants ?? {
          show: {
            x: 0,
            opacity: 1,
            transition: {
              delay: 0.5,
              duration: 0.5,
              ease: "anticipate",
            },
          },
          hide: {
            x: "100%",
            opacity: 0,
            transition: {
              duration: 1,
              ease: "anticipate",
            },
          },
        }
      }
      initial="hide"
      animate="show"
      exit="hide"
      className={[styles.container, className].filter(Boolean).join(" ")}
    >
      {children}
    </motion.div>
  );
}

Menu.Content = MenuContent;
