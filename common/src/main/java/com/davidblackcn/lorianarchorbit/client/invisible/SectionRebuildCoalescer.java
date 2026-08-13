package com.davidblackcn.lorianarchorbit.client.invisible;

final class SectionRebuildCoalescer {
    private boolean pending;
    private int completedRebuilds;

    void request() {
        pending = true;
    }

    boolean consume() {
        if (!pending) return false;
        pending = false;
        completedRebuilds++;
        return true;
    }

    int completedRebuilds() {
        return completedRebuilds;
    }
}
