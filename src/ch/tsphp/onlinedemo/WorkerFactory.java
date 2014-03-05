package ch.tsphp.onlinedemo;

import ch.tsphp.HardCodedCompilerInitialiser;

import java.io.File;
import java.util.Map;

public class WorkerFactory implements IWorkerFactory
{

    private final File requestsLog;
    private final File exceptionsLog;

    public WorkerFactory(File theRequestsLog, File theExceptionsLog) {
        this.requestsLog = theRequestsLog;
        this.exceptionsLog = theExceptionsLog;
    }

    @Override
    public IWorker create(Map<String, CompileResponseDto> theCompileResponses) {
        return new Worker(new HardCodedCompilerInitialiser(), theCompileResponses, requestsLog, exceptionsLog);
    }
}
