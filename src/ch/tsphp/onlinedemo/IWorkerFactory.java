package ch.tsphp.onlinedemo;

import java.util.Map;

public interface IWorkerFactory
{
    public IWorker create(Map<String, CompileResponseDto> theCompileResponses);
}
