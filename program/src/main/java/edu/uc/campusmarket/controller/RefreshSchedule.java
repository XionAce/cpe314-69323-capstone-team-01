package edu.uc.campusmarket.controller;

/** A replaceable clock: production uses JavaFX; tests drive ticks directly. */
public interface RefreshSchedule {
    void start(Runnable tick);

    void stop();
}
