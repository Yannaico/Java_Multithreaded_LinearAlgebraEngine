package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final Object completionLock = new Object();

    public TiredExecutor(int numThreads) {
        if(numThreads <= 0){
            throw new IllegalArgumentException("Number of threads must be positive");
        }
        this.workers = new TiredThread[numThreads];
        for(int i=0;i<numThreads;i++){
            workers[i] = new TiredThread(i, 0.5 + Math.random());
            workers[i].start();
            idleMinHeap.add(workers[i]);
        }
    }
    // Submit a task to be executed by the executor
    // The task will be assigned to the least fatigued idle worker
    public void submit(Runnable task) {
        if(task == null){
            throw new IllegalArgumentException("Task cannot be null");
        }
        try{
            inFlight.incrementAndGet();
            TiredThread worker = idleMinHeap.take();
            // Wrap the task to ensure the worker is returned to the idle heap after execution
              Runnable wrappedTask = () -> {
                try{
                    task.run();
                }
                catch(Exception e){
                    //throw e;
                }finally{
                    inFlight.decrementAndGet();
                    idleMinHeap.add(worker);
                    synchronized (completionLock) {
                        completionLock.notifyAll();
                    }
              }
            };

            worker.newTask(wrappedTask);// Assign the wrapped task to the selected worker
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new RuntimeException("TiredExecutor interrupted while submitting task", e); 
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        for(Runnable task : tasks){
            submit(task);
        }
        synchronized(completionLock){
        while(inFlight.get() > 0)
        {
            try{
                completionLock.wait();
            }
            catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }

    }
}

    public void shutdown() throws InterruptedException {
        for(int i=0;i<workers.length;i++){
            workers[i].shutdown();
        }
        for(TiredThread worker : workers){
            worker.join();
        }
    }

    public synchronized String getWorkerReport() {
        StringBuilder report = new StringBuilder();
        for (TiredThread worker : workers) {
            report.append("Worker ").append(worker.getWorkerId())
                  .append(": Time Used = ").append(worker.getTimeUsed() / 1_000_000).append(" ms, ")
                  .append("Time Idle = ").append(worker.getTimeIdle() / 1_000_000).append(" ms, ")
                  .append("Fatigue = ").append(String.format("%.4f", worker.getFatigue()))
                  .append("\n");
        }
        return report.toString();
    }
}
