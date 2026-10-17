package com.davidblackcn.lorianarchorbit.reach;

public enum ReachDecision {
    ACCEPTED,
    DISABLED,
    INCOMPATIBLE,
    NOT_CREATIVE,
    NO_PERMISSION,
    RATE_LIMITED;

    public static ReachDecision byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : INCOMPATIBLE;
    }
}
