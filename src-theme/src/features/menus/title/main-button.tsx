import { Link } from "react-router-dom";

import { motion } from "framer-motion";

import styles from "./main-button.module.css";

type MainButtonProps = {
  children: React.ReactNode;
  icon: React.ElementType;
  to: string;
};

export default function MainButton({
  children,
  icon: Icon,
  to,
}: MainButtonProps) {
  return (
    <motion.div
      variants={{
        show: {
          opacity: 1,
          x: 0,
        },
        hide: {
          opacity: 0,
          x: -1000,
        },
      }}
      key={to}
      transition={{ duration: 1, ease: "anticipate" }}
    >
      <Link to={to} className={styles.button}>
        <div className={styles.iconWrapper}>
          <Icon className={styles.icon} />
        </div>
        <div className={styles.label}>{children}</div>
      </Link>
    </motion.div>
  );
}
