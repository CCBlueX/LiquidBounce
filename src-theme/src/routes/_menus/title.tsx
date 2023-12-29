import { motion } from "framer-motion";

import Button from "~/components/button";

import Menu from "~/features/menus/menu";
import Footer from "~/features/menus/footer";

import MainButton from "~/features/menus/title/main-button";

// Main Buttons
import { ReactComponent as Singleplayer } from "~/assets/icons/singleplayer.svg";
import { ReactComponent as Multiplayer } from "~/assets/icons/multiplayer.svg";
import { ReactComponent as Customize } from "~/assets/icons/customize.svg";
import { ReactComponent as Options } from "~/assets/icons/options.svg";

// Left Footer Buttons
import { ReactComponent as Change } from "~/assets/icons/change.svg";
import { ReactComponent as Exit } from "~/assets/icons/exit.svg";

// Right Footer Buttons
import { ReactComponent as NodeBB } from "~/assets/icons/nodebb.svg";
import { ReactComponent as GitHub } from "~/assets/icons/github.svg";
import { ReactComponent as Guilded } from "~/assets/icons/guilded.svg";
import { ReactComponent as Twitter } from "~/assets/icons/twitter.svg";
import { ReactComponent as YouTube } from "~/assets/icons/youtube.svg";
import { ReactComponent as External } from "~/assets/icons/external.svg";

import styles from "./title.module.css";

export function Component() {
  return (
    <Menu>
      <Menu.Content
        variants={{
          show: {
            transition: {
              staggerChildren: 0.1,
            },
          },
          hide: {
            transition: {
              staggerChildren: 0.1,
            },
          },
        }}
      >
        <motion.div className={styles.buttons}>
          <MainButton to="/singleplayer" icon={Singleplayer}>
            Singleplayer
          </MainButton>
          <MainButton to="/multiplayer" icon={Multiplayer}>
            Multiplayer
          </MainButton>
          <MainButton to="#/settings" icon={Customize}>
            Customize
          </MainButton>
          <MainButton to="#/credits" icon={Options}>
            Options
          </MainButton>
        </motion.div>
      </Menu.Content>
      <Footer>
        <Footer.Actions>
          <Button icon={Change}>Change Background</Button>
          <Button icon={Exit}>Exit</Button>
        </Footer.Actions>
        <Footer.Actions>
          <Button variant="ghost" icon={NodeBB} tooltip="Forum" />
          <Button variant="ghost" icon={GitHub} tooltip="GitHub" />
          <Button variant="ghost" icon={Guilded} tooltip="Guilded" />
          <Button variant="ghost" icon={Twitter} tooltip="Twitter" />
          <Button variant="ghost" icon={YouTube} tooltip="YouTube" />
          <Button icon={External}>liquidbounce.net</Button>
        </Footer.Actions>
      </Footer>
    </Menu>
  );
}

Component.displayName = "TitleScreen";
