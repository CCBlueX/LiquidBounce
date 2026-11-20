import {reconnectToServer} from "../../../integration/rest";

const STORAGE_KEY = "disconnected_auto_reconnect";
export const RECONNECT_DELAY = 5000;

export class AutoReconnect {
    enabled = $state(localStorage.getItem(STORAGE_KEY) === "true");
    elapsed = $state(0);
    reconnecting = $state(false);

    readonly secondsLeft = $derived(Math.max(1, Math.ceil((RECONNECT_DELAY - this.elapsed) / 1000)));

    toggle() {
        this.enabled = !this.enabled;
        this.elapsed = 0;
        localStorage.setItem(STORAGE_KEY, String(this.enabled));
    }

    async reconnect() {
        if (this.reconnecting) {
            return;
        }

        this.reconnecting = true;
        try {
            await reconnectToServer();
        } finally {
            this.elapsed = 0;
            this.reconnecting = false;
        }
    }

    /**
     * Runs the countdown while enabled and not paused; meant to be returned from an `$effect`.
     * Pausing keeps the elapsed time.
     */
    run(paused: boolean): (() => void) | undefined {
        if (!this.enabled || paused || this.reconnecting) {
            return;
        }

        let last = performance.now();
        const tick = (now: number) => {
            this.elapsed += now - last;
            last = now;

            if (this.elapsed >= RECONNECT_DELAY) {
                this.reconnect();
            } else {
                frame = requestAnimationFrame(tick);
            }
        };
        let frame = requestAnimationFrame(tick);

        return () => cancelAnimationFrame(frame);
    }
}
