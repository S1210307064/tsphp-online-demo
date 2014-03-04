package ch.tsphp.onlinedemo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Main extends HttpServlet
{
    private Handler handler;

    public void init() throws ServletException {
        String realPath = getServletContext().getRealPath("/");
        Properties emailProperties = new Properties();
        try {
            emailProperties.load(new FileReader(realPath + "../email.properties"));
        } catch (IOException e) {
            //use standard
            emailProperties.put("mail.smtp.host", "localhost");
            emailProperties.put("mail.smtp.timeout", "1000");
            emailProperties.put("mail.smtp.connectionTimeout", "1000");
        }
        handler = new Handler(
                new WorkerPoolFactory(
                        new File(realPath + "../requests.txt"),
                        new File(realPath + "../exceptions.txt")
                ), emailProperties,
                new File(realPath + "../counter.txt")
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
