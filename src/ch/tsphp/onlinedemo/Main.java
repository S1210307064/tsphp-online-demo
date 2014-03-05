package ch.tsphp.onlinedemo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Main extends HttpServlet
{
    private Handler handler;

    public void init() throws ServletException {
        String realPath = getServletContext().getRealPath("/");
        Properties emailProperties = new Properties();
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(
                    new FileInputStream(realPath + "../email.properties"), StandardCharsets.ISO_8859_1);

            emailProperties.load(reader);
            reader.close();
        } catch (IOException e) {
            //use standard
            emailProperties.put("mail.smtp.host", "localhost");
            emailProperties.put("mail.smtp.timeout", "1000");
            emailProperties.put("mail.smtp.connectiontimeout", "1000");
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    //that's fine
                }
            }
        }

        handler = new Handler(
                new WorkerPoolFactory(
                        new File(realPath + "../requests.txt"),
                        new File(realPath + "../exceptions.txt")
                ), emailProperties,
                new File(realPath + "../counter.txt"),
                new File(realPath + "../counterExceptions.txt")
        );
    }

    public void destroy() {
        handler.destroy();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handler.doPost(request, response);
    }
}
