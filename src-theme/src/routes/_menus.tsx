import { AnimatePresence, motion } from "framer-motion";
import { useLocation } from "react-router-dom";

import Account from "~/components/account";
import AnimatedOutlet from "~/components/animated-outlet";

import { ReactComponent as Logo } from "~/assets/logo.svg";

import background from "~/assets/background.png";

import styles from "./_menus.module.css";

export default function MenuWrapper() {
  const location = useLocation();

  return (
    <div className={styles.wrapper}>
      <img
        src={background}
        aria-hidden="true"
        className={styles.background}
        alt=""
      />
      <div className={styles.container}>
        <motion.header
          initial={{ opacity: 0, y: -200 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -200 }}
          transition={{ duration: 1, ease: "anticipate" }}
          className={styles.header}
        >
          <Logo className={styles.logo} />
          <Account />
        </motion.header>
        <AnimatePresence mode="popLayout">
          <motion.main
            className={styles.content}
            key={location.pathname}
            variants={{
              show: {},
              hide: {},
            }}
            transition={{ duration: 1, ease: "anticipate" }}
          >
            <AnimatedOutlet />
          </motion.main>
        </AnimatePresence>
      </div>
    </div>
  );
}
