/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.onlinedemo.test.unit;

import ch.tsphp.onlinedemo.CompileRequestDto;
import ch.tsphp.onlinedemo.IWorker;
import ch.tsphp.onlinedemo.IWorkerFactory;
import ch.tsphp.onlinedemo.IWorkerPool;
import ch.tsphp.onlinedemo.WorkerPool;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkerPoolTest
{
    @Test
    public void start_TwoWorkers_CallsWorkerFactoryTwice() throws InterruptedException {
        IWorker worker = mock(IWorker.class);
        IWorkerFactory workerFactory = mock(IWorkerFactory.class);
        when(workerFactory.create(anyMap())).thenReturn(worker);
        int numberOfWorkers = 2;

        IWorkerPool workerPool = createWorkerPool(workerFactory, numberOfWorkers);
        workerPool.start();
        //Give it some time to set up the workers
        Thread.sleep(50);
        workerPool.shutdown();

        verify(workerFactory, times(numberOfWorkers)).create(anyMap());
    }

    @Test
    public void start_FourWorkers_CallsWorkerFactoryFourTimes() throws InterruptedException {
        IWorker worker = mock(IWorker.class);
        IWorkerFactory workerFactory = mock(IWorkerFactory.class);
        when(workerFactory.create(anyMap())).thenReturn(worker);
        int numberOfWorkers = 4;

        IWorkerPool workerPool = createWorkerPool(workerFactory, numberOfWorkers);
        workerPool.start();
        //Give it some time to set up the workers
        Thread.sleep(50);
        workerPool.shutdown();

        verify(workerFactory, times(numberOfWorkers)).create(anyMap());
    }

    @Test
    public void start_InterruptDuringTake_ResultsInShutdown() throws InterruptedException {
        IWorker worker = mock(IWorker.class);
        IWorkerFactory workerFactory = mock(IWorkerFactory.class);
        when(workerFactory.create(anyMap())).thenReturn(worker);
        BlockingQueue<CompileRequestDto> blockingQueue = mock(BlockingQueue.class);
        doThrow(new InterruptedException()).when(blockingQueue).take();

        IWorkerPool workerPool = createWorkerPool(workerFactory, blockingQueue, 1);
        workerPool.start();
        //Give it some time to set up the workers
        Thread.sleep(50);

        verify(worker).shutdown();
    }

    @Test
    public void execute_Interrupted_CountDown() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CompileRequestDto dto = new CompileRequestDto("1", "2", countDownLatch);
        BlockingQueue<CompileRequestDto> blockingQueue = mock(BlockingQueue.class);
        doThrow(new InterruptedException()).when(blockingQueue).put(dto);

        IWorkerPool workerPool = createWorkerPool(blockingQueue);
        workerPool.execute(dto);

        countDownLatch.await(1L, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(), is(0L));
    }

    @Test
    public void execute_NotInterrupted_CountDownMissing() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CompileRequestDto dto = new CompileRequestDto("1", "2", countDownLatch);

        IWorkerPool workerPool = createWorkerPool();
        workerPool.execute(dto);

        countDownLatch.await(1L, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount(), is(1L));
    }

    @Test
    public void numberOfPendingRequests_Standard_EqualsSizeOfBlockingQueue() {
        BlockingQueue<CompileRequestDto> blockingQueue = mock(BlockingQueue.class);
        int sizeOfBlockingQueue = 2;
        when(blockingQueue.size()).thenReturn(sizeOfBlockingQueue);

        IWorkerPool workerPool = createWorkerPool(blockingQueue);
        int result = workerPool.numberOfPendingRequests();

        assertThat(result, is(sizeOfBlockingQueue));
    }

    @Test(expected = IllegalStateException.class)
    public void shutdown_WithoutStart_IllegalStateException() {
        //no arrange necessary

        IWorkerPool workerPool = createWorkerPool();
        workerPool.shutdown();

        //assert in annotation - expect an IllegalStateException
    }

    @Test
    public void shutdown_Standard_CallsShutdownOnEachWorker() throws InterruptedException {
        IWorker worker = mock(IWorker.class);
        IWorkerFactory workerFactory = mock(IWorkerFactory.class);
        when(workerFactory.create(anyMap())).thenReturn(worker);
        int numberOfWorkers = 4;

        IWorkerPool workerPool = createWorkerPool(workerFactory, numberOfWorkers);
        workerPool.start();
        //Give it some time to set up the workers
        Thread.sleep(50);
        workerPool.shutdown();

        verify(worker, times(numberOfWorkers)).shutdown();
    }

    protected IWorkerPool createWorkerPool() {
        return createWorkerPool(mock(BlockingQueue.class));
    }

    protected IWorkerPool createWorkerPool(BlockingQueue<CompileRequestDto> blockingQueue) {
        return createWorkerPool(mock(IWorkerFactory.class), blockingQueue, 1);
    }

    protected IWorkerPool createWorkerPool(IWorkerFactory workerFactory, int numberOfWorkers) {
        return createWorkerPool(workerFactory, mock(BlockingQueue.class), numberOfWorkers);

    }

    protected IWorkerPool createWorkerPool(
            IWorkerFactory workerFactory,
            BlockingQueue<CompileRequestDto> blockingQueue,
            int numberOfWorkers) {
        return new WorkerPool(workerFactory, blockingQueue, mock(Map.class), numberOfWorkers);
    }


}
