package com.mowtiie.flashback.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.mowtiie.flashback.AppExecutors;
import com.mowtiie.flashback.data.FlashbackDatabase;
import com.mowtiie.flashback.data.io.ArchiveRepository;
import com.mowtiie.flashback.data.io.DeckArchive;
import com.mowtiie.flashback.data.dao.CardDao;
import com.mowtiie.flashback.data.dao.DeckDao;
import com.mowtiie.flashback.data.dao.NoteDao;
import com.mowtiie.flashback.data.dao.ReviewLogDao;
import com.mowtiie.flashback.data.dao.TagDao;
import com.mowtiie.flashback.data.entity.Card;
import com.mowtiie.flashback.data.entity.Deck;
import com.mowtiie.flashback.data.entity.DeckTagCrossRef;
import com.mowtiie.flashback.data.entity.Note;
import com.mowtiie.flashback.data.entity.ReviewLog;
import com.mowtiie.flashback.data.entity.Tag;
import com.mowtiie.flashback.data.model.DeckSummary;
import com.mowtiie.flashback.data.model.DeckWithTags;
import com.mowtiie.flashback.data.model.StudyCard;
import com.mowtiie.flashback.data.model.TagWithCount;
import com.mowtiie.flashback.scheduler.Rating;
import com.mowtiie.flashback.scheduler.SchedulingState;
import com.mowtiie.flashback.scheduler.Sm2Scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single entry point to persistence for the ViewModel layer. Reads are exposed
 * as LiveData and run on Room's own threads; writes are posted to the shared
 * disk executor.
 */
public class FlashbackRepository {

    private static volatile FlashbackRepository instance;

    private final FlashbackDatabase db;
    private final DeckDao deckDao;
    private final NoteDao noteDao;
    private final CardDao cardDao;
    private final TagDao tagDao;
    private final ReviewLogDao reviewLogDao;
    private final AppExecutors executors;
    private final Sm2Scheduler scheduler;
    private final ArchiveRepository archiveRepository;

    private FlashbackRepository(FlashbackDatabase db, AppExecutors executors, Sm2Scheduler scheduler) {
        this.db = db;
        this.deckDao = db.deckDao();
        this.noteDao = db.noteDao();
        this.cardDao = db.cardDao();
        this.tagDao = db.tagDao();
        this.reviewLogDao = db.reviewLogDao();
        this.executors = executors;
        this.scheduler = scheduler;
        this.archiveRepository = new ArchiveRepository(db);
    }

