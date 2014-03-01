package ch.tsphp.onlinedemo;

import antlr.RecognitionException;
import ch.tsphp.HardCodedCompilerInitialiser;
import ch.tsphp.common.ICompiler;
import ch.tsphp.common.ICompilerListener;
import ch.tsphp.common.IErrorLogger;
import ch.tsphp.common.exceptions.TSPHPException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Worker implements ICompilerListener, IErrorLogger
{
    private final Map<String, CompileResponseDto> compileResponses;
    private CountDownLatch compilerLatch;
    private ICompiler compiler;
    private StringBuffer stringBuffer;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
    private ExecutorService executorService;

    public Worker(Map<String, CompileResponseDto> theCompileResponses) {
        compileResponses = theCompileResponses;

        executorService = Executors.newSingleThreadExecutor();
        compiler = new HardCodedCompilerInitialiser().create(executorService);
        compiler.registerCompilerListener(this);
        compiler.registerErrorLogger(this);
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public void compile(CompileRequestDto dto) {
        stringBuffer = new StringBuffer();
        compilerLatch = new CountDownLatch(1);
        compiler.reset();
        compiler.addCompilationUnit("web", dto.tsphp);
        compiler.compile();
        try {
            compilerLatch.await();
            if (!compiler.hasFoundError()) {
                compileResponses.put(dto.ticket, new CompileResponseDto(false, compiler.getTranslations().get("web"),
                        stringBuffer.toString()));
            } else {
                compileResponses.put(dto.ticket, new CompileResponseDto(true, "", stringBuffer.toString()));
            }
        } catch (InterruptedException e) {
            compileResponses.put(dto.ticket, new CompileResponseDto(true, "", "Unexpected exception occurred, " +
                    "compilation was interrupted, please try again."));
        }
        dto.latch.countDown();
    }

    @Override
    public void afterParsingAndDefinitionPhaseCompleted() {
        stringBuffer.append(dateFormat.format(new Date())).append(": Parsing and Definition phase completed\n");
        stringBuffer.append("----------------------------------------------------------------------\n");
    }

    @Override
    public void afterReferencePhaseCompleted() {
        stringBuffer.append(dateFormat.format(new Date())).append(": Reference phase completed\n");
        stringBuffer.append("----------------------------------------------------------------------\n");
    }

    @Override
    public void afterTypecheckingCompleted() {
        stringBuffer.append(dateFormat.format(new Date())).append(": Type checking completed\n");
        stringBuffer.append("----------------------------------------------------------------------\n");
    }

    @Override
    public void afterCompilingCompleted() {
        stringBuffer.append(dateFormat.format(new Date())).append(": Compilation completed\n");
        compilerLatch.countDown();
    }

    @Override
    public void log(TSPHPException exception) {
        stringBuffer.append(dateFormat.format(new Date())).append(": ").append(exception.getMessage()).append("\n");
        //TODO log unexpected exceptions
        Throwable throwable = exception.getCause();
        if (throwable != null && !(throwable instanceof RecognitionException)) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            stringBuffer.append(stringWriter.toString());
        }
    }
}
