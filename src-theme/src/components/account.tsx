import { Link } from "react-router-dom";

import { ReactComponent as Pen } from "~/assets/icons/pen.svg";

import styles from "./account.module.css";
import Tooltip from "./tooltip";
import { useQuery } from "react-query";
import {getCurrentLocation, getCurrentSession} from "~/utils/api";

export default function Account() {
  // TODO: Pull these from the client
  const { data: session } = useQuery("session", getCurrentSession);
  const { data: location } = useQuery("location", getCurrentLocation);

  return (
    <div className={styles.account}>
      <div className={styles.faceWrapper}>
        <object data={session?.avatar} type="image/png" className={styles.face}>
          <img src={session?.avatar} alt="Face" />
        </object>
        <Tooltip text="Change Location">
          <Link to="/proxies" className={styles.location}>
            <img src={`./flags/${location?.country?.toLowerCase()}.svg`} alt="Location" />
          </Link>
        </Tooltip>
      </div>
      <div className={styles.accountInfo}>
        <h3 className={styles.username}>{session?.username}</h3>
        <span className={styles.accountType}>{session?.premium ? "Premium" : "Cracked"}</span>
      </div>
      <Tooltip text="Change Account">
        <Link to="/accounts" className={styles.changeAccount}>
          <Pen className={styles.pen} />
        </Link>
      </Tooltip>
    </div>
  );
}
