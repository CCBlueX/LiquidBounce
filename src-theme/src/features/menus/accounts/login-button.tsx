import { motion } from "framer-motion";
import { useState } from "react";

import { ReactComponent as Lock } from "~/assets/icons/lock.svg";
import { ReactComponent as Person } from "~/assets/icons/person.svg";
import { ReactComponent as Next } from "~/assets/icons/next.svg";

// Account Type Icons
import { ReactComponent as Minecraft } from "~/assets/icons/minecraft.svg";
import { ReactComponent as Microsoft } from "~/assets/icons/microsoft.svg";
import { ReactComponent as MCLeaks } from "~/assets/icons/mcleaks.svg";
import { ReactComponent as Session } from "~/assets/icons/session.svg";
import { ReactComponent as TheAltening } from "~/assets/icons/the-altening.svg";

import Button from "~/components/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "~/components/dialog";

import styles from "./login-button.module.css";
import Input, { PasswordInput } from "~/components/input";
import Switch from "~/components/switch";

const accountTypes = [
  "Minecraft",
  "Microsoft",
  "TheAltening",
  "MCLeaks",
  "Session",
] as const;

type AccountType = (typeof accountTypes)[number];

const accountTypeIcons: Record<
  (typeof accountTypes)[number],
  React.FunctionComponent<React.ComponentProps<"svg"> & { title?: string }>
> = {
  Minecraft,
  Microsoft,
  TheAltening,
  MCLeaks,
  Session,
} as const;

export default function LoginButton() {
  const [accountType, setAccountType] = useState<AccountType>("Minecraft");
  const [favorite, setFavorite] = useState(false);
  const [premium, setPremium] = useState(false);
  const [directLogin, setDirectLogin] = useState(false);

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    console.log("submit");
  }

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button icon={Next}>Login</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Login</DialogTitle>
        </DialogHeader>
        <motion.div className={styles.accountTypes}>
          {accountTypes.map((type) => {
            const Icon = accountTypeIcons[type];
            const selected = type === accountType;

            return (
              <button
                className={styles.accountType}
                key={type}
                onClick={() => setAccountType(type)}
                data-selected={selected}
              >
                {selected && (
                  <motion.span
                    layoutId="account-type-border"
                    className={styles.border}
                    transition={{
                      ease: "easeInOut",
                      duration: 0.2,
                    }}
                  />
                )}
                <Icon className={styles.icon} />
                <div className={styles.label}>{type}</div>
              </button>
            );
          })}
        </motion.div>
        <form className={styles.form} onSubmit={handleSubmit}>
          <Input label="E-Mail" icon={Person} type="email" />
          <PasswordInput label="Password" icon={Lock} />

          <div className={styles.options}>
            <Switch value={favorite} onChange={setFavorite}>
              Favorite
            </Switch>
            <Switch value={premium} onChange={setPremium}>
              Premium
            </Switch>
            <Switch value={directLogin} onChange={setDirectLogin}>
              Direct Login
            </Switch>
          </div>

          <button className={styles.submit} type="submit">
            Add
          </button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
