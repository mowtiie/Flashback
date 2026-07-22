package com.mowtiie.flashback.ui.stats;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.flashback.data.model.DailyCount;
import com.mowtiie.flashback.repository.FlashbackRepository;
import com.mowtiie.flashback.util.StreakCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class StatisticsViewModel extends AndroidViewModel {

    private static final int CHART_DAYS = 14;
    private static final int RETENTION_WINDOW_DAYS = 30;

    private final FlashbackRepository repository;
    private final MutableLiveData<StatisticsData> data = new MutableLiveData<>();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        this.repository = FlashbackRepository.getInstance(application);
        load();
    }

    public LiveData<StatisticsData> getData() {
        return data;
    }

    /** Reloads on every resume, so a study session's reviews show immediately. */
    public void load() {
        repository.runOnDiskThread(() -> {
            StatisticsData snapshot = assemble();
            repository.postToMain(() -> data.setValue(snapshot));
        });
    }

    private StatisticsData assemble() {
        long now = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();

        long startToday = StatisticsCalendar.startOfToday(now, zone);
        long startWeek = StatisticsCalendar.startOfDaysAgo(now, 6, zone);
        long startRetention = StatisticsCalendar.startOfDaysAgo(now, RETENTION_WINDOW_DAYS, zone);
        long startChart = StatisticsCalendar.startOfDaysAgo(now, CHART_DAYS - 1, zone);

        StatisticsData result = new StatisticsData();
        result.reviewsToday = repository.reviewsSinceBlocking(startToday);
        result.reviewsThisWeek = repository.reviewsSinceBlocking(startWeek);
        result.totalReviews = repository.totalReviewsBlocking();
        result.timeTodayMillis = repository.timeSpentSinceBlocking(startToday);

        int windowReviews = repository.reviewsSinceBlocking(startRetention);
        if (windowReviews > 0) {
            int correct = repository.correctSinceBlocking(startRetention);
            result.retentionPercent = Math.round(correct * 100f / windowReviews);
        } else {
            result.retentionPercent = -1;
        }

        List<String> studyDays = repository.recentStudyDaysBlocking();
        result.streakDays = StreakCalculator.compute(studyDays,
                StatisticsCalendar.todayKey(now, zone));

        result.breakdown = repository.cardStateBreakdownBlocking();

        buildChart(result, now, zone, startChart);
        return result;
    }

    /** Fills fixed day slots so quiet days show as gaps, not missing bars. */
    private void buildChart(StatisticsData result, long now, TimeZone zone, long since) {
        Map<String, Integer> byDay = new HashMap<>();
        for (DailyCount count : repository.dailyCountsSinceBlocking(since)) {
            byDay.put(count.day, count.count);
        }

        int[] values = new int[CHART_DAYS];
        String[] labels = new String[CHART_DAYS];
        for (int i = 0; i < CHART_DAYS; i++) {
            int offset = CHART_DAYS - 1 - i;
            String key = StatisticsCalendar.dayKey(now, offset, zone);
            Integer value = byDay.get(key);
            values[i] = value == null ? 0 : value;
            // Label only the ends and every third bar, so 14 numbers do not
            // collide on a phone width.
            labels[i] = (i == 0 || i == CHART_DAYS - 1 || offset % 3 == 0)
                    ? StatisticsCalendar.dayLabel(now, offset, zone) : "";
        }
        result.dailyCounts = values;
        result.dailyLabels = labels;
    }
}
