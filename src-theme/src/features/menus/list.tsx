import { AnimatePresence, motion } from "framer-motion";

import styles from "./list.module.css";

type ListProps = {
  children: React.ReactNode;
  loading?: boolean;
};

export default function List({ children, loading }: ListProps) {
  return (
    <motion.div className={styles.listContainer}>
      <motion.div className={styles.list} data-loading={loading}>
        {loading &&
          Array.from({ length: 50 }).map((_, idx) => (
            <ListItem key={idx} loading />
          ))}

        <AnimatePresence initial={false} mode="popLayout">
          {children}
        </AnimatePresence>
      </motion.div>
    </motion.div>
  );
}

type ListItemProps =
  | {
      children: React.ReactNode;
      loading?: false;
    }
  | {
      children?: never;
      loading: true;
    };

export function ListItem({ children, loading = false }: ListItemProps) {
  return (
    <motion.div
      className={styles.listItemWrapper}
      layout
      variants={{
        show: {
          opacity: 1,
        },
        hide: {
          opacity: 0,
        },
      }}
      initial="hide"
      animate="show"
      exit="hide"
    >
      <div className={styles.listItem} data-loading={loading}>
        {loading ? (
          <div className={styles.loading}>
            <div className={styles.loadingIcon} />
            <div className={styles.loadingText}>
              <div className={styles.loadingTextLine} />
              <div className={styles.loadingTextLine} />
            </div>
          </div>
        ) : (
          children
        )}
      </div>
    </motion.div>
  );
}
