package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonObject;

import java.util.Set;

public interface ConfigCodec<T> {
    T defaults();

    DecodedConfig<T> decode(JsonObject root);

    JsonObject encode(T snapshot);

    Set<String> changedNamespaces(T previous, T next);
}
