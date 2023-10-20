import { motion } from "framer-motion";

import AnimatedFooter from "~/components/AnimatedFooter";
import Button from "~/components/Button";

// Main Buttons
import { ReactComponent as Singleplayer } from "~/assets/icons/singleplayer.svg";
import { ReactComponent as Multiplayer } from "~/assets/icons/multiplayer.svg";
import { ReactComponent as Customize } from "~/assets/icons/customize.svg";
import { ReactComponent as Options } from "~/assets/icons/options.svg";

// Left Footer Buttons
import { ReactComponent as ChangeBackground } from "~/assets/icons/change-background.svg";
import { ReactComponent as Exit } from "~/assets/icons/exit.svg";

// Right Footer Buttons

import { ReactComponent as NodeBB } from "~/assets/icons/nodebb.svg";
import { ReactComponent as GitHub } from "~/assets/icons/github.svg";
import { ReactComponent as Guilded } from "~/assets/icons/guilded.svg";
import { ReactComponent as Twitter } from "~/assets/icons/twitter.svg";
import { ReactComponent as YouTube } from "~/assets/icons/youtube.svg";
import { ReactComponent as External } from "~/assets/icons/external.svg";

import MainButton from "~/features/title/components/MainButton";

export default function TitleScreen() {
  return (
    <div className="flex flex-col space-y-8 flex-1 justify-between">
      {/* Main Buttons */}

      <motion.div
        className="flex flex-col space-y-8 w-[600px]"
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
        initial="hide"
        animate="show"
        exit="hide"
      >
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

      {/* Footer */}
      <AnimatedFooter className="flex justify-between items-center">
        {/* Actions */}
        <div className="h-[92px] bg-black/70 rounded-xl flex items-center space-x-8 px-8 w-[600px]">
          <Button icon={ChangeBackground}>Change Background</Button>
          <Button icon={Exit}>Exit</Button>
        </div>

        {/* Socials */}
        <div className="h-[92px] bg-black/70 rounded-xl flex items-center space-x-8 px-8">
          <Button variant="ghost" icon={NodeBB} tooltip="Forum" />
          <Button variant="ghost" icon={GitHub} tooltip="GitHub" />
          <Button variant="ghost" icon={Guilded} tooltip="Guilded" />
          <Button variant="ghost" icon={Twitter} tooltip="Twitter" />
          <Button variant="ghost" icon={YouTube} tooltip="YouTube" />
          <Button icon={External}>liquidbounce.net</Button>
        </div>
      </AnimatedFooter>
    </div>
  );
}
