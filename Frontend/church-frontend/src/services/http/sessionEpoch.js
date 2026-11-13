// Late responses from a previous session must never invalidate a newly authenticated account.
let epoch = 0;
export const currentSessionEpoch = () => epoch;
export const advanceSessionEpoch = () => ++epoch;
