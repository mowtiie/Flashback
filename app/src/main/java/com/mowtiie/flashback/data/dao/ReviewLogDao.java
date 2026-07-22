package com.mowtiie.flashback.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.mowtiie.flashback.data.entity.ReviewLog;
import com.mowtiie.flashback.data.model.DailyCount;

import java.util.List;

@Dao
public interface ReviewLogDao {

    @Insert
    long insert(ReviewLog log);

    @Delete
    void delete(ReviewLog log);

    /** The undo target: most recent answer across the whole collection. */
    @Query("SELECT * FROM review_log ORDER BY reviewedAt DESC, id DESC LIMIT 1")
    ReviewLog findMostRecent();

    @Query("SELECT COUNT(*) FROM review_log WHERE reviewedAt >= :since")
    LiveData<Integer> observeCountSince(long since);

    @Query("SELECT COUNT(*) FROM review_log WHERE reviewedAt >= :since")
    int countSince(long since);

    @Query("SELECT COALESCE(SUM(elapsedMs), 0) FROM review_log WHERE reviewedAt >= :since")
    LiveData<Long> observeTimeSpentSince(long since);

    /** Share of answers that were not AGAIN, i.e. retention. */
    @Query("SELECT COUNT(*) FROM review_log WHERE reviewedAt >= :since AND rating > 1")
    int countCorrectSince(long since);

    /** Reviews per day for the activity chart. */
    @Query("SELECT date(reviewedAt / 1000, 'unixepoch', 'localtime') AS day, "
            + "COUNT(*) AS count FROM review_log "
            + "WHERE reviewedAt >= :since GROUP BY day ORDER BY day")
    LiveData<List<DailyCount>> observeDailyCounts(long since);

    /** Distinct study days, newest first, for the streak calculation. */
    @Query("SELECT DISTINCT date(reviewedAt / 1000, 'unixepoch', 'localtime') AS day "
            + "FROM review_log ORDER BY day DESC LIMIT 400")
    List<String> recentStudyDays();
}
