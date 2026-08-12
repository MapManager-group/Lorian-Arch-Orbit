package com.davidblackcn.lorianarchorbit.smartpick;

import com.davidblackcn.lorianarchorbit.interaction.InputGesture;

import java.util.List;

public final class SmartPickGestureState {
    private boolean smartArmed;
    private boolean smartActive;

    public synchronized void armForVanillaPick() {
        smartArmed = true;
    }

    public synchronized void smartOpened(boolean opened) {
        smartArmed = false;
        smartActive = opened;
    }

    public synchronized List<Action> accept(InputGesture gesture) {
        return switch (gesture) {
            case LONG_PRESSED -> smartArmed ? List.of(Action.OPEN_SMART_PICK) : List.of();
            case SHORT_PRESSED -> {
                clear();
                yield List.of();
            }
            case RELEASED -> release();
            case CANCELLED -> {
                clear();
                yield List.of(Action.CANCEL);
            }
            case PRESSED, DOUBLE_PRESSED, HELD -> List.of();
        };
    }

    public synchronized void clear() {
        smartArmed = false;
        smartActive = false;
    }

    private List<Action> release() {
        if (smartActive) {
            clear();
            return List.of(Action.CONFIRM_SMART_PICK);
        }
        clear();
        return List.of();
    }

    public enum Action {
        OPEN_SMART_PICK,
        CONFIRM_SMART_PICK,
        CANCEL
    }
}
