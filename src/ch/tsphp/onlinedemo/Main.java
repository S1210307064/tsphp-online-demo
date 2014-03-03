package ch.tsphp.onlinedemo;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Main extends HttpServlet
{
    private final int MAX_REQUESTS = 100;
    private WorkerPool workerPool;

    private final Map<String, CompileResponseDto> compileResponses = new HashMap<String, CompileResponseDto>();
    private File counterLog;
    private static Object counterLock = new Object();
    private Properties emailProperties = new Properties();

    public void init() throws ServletException {
        String realPath = getServletContext().getRealPath("/");
        workerPool = new WorkerPool(
                compileResponses,
                MAX_REQUESTS,
                4,
                new File(realPath + "../requests.txt"),
                new File(realPath + "../exceptions.txt")
        );
        counterLog = new File(realPath + "../counter.txt");
        try {
            emailProperties.load(new FileReader(realPath + "../email.properties"));
        } catch (IOException e) {
            //use standard
            emailProperties.put("mail.smtp.host", "localhost");
        }
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
                if (workerPool.size() != MAX_REQUESTS) {
                    workerPool.execute(new CompileRequestDto(ticket, values[0], latch));
                    incrementCounter();
                    try {
                        latch.await();
                        CompileResponseDto dto = compileResponses.remove(ticket);
                        out.print("{");
                        out.print("\"console\":\"" + jsonEscape(dto.console) + "\"");
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

    private void incrementCounter() {
        Integer counter = null;
        synchronized (counterLock) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(counterLog));
                counter = Integer.parseInt(reader.readLine());
                ++counter;
                reader.close();
                FileWriter writer = new FileWriter(counterLog);
                writer.write(counter.toString());
                writer.close();
            } catch (FileNotFoundException e) {
                //That's a pity but we don't care
            } catch (IOException e) {
                //That's a pity but we don't care
            }
        }
        if (counter != null && counter % 100 == 0) {
            notifyPerEmail(counter);
        }
    }

    private void notifyPerEmail(int counter) {
        if (!emailProperties.isEmpty()) {
            // Get session
            Session session = Session.getInstance(emailProperties);
            try {
                Message message = new MimeMessage(session);

                message.setFrom(new InternetAddress("noreply@tutteli.ch"));
                InternetAddress[] address = {new InternetAddress("rstoll@tutteli.ch")};
                message.setRecipients(Message.RecipientType.TO, address);
                message.setSubject("Online Demo - " + counter + " requests");
                message.setSentDate(new Date());
                Transport.send(message);

            } catch (MessagingException ex) {
                //too bad but we don't care
            }
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
