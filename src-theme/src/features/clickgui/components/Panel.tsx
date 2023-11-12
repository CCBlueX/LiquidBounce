import {AnimatePresence, motion} from "framer-motion";

import {Module} from "~/features/clickgui/useModules.tsx";

import styles from "./panel.module.scss";
import ModuleItem from "~/features/clickgui/components/Module.tsx";
import {CSSProperties, useEffect, useRef, useState} from "react";

type PanelProps = {

    category: string;
    modules: Module[];
    startPosition: [number, number];
}

export default function Panel({category, modules, startPosition}: PanelProps) {
    const [position, setPosition] = useState<[number, number]>(() => {
        const x = localStorage.getItem(`clickgui.panel.${category}.x`);
        const y = localStorage.getItem(`clickgui.panel.${category}.y`);

        // If there is a saved position, use it
        if (x && y) {
            return [parseInt(x), parseInt(y)];
        }

        // If not, use the default position
        return startPosition;
    });
    const ref = useRef<HTMLElement>(null);

    const [isDragging, setIsDragging] = useState(false);
    const [expanded, setExpanded] = useState(() => {
        const expanded = localStorage.getItem(`clickgui.panel.${category}.expanded`);
        return expanded === "true";
    });

    function handleMouseDown(event: React.MouseEvent<HTMLDivElement>) {
        if (event.button !== 0) return;

        setIsDragging(true);
    }

    useEffect(() => {
        if (!isDragging) return;

        function handleMouseMove(event: MouseEvent) {
            let width = ref.current?.offsetWidth ?? 0;
            let height = ref.current?.offsetHeight ?? 0;

            let x = event.clientX - (width / 2);
            let y = event.clientY - (height / 2);

            if (x < 0) x = 0;
            if (y < 0) y = 0;

            if (x > window.innerWidth - width) x = window.innerWidth - width;
            if (y > window.innerHeight - height) y = window.innerHeight - height;

            setPosition([x, y]);
        }

        function handleMouseUp() {
            setIsDragging(false);
            localStorage.setItem(`clickgui.panel.${category}.x`, `${position[0]}`);
            localStorage.setItem(`clickgui.panel.${category}.y`, `${position[1]}`);
            window.removeEventListener("mousemove", handleMouseMove);
            window.removeEventListener("mouseup", handleMouseUp);
        }

        window.addEventListener("mousemove", handleMouseMove);
        window.addEventListener("mouseup", handleMouseUp);
    }, [isDragging]);

    function toggleExpanded() {
        setExpanded(!expanded);
        localStorage.setItem(`clickgui.panel.${category}.expanded`, `${!expanded}`);
    }

    function handleContextMenu(event: React.MouseEvent<HTMLDivElement>) {
        event.preventDefault();
        toggleExpanded();
    }

    const style = {
        "--x": `${position[0]}px`,
        "--y": `${position[1]}px`,
    } as CSSProperties;

    return (
        <div className={styles.panel}
             style={style}
             data-expanded={expanded}
             data-dragging={isDragging}
        >
            <header className={styles.header}
                    onMouseDown={handleMouseDown}
                    onContextMenu={handleContextMenu}
                    ref={ref}
            >
                <img src={`/icons/${category.toLowerCase()}.svg`} aria-hidden="true" className={styles.icon}/>
                <h2 className={styles.title}>{category}</h2>
                <button className={styles.toggle}
                        onClickCapture={toggleExpanded}
                >
                    <div className={styles.toggleIcon}/>
                </button>
            </header>
            <AnimatePresence>
                {expanded && (
                    <motion.div
                        className={styles.modules}
                        variants={{
                            hidden: {
                                opacity: 0,
                                height: 0,
                            },
                            visible: {
                                opacity: 1,
                                height: "auto",
                            },
                        }}
                        transition={{
                            bounce: 0,
                            ease: "easeInOut",
                            duration: 0.2,
                        }}
                        initial="hidden"
                        animate="visible"
                        exit="hidden"
                    >
                        {modules.map((module) => (
                            <ModuleItem module={module} key={module.name}/>
                        ))}
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}
