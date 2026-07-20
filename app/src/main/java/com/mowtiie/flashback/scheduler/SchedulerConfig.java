package com.mowtiie.flashback.scheduler;

public class SchedulerConfig {

    public int[] learningStepsMinutes = {1, 10};

    public int[] relearningStepsMinutes = {10};

    public int graduatingIntervalDays = 1;

    public int easyIntervalDays = 4;

    public double lapseIntervalMultiplier = 0.0d;

    public int minimumIntervalDays = 1;

    public int maximumIntervalDays = 36500;

    public double startingEase = 2.5d;

    public double minimumEase = 1.3d;

    public double hardIntervalMultiplier = 1.2d;

    public double easyBonus = 1.3d;

    public double againEaseDelta = -0.20d;
    public double hardEaseDelta = -0.15d;
    public double easyEaseDelta = 0.15d;

    public boolean fuzzEnabled = true;

    public int rolloverHour = 4;
}
