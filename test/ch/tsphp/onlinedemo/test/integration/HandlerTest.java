package ch.tsphp.onlinedemo.test.integration;

import ch.tsphp.onlinedemo.Handler;
import ch.tsphp.onlinedemo.WorkerPoolFactory;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HandlerTest
{
    @Test
    public void doPost_Success_ResponseIncludePhpCode() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"int $a;"});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        final StringBuilder stringBuilder = new StringBuilder();
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                stringBuilder.append(invocation.getArguments()[0]);
                return false;
            }
        }).when(printWriter).print(anyString());
        when(response.getWriter()).thenReturn(printWriter);

        Handler handler = createHandler();
        handler.doPost(request, response);
        handler.destroy();

        assertThat(stringBuilder.toString(), containsString("\"php\":\"<?php\\nnamespace{\\n    $a;\\n}\\n?>\""));
    }

    @Test
    public void doPost_Error_ResponseDoesNotIncludePhpCode() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"asdf;"});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        final StringBuilder stringBuilder = new StringBuilder();
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                stringBuilder.append(invocation.getArguments()[0]);
                return false;
            }
        }).when(printWriter).print(anyString());
        when(response.getWriter()).thenReturn(printWriter);

        Handler handler = createHandler();
        handler.doPost(request, response);
        handler.destroy();

        assertThat(stringBuilder.toString(), not(containsString("\"php\"")));
    }

    protected Handler createHandler() {
        File file = new File("NonExistingFile");
        return new Handler(new WorkerPoolFactory(file, file), new Properties(), file);
    }
}
