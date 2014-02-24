package ch.tsphp.onlinedemo;

import antlr.RecognitionException;
import ch.tsphp.HardCodedCompilerInitialiser;
import ch.tsphp.common.ICompiler;
import ch.tsphp.common.ICompilerListener;
import ch.tsphp.common.IErrorLogger;
import ch.tsphp.common.exceptions.TSPHPException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class Main extends HttpServlet implements ICompilerListener, IErrorLogger
{
    private CountDownLatch compilerLatch;

    ICompiler compiler;
    StringBuilder stringBuilder;
    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

    public void init() throws ServletException {
        compiler = new HardCodedCompilerInitialiser().create();
        compiler.registerCompilerListener(this);
        compiler.registerErrorLogger(this);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String[] values = request.getParameterValues("tsphp");
        if (values != null && values.length == 1) {
            stringBuilder = new StringBuilder();
            compilerLatch = new CountDownLatch(1);
            compiler.reset();
            compiler.addCompilationUnit("web", values[0]);
            compiler.compile();
            try {
                compilerLatch.await();
                out.print("{");
                out.print("\"console\":\"" + jsonEscape(stringBuilder.toString()) + "\"");
                if (!compiler.hasFoundError()) {
                    out.print(",\"php\":\"" + jsonEscape(compiler.getTranslations().get("web")) + "\"");
                }
                out.print("}");
            } catch (InterruptedException e) {
                out.print("{\"error\": \"Exception occurred, compilation was interrupted, please try again.\"}");
            }
        } else {
            out.print("{\"error\": \"None or more than one TSPHP code defined.\"}");
        }
    }

    private String jsonEscape(String json) {
        return json.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\t","\\t")
                .replace("\n","\\n")
                .replace("\r","");
    }

    @Override
    public void afterParsingAndDefinitionPhaseCompleted() {
        stringBuilder.append(dateFormat.format(new Date()) + ":  Parsing and Definition phase completed\n"
                + "----------------------------------------------------------------------\n");
    }

    @Override
    public void afterReferencePhaseCompleted() {
        stringBuilder.append(dateFormat.format(new Date()) + ": Reference phase completed\n"
                + "----------------------------------------------------------------------\n");
    }

    @Override
    public void afterTypecheckingCompleted() {
        stringBuilder.append(dateFormat.format(new Date()) + ": Type checking completed\n"
                + "----------------------------------------------------------------------\n");
    }

    @Override
    public void afterCompilingCompleted() {
        stringBuilder.append(dateFormat.format(new Date()) + ": Compilation completed\n");
        compilerLatch.countDown();
    }

    @Override
    public void log(TSPHPException exception) {
        stringBuilder.append(dateFormat.format(new Date()) + ": Unexpected exception occurred - "
                + exception.getMessage() + "\n");
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
