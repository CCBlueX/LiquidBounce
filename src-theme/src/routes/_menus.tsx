import { AnimatePresence, motion } from "framer-motion";
import { useLocation } from "react-router-dom";

import AnimatedOutlet from "~/components/AnimatedOutlet";

import { ReactComponent as Logo } from "~/assets/logo.svg";

import background from "~/assets/background.png";

import styles from "./_menus.module.css";
import Account from "~/components/Account";

export default function MenuWrapper() {
  const location = useLocation();

  return (
    <div className={styles.wrapper}>
      <motion.img
        src={background}
        loading="eager"
        className={styles.background}
        alt="background"
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
            className="flex flex-1"
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
