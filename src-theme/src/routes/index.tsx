import { motion } from "framer-motion";
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { ReactComponent as Logo } from "~/assets/logo.svg";

import styles from "./index.module.css";

export default function SplashScreen() {
  const navigate = useNavigate();

  useEffect(() => {
    const timeout = setTimeout(() => {
      navigate("/title");
    }, 1500);

    return () => {
        clearTimeout(timeout);
    }
  });

  return (
    <motion.div
      className={styles.wrapper}
      variants={{
        show: {
          opacity: 1,
          transition: {
            duration: 1,
            ease: "anticipate",
          },
        },
        hide: {
          opacity: 0,
          transition: {
            duration: 1,
            ease: "anticipate",
          },
        },
      }}
      initial="hide"
      animate="show"
    >
      <Logo className={styles.logo} />
    </motion.div>
  );
}
