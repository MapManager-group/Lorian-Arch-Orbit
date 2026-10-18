package com.davidblackcn.lorianarchorbit.palette.hueblocks;

/** Generation tokens prevent obsolete reloads from publishing results or failures. */
public final class LocalHueState<T> {
    private long generation;
    private T value;
    private boolean checking, failed;
    public synchronized long begin(boolean resourcesChanged) {
        generation++; checking = true; failed = false;
        if (resourcesChanged) value = null;
        return generation;
    }
    public synchronized boolean publish(long token, T replacement) {
        if (token != generation) return false;
        value = replacement; checking = false; failed = false; return true;
    }
    public synchronized void fail(long token) {
        if (token == generation) { checking = false; failed = true; }
    }
    public synchronized T value() { return value; }
    public synchronized boolean checking() { return checking; }
    public synchronized boolean failed() { return failed; }
}
