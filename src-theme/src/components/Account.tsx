import { Link } from "react-router-dom";

import { ReactComponent as Pen } from "~/assets/icons/pen.svg";

import styles from "./account.module.css";
import Tooltip from "./Tooltip";

export default function Account() {
  // TODO: Pull these from the client
  const username = "NurMarvin";
  const accountType = "Premium";
  const faceUrl =
    "https://crafatar.com/avatars/3e395dd4-7158-4641-a469-35001933cf70?size=100";
  const location = "de";

  return (
    <div className={styles.account}>
      <div className={styles.faceWrapper}>
        <object data={faceUrl} type="image/png" className={styles.face}>
          <img src={faceUrl} alt="Face" />
        </object>
        <Tooltip text="Change Location">
          <Link to="/proxies" className={styles.location}>
            <img
              src={`/flags/${location}.svg`}
              alt="Location"
            />
          </Link>
        </Tooltip>
      </div>
      <div className={styles.accountInfo}>
        <h3 className={styles.username}>{username}</h3>
        <span className={styles.accountType}>{accountType}</span>
      </div>
      <Tooltip text="Change Account">
        <Link to="/accounts" className={styles.changeAccount}>
          <Pen className={styles.pen} />
        </Link>
      </Tooltip>
    </div>
  );
}
