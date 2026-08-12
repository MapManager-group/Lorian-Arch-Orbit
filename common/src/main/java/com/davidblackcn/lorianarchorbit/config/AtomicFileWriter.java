package com.davidblackcn.lorianarchorbit.config;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface AtomicFileWriter {
    void write(Path target, byte[] content) throws IOException;
}
