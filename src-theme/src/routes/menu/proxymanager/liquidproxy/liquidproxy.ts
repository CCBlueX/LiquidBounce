import type {LiquidProxySubscription} from "../../../../integration/types";

export function formatDuration(millis: number): string {
    const minutes = Math.round(millis / 60_000);
    if (minutes < 1) {
        return "less than a minute";
    }
    if (minutes < 60) {
        return minutes === 1 ? "1 minute" : `${minutes} minutes`;
    }

    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    const hoursText = hours === 1 ? "1 hour" : `${hours} hours`;
    return rest === 0 ? hoursText : `${hoursText} ${rest} min`;
}

export function formatDate(millis: number): string {
    return new Date(millis).toLocaleDateString(undefined, {day: "numeric", month: "long", year: "numeric"});
}

export function subscriptionStatus(subscription: LiquidProxySubscription): string {
    switch (subscription.state) {
        case "active":
            return "Active";
        case "expired":
            return "Expired";
        default:
            return "Unavailable";
    }
}
