package org.ppvon.ultimateCobblemonProgression.client.gui.layout;

import java.util.function.Consumer;

/** A screen with tabs, pages or dialogs, so the {@link UiSweep} checks each of them and not only the first. */
public interface UiSweepable {
    /** Puts the screen in each of its states in turn, calling {@code draw} with the state's name each time. */
    void sweepStates(Consumer<String> draw);
}
