package com.davidblackcn.lorianarchorbit.interaction;

public interface GestureRegistration extends AutoCloseable {
    String ownerId();

    boolean active();

    @Override
    void close();
}
