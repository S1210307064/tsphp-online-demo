/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

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
