import { motion } from "framer-motion";
import Fuse from "fuse.js";
import { Link } from "react-router-dom";
import { useMemo, useState } from "react";

import AnimatedFooter from "~/components/AnimatedFooter";
import Button from "~/components/Button";

import Header from "~/features/multiplayer/Header";
import ServerEntry from "~/features/multiplayer/ServerEntry";
import { useServers } from "~/features/multiplayer/useServers";

// Left Footer Buttons
import { ReactComponent as Add } from "~/assets/icons/add.svg";
import { ReactComponent as DirectConnect } from "~/assets/icons/direct-connect.svg";
import { ReactComponent as Refresh } from "~/assets/icons/refresh.svg";

// Right Footer Buttons
import { ReactComponent as Back } from "~/assets/icons/back.svg";

export default function Multiplayer() {
  const { servers } = useServers();
  const [search, setSearch] = useState("");

  const filteredServers = useMemo(() => {
    if (!search) return servers;

    const fuse = new Fuse(servers, {
      keys: ["name", "motd", "ip"],
    });

    return fuse.search(search).map((result) => result.item);
  }, [search, servers]);

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
          {/* Servers */}
          <div className="flex flex-col space-y-4">
            {filteredServers.map((server) => (
              <ServerEntry key={server.name} server={server} />
            ))}
          </div>
        </motion.div>
      </motion.div>
      <AnimatedFooter className="flex justify-between items-center">
        {/* Actions */}
        <div className="h-[92px] bg-black/70 rounded-xl flex items-center space-x-8 px-8">
          <Button icon={Add}>Add</Button>
          <Button icon={DirectConnect}>Direct</Button>
          <Button icon={Refresh}>Refresh</Button>
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
