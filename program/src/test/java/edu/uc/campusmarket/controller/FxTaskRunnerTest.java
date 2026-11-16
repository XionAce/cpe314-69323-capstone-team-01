package edu.uc.campusmarket.controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxTaskRunnerTest {
    private final Worker worker = new Worker();
    private final Deque<Runnable> ui = new ArrayDeque<>();
    private final FxTaskRunner tasks = new FxTaskRunner(worker, ui::addLast);

    @Test
    void userActionQueuesBehindAQuietReadWithoutBeingDropped() {
        List<String> results = new ArrayList<>();
        assertTrue(tasks.run(() -> "refresh", results::add, true));
        assertFalse(tasks.busyProperty().get());
        assertTrue(tasks.run(() -> "search", results::add));
        assertTrue(tasks.busyProperty().get());
        worker.next();
        assertTrue(results.isEmpty(), "Worker must not publish results directly.");
        ui.removeFirst().run();
        assertEquals(List.of("refresh"), results);
        assertTrue(tasks.busyProperty().get(), "Quiet completion must not clear user busy state.");
        worker.next();
        ui.removeFirst().run();
        assertEquals(List.of("refresh", "search"), results);
        assertFalse(tasks.busyProperty().get());
    }

    @Test
    void repeatedTicksDoNotQueueMultipleQuietReads() {
        assertTrue(tasks.run(() -> 1, value -> {
        }, true));
        assertFalse(tasks.run(() -> 2, value -> {
        }, true));
        worker.next();
        assertFalse(tasks.run(() -> 3, value -> {
        }, true));
        ui.removeFirst().run();
        assertTrue(tasks.run(() -> 4, value -> {
        }, true));
        assertEquals(1, worker.queue.size());
    }

    @Test
    void quietFailureDoesNotOverwriteANewerUserActionsStatus() {
        tasks.run(() -> {
            throw new IllegalArgumentException("Old refresh failed");
        }, value -> {
        }, true);
        tasks.run(() -> "saved", value -> {
        });
        worker.next();
        ui.removeFirst().run();
        assertTrue(tasks.busyProperty().get());
        assertEquals("Working...", tasks.statusProperty().get());
        assertEquals("", tasks.errorProperty().get());
        worker.next();
        ui.removeFirst().run();
        assertFalse(tasks.busyProperty().get());
        assertEquals("Ready.", tasks.statusProperty().get());
    }

    @Test
    void failedUserActionReleasesBusyStateAndKeepsItsClearMessage() {
        tasks.run(() -> {
            throw new IllegalArgumentException("Enter a valid price.");
        }, value -> {
        });
        worker.next();
        assertTrue(tasks.busyProperty().get());
        ui.removeFirst().run();
        assertFalse(tasks.busyProperty().get());
        assertEquals("Enter a valid price.", tasks.errorProperty().get());
    }

    @Test
    void rejectedExecutorDoesNotLeaveTheAppBusy() {
        worker.reject = true;
        assertFalse(tasks.run(() -> 1, value -> {
        }));
        assertFalse(tasks.busyProperty().get());
        assertEquals("The application is closing.", tasks.errorProperty().get());
    }

    @Test
    void quietSuccessPreservesTheLastValidationError() {
        tasks.run(() -> {
            throw new IllegalArgumentException("Correct the title.");
        }, value -> {
        });
        worker.next();
        ui.removeFirst().run();
        tasks.run(() -> 1, value -> {
        }, true);
        worker.next();
        ui.removeFirst().run();
        assertEquals("Correct the title.", tasks.errorProperty().get());
    }

    private static final class Worker extends AbstractExecutorService {
        final Deque<Runnable> queue = new ArrayDeque<>();
        boolean stopped;
        boolean reject;

        void next() {
            queue.removeFirst().run();
        }

        public void execute(Runnable task) {
            if (stopped || reject) {
                throw new RejectedExecutionException();
            }
            queue.addLast(task);
        }

        public void shutdown() {
            stopped = true;
        }

        public List<Runnable> shutdownNow() {
            stopped = true;
            return List.copyOf(queue);
        }

        public boolean isShutdown() {
            return stopped;
        }

        public boolean isTerminated() {
            return stopped && queue.isEmpty();
        }

        public boolean awaitTermination(long time, TimeUnit unit) {
            return isTerminated();
        }
    }
}
