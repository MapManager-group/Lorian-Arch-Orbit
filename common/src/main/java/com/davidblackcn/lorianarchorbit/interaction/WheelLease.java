package com.davidblackcn.lorianarchorbit.interaction;

public interface WheelLease extends AutoCloseable {
    String ownerId();

    boolean active();

    @Override
    void close();
}
