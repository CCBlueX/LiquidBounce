import {getHashParams} from './util';

const hashParams = getHashParams();
const portParam = hashParams.get('port');
export const isStatic = hashParams.has('static');

export const REST_BASE = portParam
    ? `http://localhost:${portParam}`
    : window.location.origin;

export const WS_BASE = portParam
    ? `ws://localhost:${portParam}`
    : `ws://${window.location.host}`;
