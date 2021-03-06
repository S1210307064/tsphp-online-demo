/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.onlinedemo;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Handler
{
    private static final int MAX_REQUESTS = 100;
    private static final int NUMBER_OF_WORKERS = 4;
    private static final Object COUNTER_LOCK = new Object();
    private static final Object COUNTER_EXCEPTION_LOCK = new Object();

    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
    private final IWorkerPool workerPool;
    private final Map<String, CompileResponseDto> compileResponses = new HashMap<String, CompileResponseDto>();
    private final File counterLog;
    private final File counterExceptionsLog;

    private final Properties emailProperties;

    public Handler(IWorkerPoolFactory workerPoolFactory, Properties theEmailProperties,
            File theCounterLog, File theCounterExceptionsLog) {
        workerPool = workerPoolFactory.create(MAX_REQUESTS, compileResponses, NUMBER_OF_WORKERS);
        workerPool.start();

        emailProperties = theEmailProperties;
        counterLog = theCounterLog;
        counterExceptionsLog = theCounterExceptionsLog;
    }

    public void destroy() {
        workerPool.shutdown();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String[] values = request.getParameterValues("tsphp");
        if (values != null && values.length == 1) {
            String tsphp = values[0].trim();
            if (tsphp.length() != 0) {
                if (workerPool.numberOfPendingRequests() < MAX_REQUESTS) {
                    String ticket = UUID.randomUUID().toString();
                    CountDownLatch latch = new CountDownLatch(1);
                    execute(out, latch, new CompileRequestDto(ticket, tsphp, latch));
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

    private void execute(PrintWriter out, CountDownLatch latch, CompileRequestDto requestDto) {
        workerPool.execute(requestDto);
        incrementCounter();
        try {
            latch.await();
            CompileResponseDto responseDto = compileResponses.remove(requestDto.ticket);
            out.print("{");
            out.print("\"console\":\"" + jsonEscape(responseDto.console) + "\"");
            if (!responseDto.hasFoundError) {
                out.print(",\"php\":\"" + jsonEscape(responseDto.php) + "\"");
            }
            out.print("}");
        } catch (InterruptedException e) {
            out.print("{\"error\": \"Exception occurred, compilation was interrupted, "
                    + "please try again.\"}");
        }
    }

    private void incrementCounter() {
        Integer counter = null;
        synchronized (COUNTER_LOCK) {
            if (counterLog.exists()) {
                String log = readCounterLog();
                if (log != null) {
                    try {
                        counter = Integer.parseInt(log);
                        ++counter;
                        writeCounterLog(counter);
                    } catch (NumberFormatException e) {
                        writeCounterException(log);
                        writeCounterLog(0);
                    }
                }
            }
        }

        if (counter != null && counter % MAX_REQUESTS == 0) {
            notifyPerEmail(counter);
        }

    }

    private String readCounterLog() {
        String log = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(counterLog), StandardCharsets.ISO_8859_1));
            log = reader.readLine();
            reader.close();
        } catch (IOException e) {
            //That's a pity but we don't care
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e2) {
                    //that's fine
                }
            }
        }
        return log;
    }

    private void writeCounterLog(Integer counter) {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(counterLog), StandardCharsets.ISO_8859_1);
            writer.write(counter.toString());
            writer.close();
        } catch (IOException e) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e2) {
                    //that's fine
                }
            }
        }
    }

    private void writeCounterException(String log) {
        synchronized (COUNTER_EXCEPTION_LOCK) {
            OutputStreamWriter writer = null;
            try {
                writer = new OutputStreamWriter(
                        new FileOutputStream(counterExceptionsLog, true), StandardCharsets.ISO_8859_1);
                writer.write(dateFormat.format(new Date()));
                writer.append(log).append("\n");
                writer.close();
            } catch (IOException e) {
                //That's a pity but we don't care
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e2) {
                        //that's fine
                    }
                }
            }
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
