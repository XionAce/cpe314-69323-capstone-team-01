package edu.uc.campusmarket.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Starts a single JavaFX timer and always stops its previous schedule first.
 */
public class FxRefreshSchedule implements RefreshSchedule {
    private Timeline timer;

    /** Provides the start operation for this component. */
    @Override
    public void start(Runnable tick) {
        stop();
        timer = new Timeline(new KeyFrame(Duration.seconds(RefreshController.REFRESH_SECONDS),
                event -> tick.run()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.playFromStart();
    }

    /** Stops the component or worker without starting further work. */
    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }
}
