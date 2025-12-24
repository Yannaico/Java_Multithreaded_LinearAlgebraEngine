package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        this.workers = new TiredThread[numThreads];
        for(int i=0;i<numThreads;i++){
            workers[i] = new TiredThread(i, 0.5 + Math.random());
            workers[i].start();
            idleMinHeap.add(workers[i]);
        }
    }

    public void submit(Runnable task) {
        if(idleMinHeap.isEmpty())
            throw new IllegalStateException();

        try{
            inFlight.incrementAndGet();
            TiredThread worker = idleMinHeap.take();
            
              Runnable wrappedTask = () -> {
                try{
                    task.run();
                }finally{
                    inFlight.decrementAndGet();
                    idleMinHeap.add(worker);
                }
              };
              worker.newTask(wrappedTask);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        for(Runnable task : tasks){
            submit(task);
        }
    }

    public void shutdown() throws InterruptedException {
        for(int i=0;i<workers.length;i++){
            workers[i].shutdown();
        }
    }

    public synchronized String getWorkerReport() {
        
    }
}
