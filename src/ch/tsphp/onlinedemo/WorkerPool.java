/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.onlinedemo;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

public class WorkerPool implements IWorkerPool
{
    private final Map<String, CompileResponseDto> compileResponses;
    private final BlockingQueue<CompileRequestDto> blockingQueue;
    private final Collection<IWorker> workers = new CopyOnWriteArrayList<IWorker>();
    private final IWorkerFactory workerFactory;
    private final int numbersOfWorkers;

    private boolean busy = false;

    public WorkerPool(
            IWorkerFactory theWorkerFactory,
            BlockingQueue<CompileRequestDto> theBlockingQueue,
            Map<String, CompileResponseDto> theCompileResponses,
            int theNumbersOfWorkers) {
        workerFactory = theWorkerFactory;
        blockingQueue = theBlockingQueue;
        compileResponses = theCompileResponses;
        numbersOfWorkers = theNumbersOfWorkers;
    }

    @Override
    public void start() {
        busy = true;
        for (int i = 0; i < numbersOfWorkers; ++i) {
            activateWorker();
        }
    }

    private void activateWorker() {
        Thread runLoop = new Thread()
        {
            public void run() {
                IWorker worker = workerFactory.create(compileResponses);
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

    @Override
    public void execute(CompileRequestDto dto) {
        try {
            blockingQueue.put(dto);
        } catch (InterruptedException e) {
            compileResponses.put(dto.ticket, new CompileResponseDto(true, "", "Unexpected exception occurred, "
                    + "compilation was interrupted, please try again."));
            dto.latch.countDown();
        }
    }

    @Override
    public int numberOfPendingRequests() {
        return blockingQueue.size();
    }

    @Override
    public void shutdown() {
        if (busy) {
            busy = false;
            for (IWorker worker : workers) {
                worker.shutdown();
                execute(new CompileRequestDto("shutdown", "", new CountDownLatch(1)));
            }
        } else {
            throw new IllegalStateException("cannot shutdown when WorkerPool was not started");
        }
    }
}
