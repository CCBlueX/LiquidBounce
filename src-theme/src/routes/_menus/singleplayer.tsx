import { motion } from "framer-motion";
import Fuse from "fuse.js";
import { Link } from "react-router-dom";
import { useMemo, useState } from "react";

import AnimatedFooter from "~/components/AnimatedFooter";
import Button from "~/components/Button";

import Header from "~/features/singleplayer/Header";
import WorldEntry from "~/features/singleplayer/WorldEntry";

// Left Footer Buttons
import { ReactComponent as Add } from "~/assets/icons/add.svg";

// Right Footer Buttons
import { ReactComponent as Back } from "~/assets/icons/back.svg";
import { useWorlds } from "~/features/singleplayer/useWorlds";

export default function Singleplayer() {
  const { worlds } = useWorlds();
  const [search, setSearch] = useState("");

  const filteredWorlds = useMemo(() => {
    if (!search) return worlds;

    const fuse = new Fuse(worlds, {
      keys: ["name"],
    });

    return fuse.search(search).map((result) => result.item);
  }, [search, worlds]);

  return (
    <div className="flex flex-col space-y-8 flex-1 justify-between">
      <motion.div
        variants={{
          show: {
            x: 0,
            opacity: 1,
            transition: {
              delay: 0.5,
              duration: 0.5,
              ease: "anticipate",
            },
          },
          hide: {
            x: "100%",
            opacity: 0,
            transition: {
              duration: 1,
              ease: "anticipate",
            },
          },
        }}
        initial="hide"
        animate="show"
        exit="hide"
        className="flex flex-col space-y-8 flex-1"
      >
        <Header onSearch={setSearch} />
        <motion.div className="py-4 px-8 bg-black/70 rounded-xl flex-1">
          <div className="flex flex-col space-y-4">
            {filteredWorlds.map((world) => (
              <WorldEntry key={world.name} world={world} />
            ))}
          </div>
        </motion.div>
      </motion.div>
      <AnimatedFooter className="flex justify-between items-center">
        {/* Actions */}
        <div className="h-[92px] bg-black/70 rounded-xl flex items-center space-x-8 px-8">
          <Button icon={Add}>Add</Button>
        </div>

        {/* Back Button */}
        <div className="h-[92px] bg-black/70 rounded-xl flex items-center space-x-8 px-8">
          <Link to="/title">
            <Button icon={Back}>Back</Button>
          </Link>
        </div>
      </AnimatedFooter>
    </div>
  );
}
