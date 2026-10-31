package com.mowtiie.flashback.scheduler

class SchedulerConfig {
    var learningStepsMinutes: IntArray = intArrayOf(1, 10)
    var relearningStepsMinutes: IntArray = intArrayOf(10)
    var graduatingIntervalDays: Int = 1
    var easyIntervalDays: Int = 4
    var lapseIntervalMultiplier: Double = 0.0
    var minimumIntervalDays: Int = 1
    var maximumIntervalDays: Int = 36500
    var minimumEase: Double = 1.3
    var hardIntervalMultiplier: Double = 1.2
    var easyBonus: Double = 1.3
    var againEaseDelta: Double = -0.20
    var hardEaseDelta: Double = -0.15
    var easyEaseDelta: Double = 0.15

    /** Spreads due dates so cards added together don't all return on one day. */
    var fuzzEnabled: Boolean = true
    var rolloverHour: Int = 4
}
