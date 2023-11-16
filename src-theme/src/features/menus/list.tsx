import { AnimatePresence, motion } from "framer-motion";

import styles from "./list.module.css";

type ListProps = {
  children: React.ReactNode;
};

export default function List({ children }: ListProps) {
  return (
    <motion.div className={styles.listContainer}>
      <motion.div className={styles.list} layout>
        <AnimatePresence initial={false} mode="wait">
          {children}
        </AnimatePresence>
      </motion.div>
    </motion.div>
  );
}

type ListItemProps = {
  layoutId: string;
  children: React.ReactNode;
};

function ListItem({ layoutId, children }: ListItemProps) {
  return (
    <motion.div
      className={styles.listItem}
      layoutId={layoutId}
      variants={{
        show: {
          opacity: 1,
          transition: {
            duration: 0.5,
            ease: "anticipate",
          },
        },
        hide: {
          opacity: 0,
          transition: {
            duration: 0.5,
            ease: "anticipate",
          },
        },
      }}
    >
      {children}
    </motion.div>
  );
}

List.Item = ListItem;
