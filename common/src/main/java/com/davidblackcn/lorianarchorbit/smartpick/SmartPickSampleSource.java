package com.davidblackcn.lorianarchorbit.smartpick;

@FunctionalInterface
public interface SmartPickSampleSource<T> {
    SmartPickSample<T> sample(int offsetX, int offsetY, int offsetZ);
}
