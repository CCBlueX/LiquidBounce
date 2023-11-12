import {motion} from "framer-motion";

import {Module, useModules} from "../useModules";

import styles from "./module.module.scss";

type ModuleProps = {
    module: Module;
};

export default function ModuleItem({module}: ModuleProps) {
    const {toggleModule} = useModules()

    return (
        <motion.div
            className={styles.module}
            data-enabled={module.enabled}
            variants={{
                hidden: {
                    opacity: 0,
                    y: -10,
                },
                visible: {
                    opacity: 1,
                    y: 0,
                },
            }}
            transition={{
                bounce: 0,
                ease: "easeOut",
            }}
            onClickCapture={() => toggleModule(module.name)}
        >
            {module.name}
        </motion.div>
    );
}
