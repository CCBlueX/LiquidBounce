import { motion } from "framer-motion";
import Fuse from "fuse.js";
import { Link } from "react-router-dom";
import { useMemo, useState } from "react";

import AnimatedFooter from "~/components/AnimatedFooter";
import Button from "~/components/Button";

import Header from "~/features/proxies/Header";
import ProxyEntry from "~/features/proxies/ProxyEntry";
import { useProxies } from "~/features/proxies/useProxies";

// Left Footer Buttons
import { ReactComponent as Add } from "~/assets/icons/add.svg";
import { ReactComponent as Clipboard } from "~/assets/icons/clipboard.svg";
import { ReactComponent as Import } from "~/assets/icons/import.svg";
import { ReactComponent as Shuffle } from "~/assets/icons/shuffle.svg";
import { ReactComponent as Check } from "~/assets/icons/check.svg";

// Right Footer Buttons
import { ReactComponent as Back } from "~/assets/icons/back.svg";

export default function ProxyManager() {
  const { proxies } = useProxies();
  const [search, setSearch] = useState("");

  const filteredProxies = useMemo(() => {
    if (!search) return proxies;

    const fuse = new Fuse(proxies, {
      keys: ["host", "port", "username"],
    });

    return fuse.search(search).map((result) => result.item);
  }, [search, proxies]);

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
            {filteredProxies.map((proxy) => (
              <ProxyEntry
                key={[proxy.host, proxy.port, proxy.username].join(":")}
                proxy={proxy}
              />
            ))}
          </div>
        </motion.div>
      </motion.div>
      <AnimatedFooter className="flex justify-between items-center">
        {/* Actions */}
        <div className="h-[92px] bg-black/70 rounded-xl flex items-center space-x-8 px-8">
          <Button icon={Add}>Add</Button>
          <Button icon={Clipboard}>Copy</Button>
          <Button icon={Import}>Import</Button>
          <Button icon={Shuffle}>Random</Button>
          <Button icon={Check}>Check</Button>
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
