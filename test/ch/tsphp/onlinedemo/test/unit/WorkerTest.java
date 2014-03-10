/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.onlinedemo.test.unit;

import antlr.RecognitionException;
import ch.tsphp.ICompilerInitialiser;
import ch.tsphp.common.ICompiler;
import ch.tsphp.common.exceptions.TSPHPException;
import ch.tsphp.onlinedemo.CompileRequestDto;
import ch.tsphp.onlinedemo.IWorker;
import ch.tsphp.onlinedemo.Worker;
import org.junit.Test;
import org.mockito.exceptions.base.MockitoException;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class WorkerTest
{
    @Test
    public void compile_Standard_WriteToRequestLog() {
        File requestsLog = mock(File.class);
        when(requestsLog.exists()).thenReturn(true);
        when(requestsLog.getPath()).thenThrow(new MockitoException("just a unit test, do not really want to write"));

        IWorker worker = createWorker(requestsLog);
        try {
            worker.compile(new CompileRequestDto("1", "", new CountDownLatch(1)));
        } catch (MockitoException ex) {
            //that's fine caused by the file
        }

        verify(requestsLog).exists();
        verify(requestsLog).getPath();
    }

    @Test
    public void compile_RejectedExecutionExceptionAndNotYetShutdown_LogUnexpectedException() {
        ICompiler compiler = mock(ICompiler.class);
        ICompilerInitialiser compilerInitialiser = mock(ICompilerInitialiser.class);
        when(compilerInitialiser.create(any(ExecutorService.class))).thenReturn(compiler);
        doThrow(new RejectedExecutionException()).when(compiler).reset();
        File exceptionsLog = mock(File.class);
        when(exceptionsLog.exists()).thenReturn(true);
        when(exceptionsLog.getPath()).thenThrow(new MockitoException("just a unit test, do not really want to write"));

        IWorker worker = createWorker(compilerInitialiser, mock(File.class), exceptionsLog);
        try {
            worker.compile(new CompileRequestDto("1", "", new CountDownLatch(1)));
        } catch (MockitoException ex) {
            //that's fine caused by the file
        }

        verify(exceptionsLog).exists();
        verify(exceptionsLog).getPath();
    }

    @Test
    public void compile_RejectedExecutionExceptionAndNotYetShutdownLogDoesNotExist_LogUnexpectedException() {
        ICompiler compiler = mock(ICompiler.class);
        ICompilerInitialiser compilerInitialiser = mock(ICompilerInitialiser.class);
        when(compilerInitialiser.create(any(ExecutorService.class))).thenReturn(compiler);
        doThrow(new RejectedExecutionException()).when(compiler).reset();
        File exceptionsLog = mock(File.class);
        when(exceptionsLog.exists()).thenReturn(false);

        IWorker worker = createWorker(compilerInitialiser, mock(File.class), exceptionsLog);

        worker.compile(new CompileRequestDto("1", "", new CountDownLatch(1)));


        verify(exceptionsLog).exists();
        verifyNoMoreInteractions(exceptionsLog);
    }

    @Test
    public void compile_RejectedExecutionExceptionAlreadyShutdown_NoExceptionLoggingAndCountDownLatch() {
        ICompiler compiler = mock(ICompiler.class);
        ICompilerInitialiser compilerInitialiser = mock(ICompilerInitialiser.class);
        when(compilerInitialiser.create(any(ExecutorService.class))).thenReturn(compiler);
        doThrow(new RejectedExecutionException()).when(compiler).reset();
        File exceptionsLog = mock(File.class);
        CountDownLatch countDownLatch = new CountDownLatch(1);

        IWorker worker = createWorker(compilerInitialiser, mock(File.class), exceptionsLog);
        worker.shutdown();
        try {
            worker.compile(new CompileRequestDto("1", "", countDownLatch));
        } catch (MockitoException ex) {
            //that's fine caused by the file
        }

        verifyNoMoreInteractions(exceptionsLog);
        assertThat(countDownLatch.getCount(), is(0L));
    }

    @Test
    public void compile_IOExceptionDuringRequestLog_DoesNotStopCompilation() {
        ICompiler compiler = mock(ICompiler.class);
        //otherwise the compilerLatch.await will wait forever
        doThrow(new RejectedExecutionException()).when(compiler).compile();
        ICompilerInitialiser compilerInitialiser = mock(ICompilerInitialiser.class);
        when(compilerInitialiser.create(any(ExecutorService.class))).thenReturn(compiler);
        File requestsLog = mock(File.class);
        when(requestsLog.exists()).thenReturn(true);
        when(requestsLog.getPath()).thenReturn("./nonExistingFolder/nonExistingFile.txt");

        IWorker worker = createWorker(compilerInitialiser, requestsLog, mock(File.class));
        worker.compile(new CompileRequestDto("1", "", new CountDownLatch(1)));

        verify(requestsLog).exists();
        verify(requestsLog).getPath();
        verify(compiler).compile();
    }

    @Test
    public void log_UnexpectedException_WriteToExceptionLog() {
        File exceptionsLog = mock(File.class);
        when(exceptionsLog.exists()).thenReturn(true);
        when(exceptionsLog.getPath()).thenThrow(new MockitoException("just a unit test, do not really want to write"));
        Throwable throwable = mock(Throwable.class);

        IWorker worker = createWorker(mock(File.class), exceptionsLog);
        try {
            worker.log(new TSPHPException(throwable));
        } catch (MockitoException ex) {
            //that's fine caused by the file
        }

        verify(throwable).printStackTrace(any(PrintWriter.class));
        verify(exceptionsLog).exists();
        verify(exceptionsLog).getPath();
    }

    @Test
    public void log_UnexpectedExceptionLogDoesNotExist_WriteToExceptionLog() {
        File exceptionsLog = mock(File.class);
        when(exceptionsLog.exists()).thenReturn(false);
        Throwable throwable = mock(Throwable.class);

        IWorker worker = createWorker(mock(File.class), exceptionsLog);
        try {
            worker.log(new TSPHPException(throwable));
        } catch (MockitoException ex) {
            //that's fine caused by the file
        }

        verify(throwable).printStackTrace(any(PrintWriter.class));
        verify(exceptionsLog).exists();
        verifyNoMoreInteractions(exceptionsLog);
    }

    @Test
    public void log_RecognitionException_IsNotWrittenToExceptionLog() {
        File exceptionsLog = mock(File.class);

        IWorker worker = createWorker(mock(File.class), exceptionsLog);
        worker.log(new TSPHPException(new RecognitionException()));

        verifyNoMoreInteractions(exceptionsLog);
    }


    protected IWorker createWorker(File requestsLog) {
        return createWorker(requestsLog, mock(File.class));
    }

    protected IWorker createWorker(File requestsLog, File exceptionsLog) {
        ICompiler compiler = mock(ICompiler.class);
        ICompilerInitialiser compilerInitialiser = mock(ICompilerInitialiser.class);
        when(compilerInitialiser.create(any(ExecutorService.class))).thenReturn(compiler);
        return createWorker(compilerInitialiser, requestsLog, exceptionsLog);
    }

    protected IWorker createWorker(ICompilerInitialiser compilerInitialiser, File requestsLog, File exceptionsLog) {
        return new Worker(compilerInitialiser, mock(Map.class), requestsLog, exceptionsLog);
    }

}
