import type {Vec3} from "../integration/types";

export const distanceSq = (pos1: Vec3, pos2: Vec3): number =>
    (pos1.x - pos2.x) ** 2 + (pos1.y - pos2.y) ** 2 + (pos1.z - pos2.z) ** 2;

export const distance = (pos1: Vec3, pos2: Vec3): number =>
    Math.sqrt(distanceSq(pos1, pos2));
