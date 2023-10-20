import { motion } from "framer-motion";

type AnimatedFooterProps = {
  children: React.ReactNode;
  className?: string;
};

export default function AnimatedFooter({
  children,
  className,
}: AnimatedFooterProps) {
  return (
    <motion.footer
      className={className}
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
