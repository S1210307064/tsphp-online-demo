package ch.tsphp.onlinedemo;

import java.util.Map;

public interface IWorkerPoolFactory
{
    IWorkerPool create(int maxRequests, Map<String, CompileResponseDto> compileResponses, int numberOfWorkers);
}
