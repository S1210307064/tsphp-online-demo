package ch.tsphp.onlinedemo;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

public class WorkerPoolFactory implements IWorkerPoolFactory
{

    private final File requestsLog;
    private final File exceptionsLog;

    public WorkerPoolFactory(File theRequestsLog, File theExceptionsLog) {
        requestsLog = theRequestsLog;
        exceptionsLog = theExceptionsLog;
    }

    public IWorkerPool create(
            int maxRequests, Map<String, CompileResponseDto> compileResponses, int numberOfWorkers) {

        IWorkerFactory workerFactory = new WorkerFactory(requestsLog, exceptionsLog);
        return new WorkerPool(
                workerFactory,
                new ArrayBlockingQueue<CompileRequestDto>(maxRequests),
                compileResponses,
                numberOfWorkers
        );
    }
}
