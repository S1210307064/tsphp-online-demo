package ch.tsphp.onlinedemo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Main extends HttpServlet
{
    private final int MAX_REQUESTS = 1;
    private WorkerPool workerPool;

    private final Map<String, CompileResponseDto> compileResponses = new HashMap<String, CompileResponseDto>();

    public void init() throws ServletException {
        workerPool = new WorkerPool(compileResponses, MAX_REQUESTS, 4);
    }

    public void destroy() {
        workerPool.shutdown();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String[] values = request.getParameterValues("tsphp");
        if (values != null && values.length == 1) {
            String tsphp = values[0].trim();
            if (tsphp.length() != 0) {
                String ticket = UUID.randomUUID().toString();
                CountDownLatch latch = new CountDownLatch(1);
                int size = workerPool.size();
                if (workerPool.size() != MAX_REQUESTS) {
                    workerPool.execute(new CompileRequestDto(ticket, values[0], latch));
                    try {
                        latch.await();
                        CompileResponseDto dto = compileResponses.remove(ticket);
                        out.print("{");
                        out.print("\"console\":\"size: " + size + " - " + jsonEscape(dto.console) + "\"");
                        if (!dto.hasFoundError) {
                            out.print(",\"php\":\"" + jsonEscape(dto.php) + "\"");
                        }
                        out.print("}");
                    } catch (InterruptedException e) {
                        out.print("{\"error\": \"Exception occurred, compilation was interrupted, " +
                                "please try again.\"}");
                    }
                } else {
                    out.print("{\"error\": \"Too many requests at the moment. Please try it again in a moment.\"}");
                }
            } else {
                out.print("{\"error\": \"None or more than one TSPHP code defined.\"}");
            }
        } else {
            out.print("{\"error\": \"None or more than one TSPHP code defined.\"}");
        }
    }

    private String jsonEscape(String json) {
        return json.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
