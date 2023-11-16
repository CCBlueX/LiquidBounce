import { motion } from "framer-motion";
import { Link } from "react-router-dom";

import Button from "~/components/button";

import { ReactComponent as Back } from "~/assets/icons/back.svg";

import styles from "./footer.module.css";

type FooterProps = {
  children: React.ReactNode;
};

export default function Footer({ children }: FooterProps) {
  return (
    <motion.footer
      className={styles.footer}
      variants={{
        show: {
          y: 0,
          opacity: 1,
          transition: {
            delay: 0.3,
            duration: 0.8,
            delayChildren: 0.1,
            staggerDirection: -1,
          },
        },
        hide: {
          y: 400,
          opacity: 0,
          transition: {
            duration: 0.8,
            staggerChildren: 0.1,
          },
        },
      }}
      initial="hide"
      animate="show"
      exit="hide"
      transition={{ duration: 1, ease: "anticipate" }}
    >
      {children}
    </motion.footer>
  );
}

type FooterActionsProps = {
  children: React.ReactNode;
};

function FooterActions({ children }: FooterActionsProps) {
  return <div className={styles.actions}>{children}</div>;
}

Footer.Actions = FooterActions;

function FooterBack() {
  return (
    <Footer.Actions>
      <Link to="/title">
        <Button icon={Back}>Back</Button>
      </Link>
    </Footer.Actions>
  );
}

Footer.Back = FooterBack;
