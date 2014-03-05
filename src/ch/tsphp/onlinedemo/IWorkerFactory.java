package ch.tsphp.onlinedemo;

import java.util.Map;

public interface IWorkerFactory
{
    IWorker create(Map<String, CompileResponseDto> theCompileResponses);
}
