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
    private StringBuilder stringBuilder;
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
        stringBuilder = new StringBuilder();
        compilerLatch = new CountDownLatch(1);
        compiler.reset();
        compiler.addCompilationUnit("web", dto.tsphp);
        compiler.compile();

        try {
            compilerLatch.await();
            if (!compiler.hasFoundError()) {
                compileResponses.put(dto.ticket, new CompileResponseDto(false, compiler.getTranslations().get("web"),
                        stringBuilder.toString()));
            } else {
                compileResponses.put(dto.ticket, new CompileResponseDto(true, "", stringBuilder.toString()));
            }
        } catch (InterruptedException e) {
            compileResponses.put(dto.ticket, new CompileResponseDto(true, "", "Unexpected exception occurred, " +
                    "compilation was interrupted, please try again."));
        }
        dto.latch.countDown();
    }

    @Override
    public void afterParsingAndDefinitionPhaseCompleted() {
        stringBuilder.append(dateFormat.format(new Date())).append(":  Parsing and Definition phase completed\n");
        stringBuilder.append("----------------------------------------------------------------------\n");
    }

    @Override
    public void afterReferencePhaseCompleted() {
        stringBuilder.append(dateFormat.format(new Date())).append(": Reference phase completed\n");
        stringBuilder.append("----------------------------------------------------------------------\n");
    }

    @Override
    public void afterTypecheckingCompleted() {
        stringBuilder.append(dateFormat.format(new Date())).append(": Type checking completed\n");
        stringBuilder.append("----------------------------------------------------------------------\n");
    }

    @Override
    public void afterCompilingCompleted() {
        stringBuilder.append(dateFormat.format(new Date())).append(": Compilation completed\n");
        compilerLatch.countDown();
    }

    @Override
    public void log(TSPHPException exception) {
        stringBuilder.append(dateFormat.format(new Date())).append(": Unexpected exception occurred - ")
                .append(exception.getMessage()).append("\n");
        //TODO log unexpected exceptions
        Throwable throwable = exception.getCause();
        if (throwable != null && !(throwable instanceof RecognitionException)) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            stringBuilder.append(stringWriter.toString());
        }
    }
}
