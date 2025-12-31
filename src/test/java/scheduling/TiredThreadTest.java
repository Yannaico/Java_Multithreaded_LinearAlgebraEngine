package scheduling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredThreadTest {
    
    private TiredThread thread;
    
    @AfterEach
    public void tearDown() throws InterruptedException {
        if (thread != null && thread.isAlive()) {
            thread.shutdown();
            thread.join(1000);
        }
    }
    
    // ========== VALID CASES ==========
    
    @Test
    public void testConstructor_ValidParameters() {
        thread = new TiredThread(0, 1.0);
        assertNotNull(thread);
        assertEquals(0, thread.getWorkerId());
        assertEquals(0.0, thread.getFatigue(), 0.001);
    }
    
    @Test
    public void testGetWorkerId_ReturnsCorrectId() {
        thread = new TiredThread(5, 1.0);
        assertEquals(5, thread.getWorkerId());
    }
    
    @Test
    public void testGetFatigue_InitiallyZero() {
        thread = new TiredThread(0, 1.0);
        assertEquals(0.0, thread.getFatigue(), 0.001);
    }
    
    @Test
    public void testIsBusy_InitiallyFalse() {
        thread = new TiredThread(0, 1.0);
        thread.start();
        assertFalse(thread.isBusy());
    }
    
    @Test
    public void testGetTimeUsed_InitiallyZero() {
        thread = new TiredThread(0, 1.0);
        assertEquals(0L, thread.getTimeUsed());
    }
    
    @Test
    public void testGetTimeIdle_InitiallyZero() {
        thread = new TiredThread(0, 1.0);
        assertEquals(0L, thread.getTimeIdle());
    }
    
    @Test
    public void testNewTask_ExecutesTask() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        
        AtomicBoolean executed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        
        Runnable task = () -> {
            executed.set(true);
            latch.countDown();
        };
        
        thread.newTask(task);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(executed.get());
    }
    
    @Test
    public void testNewTask_UpdatesTimeUsed() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Runnable task = () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        };
        
        thread.newTask(task);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertTrue(thread.getTimeUsed() > 0, "TimeUsed should be greater than 0");
    }
    
    @Test
    public void testNewTask_UpdatesFatigue() throws InterruptedException {
        thread = new TiredThread(0, 1.5);
        thread.start();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Runnable task = () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        };
        
        thread.newTask(task);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertTrue(thread.getFatigue() > 0, "Fatigue should increase after task execution");
    }
    
    @Test
    public void testNewTask_SetsBusyDuringExecution() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);
        AtomicBoolean wasBusy = new AtomicBoolean(false);
        
        Runnable task = () -> {
            startLatch.countDown();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            endLatch.countDown();
        };
        
        thread.newTask(task);
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));
        
        Thread.sleep(20);
        wasBusy.set(thread.isBusy());
        
        assertTrue(endLatch.await(2, TimeUnit.SECONDS));
        assertTrue(wasBusy.get(), "Thread should be busy during task execution");
    }
    
    @Test
    public void testNewTask_NotBusyAfterCompletion() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Runnable task = () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        };
        
        thread.newTask(task);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        assertFalse(thread.isBusy(), "Thread should not be busy after task completion");
    }
    
    @Test
    public void testShutdown_StopsThread() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        
        assertTrue(thread.isAlive());
        thread.shutdown();
        thread.join(1000);
        assertFalse(thread.isAlive());
    }
    
    @Test
    public void testCompareTo_LowerFatigueFirst() throws InterruptedException {
        TiredThread thread1 = new TiredThread(0, 1.0);
        TiredThread thread2 = new TiredThread(1, 1.0);
        
        thread1.start();
        thread2.start();
        
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        
        thread1.newTask(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch1.countDown();
        });
        
        thread2.newTask(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch2.countDown();
        });
        
        assertTrue(latch1.await(2, TimeUnit.SECONDS));
        assertTrue(latch2.await(2, TimeUnit.SECONDS));
        
        Thread.sleep(100);
        
        assertTrue(thread1.compareTo(thread2) > 0, "Thread1 should have higher fatigue");
        
        thread1.shutdown();
        thread2.shutdown();
        thread1.join(1000);
        thread2.join(1000);
    }
    
    @Test
    public void testCompareTo_EqualFatigue() {
        TiredThread thread1 = new TiredThread(0, 1.0);
        TiredThread thread2 = new TiredThread(1, 1.0);
        
        assertEquals(0, thread1.compareTo(thread2));
    }
    
    // ========== INVALID CASES ==========
    
    @Test
    public void testNewTask_WhenNotStarted_ThrowsException() {
        thread = new TiredThread(0, 1.0);
        
        Runnable task = () -> {};
        
        assertThrows(IllegalStateException.class, () -> {
            thread.newTask(task);
            Thread.sleep(100);
            thread.newTask(task);
        });
    }
    
    @Test
    public void testNewTask_AfterShutdown_ThrowsException() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        thread.shutdown();
        thread.join(1000);
        
        Runnable task = () -> {};
        
        assertThrows(IllegalStateException.class, () -> thread.newTask(task));
    }
    
    @Test
    public void testNewTask_WhileBusy_ThrowsException() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);
        
        Runnable longTask = () -> {
            startLatch.countDown();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            endLatch.countDown();
        };
        
        thread.newTask(longTask);
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));
        
        Runnable secondTask = () -> {};
        assertThrows(IllegalStateException.class, () -> thread.newTask(secondTask));
        
        assertTrue(endLatch.await(2, TimeUnit.SECONDS));
    }
    
    // ========== EDGE CASES ==========
    
    @Test
    public void testMultipleTasks_Sequential() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);
        
        thread.newTask(() -> {
            counter.incrementAndGet();
            latch1.countDown();
        });
        
        assertTrue(latch1.await(1, TimeUnit.SECONDS));
        
        thread.newTask(() -> {
            counter.incrementAndGet();
            latch2.countDown();
        });
        
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        
        thread.newTask(() -> {
            counter.incrementAndGet();
            latch3.countDown();
        });
        
        assertTrue(latch3.await(1, TimeUnit.SECONDS));
        
        assertEquals(3, counter.get());
    }
    
    @Test
    public void testFatigueFactor_LowerValue() throws InterruptedException {
        thread = new TiredThread(0, 0.5);
        thread.start();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        thread.newTask(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        
        double fatigue = thread.getFatigue();
        assertTrue(fatigue > 0, "Fatigue should be positive");
        assertTrue(fatigue < thread.getTimeUsed(), "Fatigue should be less than time used due to low factor");
    }
    
    @Test
    public void testFatigueFactor_HigherValue() throws InterruptedException {
        thread = new TiredThread(0, 1.5);
        thread.start();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        thread.newTask(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        
        double fatigue = thread.getFatigue();
        assertTrue(fatigue > 0, "Fatigue should be positive");
        assertTrue(fatigue > thread.getTimeUsed(), "Fatigue should be greater than time used due to high factor");
    }
    
    @Test
    public void testTaskWithException_ContinuesRunning() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        
        thread.newTask(() -> {
            latch1.countDown();
            throw new RuntimeException("Test exception");
        });
        
        assertTrue(latch1.await(1, TimeUnit.SECONDS));
        Thread.sleep(100);
        
        thread.newTask(() -> {
            latch2.countDown();
        });
        
        assertTrue(latch2.await(1, TimeUnit.SECONDS), "Thread should continue after exception");
    }
    
    @Test
    public void testTimeIdle_IncreasesWhenIdle() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        Runnable task = () ->{

            try{
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        };

        thread.newTask(task);
        Thread.sleep(2000); // Wait to ensure thread goes idle
        
        long idleTime = thread.getTimeIdle();
        assertTrue(idleTime > 0, "Idle time should increase when thread is waiting");
    }
    
    @Test
    public void testZeroLengthTask_ExecutesQuickly() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        
        AtomicBoolean executed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        
        thread.newTask(() -> {
            executed.set(true);
            latch.countDown();
        });
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(executed.get());
        assertTrue(thread.getTimeUsed() >= 0);
    }
}
