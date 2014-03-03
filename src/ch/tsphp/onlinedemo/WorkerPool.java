package ch.tsphp.onlinedemo;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class WorkerPool
{
    private final Map<String, CompileResponseDto> compileResponses;
    private final BlockingQueue<CompileRequestDto> blockingQueue;
    private final Collection<Worker> workers = new ArrayDeque<Worker>();
    private final File requestsLog;
    private final File exceptionsLog;

    private boolean busy = true;

    public WorkerPool(
            Map<String, CompileResponseDto> theCompileResponses,
            int maximumRequests,
            int numbersOfWorkers,
            File theRequestsLog,
            File theExceptionsLog) {
        compileResponses = theCompileResponses;
        requestsLog = theRequestsLog;
        exceptionsLog = theExceptionsLog;

        blockingQueue = new ArrayBlockingQueue<CompileRequestDto>(maximumRequests);

        for (int i = 0; i < numbersOfWorkers; ++i) {
            activateWorker();
        }
    }

    private void activateWorker() {
        Thread runLoop = new Thread()
        {
            public void run() {
                Worker worker = new Worker(compileResponses, requestsLog, exceptionsLog);
                workers.add(worker);
                while (busy) {
                    try {
                        worker.compile(blockingQueue.take());
                    } catch (InterruptedException e) {
                        //unexpected error, better shutdown
                        shutdown();
                    }
                }
            }
        };
        runLoop.start();
    }

    public void execute(CompileRequestDto dto) {
        try {
            blockingQueue.put(dto);
        } catch (InterruptedException e) {
            compileResponses.put(dto.ticket,new CompileResponseDto(true,"","Unexpected exception occurred, " +
                    "compilation was interrupted, please try again."));
            dto.latch.countDown();
        }
    }

    public int size() {
        return blockingQueue.size();
    }

    public void shutdown() {
        busy = false;
        for (Worker worker : workers) {
            worker.shutdown();
            execute(new CompileRequestDto("shutdown", "", new CountDownLatch(1)));
        }
    }
}
