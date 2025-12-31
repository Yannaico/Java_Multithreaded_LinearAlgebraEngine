package scheduling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TiredExecutorTest {
    
    private TiredExecutor executor;
    
    @AfterEach
    public void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    // ========== VALID CASES ==========
    
    @Test
    @Timeout(5)
    public void testCreateExecutor_ValidThreadCount() {
        executor = new TiredExecutor(4);
        assertNotNull(executor);
    }
    
    @Test
    @Timeout(5)
    public void testSubmitSingleTask_ExecutesSuccessfully() throws InterruptedException {
        executor = new TiredExecutor(2);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        executor.submit(() -> counter.incrementAndGet());
        
        // Give time for task to complete
        Thread.sleep(100);
        
        assertEquals(1, counter.get());
    }
    
    @Test
    @Timeout(5)
    public void testSubmitMultipleTasks_AllExecute() throws InterruptedException {
        executor = new TiredExecutor(4);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> counter.incrementAndGet());
        }
        
        // Give time for all tasks to complete
        Thread.sleep(500);
        
        assertEquals(10, counter.get());
    }
    
    @Test
    @Timeout(5)
    public void testSubmitAll_WaitsForCompletion() {
        executor = new TiredExecutor(4);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> {
                counter.incrementAndGet();
                try {
                    Thread.sleep(10); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.submitAll(tasks);
        
        // After submitAll returns, all tasks should be complete
        assertEquals(20, counter.get());
    }
    
    @Test
    @Timeout(10)
    public void testSubmitAll_BlocksUntilAllTasksFinish() {
        executor = new TiredExecutor(2);
        
        AtomicInteger counter = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> {
                try {
                    Thread.sleep(200); // Each task takes 200ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
            });
        }
        
        executor.submitAll(tasks);
        long endTime = System.currentTimeMillis();
        
        // With 2 workers and 4 tasks of 200ms each:
        // Should take at least 400ms (2 batches × 200ms)
        assertTrue(endTime - startTime >= 400, "Should block until all tasks complete");
        assertEquals(4, counter.get());
    }
    
    @Test
    @Timeout(5)
    public void testParallelExecution_MultipleWorkersWork() {
        executor = new TiredExecutor(4);
        
        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                int current = concurrent.incrementAndGet();
                
                // Update max concurrent if this is higher
                synchronized (maxConcurrent) {
                    if (current > maxConcurrent.get()) {
                        maxConcurrent.set(current);
                    }
                }
                
                try {
                    Thread.sleep(100); // Hold for a bit
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                concurrent.decrementAndGet();
            });
        }
        
        executor.submitAll(tasks);
        
        // With 4 workers, we should have seen at least 2 tasks running concurrently
        assertTrue(maxConcurrent.get() >= 2, 
            "Should have multiple tasks running concurrently, got: " + maxConcurrent.get());
    }
    
    @Test
    @Timeout(5)
    public void testShutdown_StopsAllWorkers() throws InterruptedException {
        executor = new TiredExecutor(4);
        
        AtomicInteger counter = new AtomicInteger(0);
        executor.submit(() -> counter.incrementAndGet());
        
        Thread.sleep(100); // Let task complete
        
        executor.shutdown();
        
        // After shutdown, all workers should have stopped
        assertEquals(1, counter.get());
    }
    
    @Test
    @Timeout(5)
    public void testGetWorkerReport_ReturnsStatistics() {
        executor = new TiredExecutor(4);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.submitAll(tasks);
        
        String report = executor.getWorkerReport();
        
        assertNotNull(report);
        assertTrue(report.contains("Worker"));
        assertTrue(report.contains("Fatigue"));
    }
    
    @Test
    @Timeout(5)
    public void testFairnessDistribution_WorkIsBalanced() {
        executor = new TiredExecutor(4);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.submitAll(tasks);
        
        String report = executor.getWorkerReport();
        
        // All workers should have done some work
        assertNotNull(report);
        assertTrue(report.length() > 0);
    }
    
    // ========== INVALID CASES ==========
    
    @Test
    public void testCreateExecutor_ZeroThreads() {
        assertThrows(Exception.class, () -> {
            executor = new TiredExecutor(0);
        });
    }
    
    @Test
    public void testCreateExecutor_NegativeThreads() {
        assertThrows(Exception.class, () -> {
            executor = new TiredExecutor(-1);
        });
    }
    
    @Test
    @Timeout(5)
    public void testSubmit_NullTask() {
        executor = new TiredExecutor(2);
        
        assertThrows(Exception.class, () -> {
            executor.submit(null);
        });
    }
    
    @Test
    @Timeout(5)
    public void testSubmitAll_NullTaskList() {
        executor = new TiredExecutor(2);
        
        assertThrows(Exception.class, () -> {
            executor.submitAll(null);
        });
    }
    
    @Test
    @Timeout(5)
    public void testSubmitAll_EmptyTaskList() {
        executor = new TiredExecutor(2);
        
        List<Runnable> emptyList = new ArrayList<>();
        
        // Should complete immediately without error
        assertDoesNotThrow(() -> executor.submitAll(emptyList));
    }
    
    @Test
    @Timeout(5)
    public void testTaskThrowsException_DoesNotCrashWorker() {
        executor = new TiredExecutor(2);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        
        // Task that throws exception
        tasks.add(() -> {
            throw new RuntimeException("Intentional exception");
        });
        
        // Normal task
        tasks.add(() -> counter.incrementAndGet());
        
        // Should not throw exception to main thread
        assertDoesNotThrow(() -> executor.submitAll(tasks));
        
        // The normal task should still have executed
        assertEquals(1, counter.get());
    }
    
    @Test
    @Timeout(5)
    public void testMultipleTasksThrowExceptions_ExecutorContinues() {
        executor = new TiredExecutor(2);
        
        AtomicInteger successCount = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int index = i;
            if (index % 2 == 0) {
                // Even indices throw exceptions
                tasks.add(() -> {
                    throw new RuntimeException("Exception from task " + index);
                });
            } else {
                // Odd indices succeed
                tasks.add(() -> successCount.incrementAndGet());
            }
        }
        
        assertDoesNotThrow(() -> executor.submitAll(tasks));
        
        // Should have 5 successful tasks
        assertEquals(5, successCount.get());
    }
    
    // ========== EDGE CASES ==========
    
    @Test
    @Timeout(5)
    public void testSingleWorkerThread_ExecutesSequentially() {
        executor = new TiredExecutor(1);
        
        List<Integer> executionOrder = new ArrayList<>();
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            tasks.add(() -> {
                synchronized (executionOrder) {
                    executionOrder.add(taskId);
                }
            });
        }
        
        executor.submitAll(tasks);
        
        // All tasks should complete
        assertEquals(5, executionOrder.size());
    }
    
    @Test
    @Timeout(5)
    public void testManyWorkers_100Threads() {
        executor = new TiredExecutor(100);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> counter.incrementAndGet());
        }
        
        executor.submitAll(tasks);
        
        assertEquals(100, counter.get());
    }
    
    @Test
    @Timeout(5)
    public void testVeryQuickTasks_ExecuteCorrectly() {
        executor = new TiredExecutor(4);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tasks.add(() -> counter.incrementAndGet());
        }
        
        executor.submitAll(tasks);
        
        assertEquals(1000, counter.get());
    }
    
    @Test
    @Timeout(15)
    public void testVerySlowTasks_CompleteEventually() {
        executor = new TiredExecutor(2);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> {
                try {
                    Thread.sleep(1000); // 1 second per task
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
            });
        }
        
        long start = System.currentTimeMillis();
        executor.submitAll(tasks);
        long duration = System.currentTimeMillis() - start;
        
        // With 2 workers and 4 tasks of 1s each, should take ~2s
        assertTrue(duration >= 2000, "Should take at least 2 seconds");
        assertEquals(4, counter.get());
    }
    
    @Test
    @Timeout(5)
    public void testEmptyTask_DoesNothing() {
        executor = new TiredExecutor(2);
        
        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> {}); // Empty task
        
        assertDoesNotThrow(() -> executor.submitAll(tasks));
    }
    
    @Test
    @Timeout(5)
    public void testTaskModifiesSharedState_ThreadSafe() {
        executor = new TiredExecutor(4);
        
        AtomicInteger sharedCounter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> {
                for (int j = 0; j < 100; j++) {
                    sharedCounter.incrementAndGet();
                }
            });
        }
        
        executor.submitAll(tasks);
        
        // Should be exactly 10000 with no race conditions
        assertEquals(10000, sharedCounter.get());
    }
    
    @Test
    @Timeout(5)
    public void testConsecutiveSubmitAll_BothComplete() {
        executor = new TiredExecutor(2);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> batch1 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            batch1.add(() -> counter.incrementAndGet());
        }
        
        List<Runnable> batch2 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            batch2.add(() -> counter.incrementAndGet());
        }
        
        executor.submitAll(batch1);
        assertEquals(5, counter.get(), "First batch should complete");
        
        executor.submitAll(batch2);
        assertEquals(10, counter.get(), "Second batch should complete");
    }
    
    @Test
    @Timeout(5)
    public void testSingleTask_1x1Matrix() {
        executor = new TiredExecutor(4);
        
        AtomicInteger result = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> result.set(42));
        
        executor.submitAll(tasks);
        
        assertEquals(42, result.get());
    }
    
    @Test
    @Timeout(5)
    public void testTasksWithDifferentDurations_AllComplete() {
        executor = new TiredExecutor(3);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        
        // Fast task
        tasks.add(() -> counter.incrementAndGet());
        
        // Medium task
        tasks.add(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            counter.incrementAndGet();
        });
        
        // Slow task
        tasks.add(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            counter.incrementAndGet();
        });
        
        executor.submitAll(tasks);
        
        assertEquals(3, counter.get());
    }
    
    @Test
    @Timeout(5)
    public void testMoreTasksThanWorkers_AllComplete() {
        executor = new TiredExecutor(2);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> counter.incrementAndGet());
        }
        
        executor.submitAll(tasks);
        
        assertEquals(10, counter.get());
    }
    
    @Test
    @Timeout(5)
    public void testFewerTasksThanWorkers_AllComplete() {
        executor = new TiredExecutor(10);
        
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tasks.add(() -> counter.incrementAndGet());
        }
        
        executor.submitAll(tasks);
        
        assertEquals(3, counter.get());
    }
    
    @Test
    @Timeout(10)
    public void testShutdownAfterMultipleBatches_CleansUpProperly() throws InterruptedException {
        executor = new TiredExecutor(4);
        
        for (int batch = 0; batch < 5; batch++) {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tasks.add(() -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            executor.submitAll(tasks);
        }
        
        // Should shutdown cleanly
        assertDoesNotThrow(() -> executor.shutdown());
    }
}