    public static FlashbackRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (FlashbackRepository.class) {
                if (instance == null) {
                    instance = new FlashbackRepository(
                            FlashbackDatabase.getInstance(context),
                            AppExecutors.getInstance(),
                            new Sm2Scheduler());
                }
            }
        }
        return instance;
    }

    /** Test seam. */
    public static FlashbackRepository create(FlashbackDatabase db, AppExecutors executors,
                                             Sm2Scheduler scheduler) {
        return new FlashbackRepository(db, executors, scheduler);
    }

    /** Result of an answer, so the UI can offer undo and report the new interval. */
    public static class AnswerResult {

        public final long reviewLogId;

        /**
         * The card's state after the answer. The study session holds its cards
         * in memory, so it needs this to decide whether the card comes back
         * later in the session or is finished for the day.
         */
        public final SchedulingState after;

        AnswerResult(long reviewLogId, SchedulingState after) {
            this.reviewLogId = reviewLogId;
            this.after = after;
        }
    }

    public interface Callback<T> {
        void onResult(T value);
    }

    // ---------------------------------------------------------------- decks

    public LiveData<List<DeckSummary>> observeDeckSummaries(long now) {
        return deckDao.observeSummaries(now);
    }

    public LiveData<Deck> observeDeck(long deckId) {
        return deckDao.observeById(deckId);
    }

    public LiveData<DeckSummary> observeDeckSummary(long deckId, long now) {
        return deckDao.observeSummary(deckId, now);
    }

    public void insertDeck(Deck deck, Callback<Long> callback) {
        executors.diskIO().execute(() -> {
            long id = deckDao.insert(deck);
            postBack(callback, id);
        });
    }

    public void updateDeck(Deck deck) {
        executors.diskIO().execute(() -> deckDao.update(deck));
    }

    public void deleteDeck(Deck deck) {
        executors.diskIO().execute(() -> deckDao.delete(deck));
    }

    // ---------------------------------------------------------------- notes

    public LiveData<List<Note>> observeNotes(long deckId) {
        return noteDao.observeByDeck(deckId);
    }

    /**
     * Inserts a note together with the cards it implies: always a forward card,
     * plus a reverse card when the note asks for one.
     */
    public void addNote(Note note, Callback<Long> callback) {
        executors.diskIO().execute(() -> {
            long id = db.runInTransaction(() -> insertNoteBlocking(note));
            postBack(callback, id);
        });
    }

    @WorkerThread
    public long insertNoteBlocking(Note note) {
        long noteId = noteDao.insert(note);
        List<Card> cards = new ArrayList<>(2);
        cards.add(new Card(noteId, Card.ORDINAL_FORWARD));
        if (note.reverseEnabled) {
            cards.add(new Card(noteId, Card.ORDINAL_REVERSE));
        }
        cardDao.insertAll(cards);
        return noteId;
    }

    /**
     * Saves an edit. Toggling the reverse flag on creates the missing card;
     * toggling it off removes it, discarding that card's scheduling history.
     */
    public void updateNote(Note note) {
        executors.diskIO().execute(() -> db.runInTransaction(() -> {
            note.modifiedAt = System.currentTimeMillis();
            noteDao.update(note);
            boolean hasReverse = false;
            for (Card c : cardDao.getByNote(note.id)) {
                if (c.ordinal == Card.ORDINAL_REVERSE) {
                    hasReverse = true;
                    break;
                }
            }
            if (note.reverseEnabled && !hasReverse) {
                cardDao.insert(new Card(note.id, Card.ORDINAL_REVERSE));
            } else if (!note.reverseEnabled && hasReverse) {
                cardDao.deleteByNoteAndOrdinal(note.id, Card.ORDINAL_REVERSE);
            }
        }));
    }

    public void deleteNote(Note note) {
        executors.diskIO().execute(() -> noteDao.delete(note));
    }

    /** One-shot load for the note editor. Yields null for a new note. */
    public void getNote(long noteId, Callback<Note> callback) {
        executors.diskIO().execute(() -> postBack(callback, noteDao.getById(noteId)));
    }

    /** One-shot load for the deck editor. Yields null for a new deck. */
    public void getDeck(long deckId, Callback<Deck> callback) {
        executors.diskIO().execute(() -> postBack(callback, deckDao.getById(deckId)));
    }

    // ---------------------------------------------------------------- study

    /**
     * Assembles a session queue. Learning cards come first because their
     * intervals are minutes and delaying them defeats the point; new cards are
     * capped by the deck's daily allowance and interleaved after reviews.
     */
    public void buildStudyQueue(long deckId, long now, Callback<List<StudyCard>> callback) {
        executors.diskIO().execute(() -> {
            Deck deck = deckDao.getById(deckId);
            if (deck == null) {
                postBack(callback, Collections.emptyList());
                return;
            }

            int reviewBudget = Math.max(0, deck.reviewsPerDay
                    - reviewLogDao.countSince(startOfTodayMillis(now)));

            List<StudyCard> queue = new ArrayList<>();
            queue.addAll(cardDao.findDueLearning(deckId, now, 50));
            queue.addAll(cardDao.findDueReviews(deckId, now, reviewBudget));
            queue.addAll(cardDao.findNew(deckId, Math.max(0, deck.newPerDay)));

            postBack(callback, queue);
        });
    }

    /**
     * Applies a rating. The card update and its log row are written in one
     * transaction, so an undo can never find a half-written review.
     */
    public void answerCard(long cardId, Rating rating, long elapsedMs, long now,
                           Callback<AnswerResult> callback) {
        executors.diskIO().execute(() -> {
            AnswerResult result = db.runInTransaction(() -> {
                Card card = cardDao.getById(cardId);
                if (card == null) {
                    return null;
                }
                SchedulingState before = card.toSchedulingState();
                SchedulingState after = scheduler.answer(before, rating, now);

                card.applySchedulingState(after);
                cardDao.update(card);

                ReviewLog log = ReviewLog.of(cardId, rating.value, elapsedMs, before, after, now);
                long logId = reviewLogDao.insert(log);

                return new AnswerResult(logId, after);
            });
            postBack(callback, result);
        });
    }

    /**
     * Reverts the most recent answer by writing the logged snapshot back and
     * dropping the log row. Returns the restored card id, or -1 if there was
     * nothing to undo.
     */
    public void undoLastReview(Callback<Long> callback) {
        executors.diskIO().execute(() -> {
            Long cardId = db.runInTransaction(() -> {
                ReviewLog log = reviewLogDao.findMostRecent();
                if (log == null) {
                    return -1L;
                }
                Card card = cardDao.getById(log.cardId);
                if (card != null) {
                    card.applySchedulingState(log.toPreviousState());
                    cardDao.update(card);
                }
                reviewLogDao.delete(log);
                return log.cardId;
            });
            postBack(callback, cardId);
        });
    }

    // ----------------------------------------------------------------- tags

    public LiveData<List<Tag>> observeAllTags() {
        return tagDao.observeAll();
    }

    public LiveData<List<TagWithCount>> observeTagsWithCounts() {
        return tagDao.observeAllWithCounts();
    }

    /** Deck-to-tag mapping for the deck list; counts come from a separate query. */
    public LiveData<List<DeckWithTags>> observeDecksWithTags() {
        return deckDao.observeAllWithTags();
    }

    public void getTag(long tagId, Callback<Tag> callback) {
        executors.diskIO().execute(() -> postBack(callback, tagDao.getById(tagId)));
    }

    public LiveData<List<Tag>> observeTagsForDeck(long deckId) {
        return tagDao.observeTagsForDeck(deckId);
    }

    public void insertTag(Tag tag, Callback<Long> callback) {
        executors.diskIO().execute(() -> postBack(callback, tagDao.insert(tag)));
    }

    public void updateTag(Tag tag) {
        executors.diskIO().execute(() -> tagDao.update(tag));
    }

    public void deleteTag(Tag tag) {
        executors.diskIO().execute(() -> tagDao.delete(tag));
    }

    public void setDeckTags(long deckId, List<Long> tagIds) {
        executors.diskIO().execute(() -> db.runInTransaction(() -> {
            tagDao.clearTagsForDeck(deckId);
            for (Long tagId : tagIds) {
                tagDao.link(new DeckTagCrossRef(deckId, tagId));
            }
        }));
    }

    // ----------------------------------------------------------- statistics

    public LiveData<Integer> observeReviewsSince(long since) {
        return reviewLogDao.observeCountSince(since);
    }

    public LiveData<Long> observeTimeSpentSince(long since) {
        return reviewLogDao.observeTimeSpentSince(since);
    }

    public void countDueEverywhere(long now, Callback<Integer> callback) {
        executors.diskIO().execute(
                () -> postBack(callback, cardDao.countDueEverywhere(now)));
    }

    /** For the reminder worker, which already runs off the main thread. */
    @androidx.annotation.WorkerThread
    public int countDueEverywhereBlocking(long now) {
        return cardDao.countDueEverywhere(now);
    }

    // ------------------------------------------------------- import/export

    public void exportAll(Callback<DeckArchive> callback) {
        executors.diskIO().execute(() -> postBack(callback, archiveRepository.exportAll()));
    }

    public void exportDeck(long deckId, Callback<DeckArchive> callback) {
        executors.diskIO().execute(
                () -> postBack(callback, archiveRepository.exportSingle(deckId)));
    }

    public void importArchive(DeckArchive archive,
                              Callback<ArchiveRepository.ImportResult> callback) {
        executors.diskIO().execute(
                () -> postBack(callback, archiveRepository.importArchive(archive)));
    }

    // ---------------------------------------------------------------- utils

    private <T> void postBack(@NonNull Callback<T> callback, T value) {
        executors.mainThread().execute(() -> callback.onResult(value));
    }

    private long startOfTodayMillis(long now) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(java.util.Calendar.HOUR_OF_DAY, scheduler.getConfig().rolloverHour);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() > now) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        }
        return cal.getTimeInMillis();
    }
}
