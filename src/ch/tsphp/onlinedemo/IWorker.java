package ch.tsphp.onlinedemo;

import ch.tsphp.common.ICompilerListener;
import ch.tsphp.common.IErrorLogger;

public interface IWorker extends ICompilerListener, IErrorLogger
{
    void shutdown();

    void compile(CompileRequestDto dto);
}
