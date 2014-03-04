package ch.tsphp.onlinedemo;

public interface IWorkerPool
{
    void start();

    void shutdown();

    void execute(CompileRequestDto dto);

    int numberOfPendingRequests();
}
