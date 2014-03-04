package ch.tsphp.onlinedemo.test.unit;

import ch.tsphp.onlinedemo.Handler;
import ch.tsphp.onlinedemo.IWorkerPool;
import ch.tsphp.onlinedemo.IWorkerPoolFactory;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HandlerTest
{
    @Test
    public void doPost_ParameterNotSet_WriteError() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);

        Handler handler = createHandler();
        handler.doPost(request, response);

        verify(printWriter).print("{\"error\": \"None or more than one TSPHP code defined.\"}");
    }

    @Test
    public void doPost_ParameterLengthBiggerThanOne_WriteError() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"1", "2values"});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);

        Handler handler = createHandler();
        handler.doPost(request, response);

        verify(printWriter).print("{\"error\": \"None or more than one TSPHP code defined.\"}");
    }

    @Test
    public void doPost_CodeIsEmpty_WriteError() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{""});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);

        Handler handler = createHandler();
        handler.doPost(request, response);

        verify(printWriter).print("{\"error\": \"None or more than one TSPHP code defined.\"}");
    }

    @Test
    public void doPost_CodeIsOnlySpaces_WriteError() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"  "});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);

        Handler handler = createHandler();
        handler.doPost(request, response);

        verify(printWriter).print("{\"error\": \"None or more than one TSPHP code defined.\"}");
    }

    @Test
    public void doPost_MaxRequestsReached_WriteTryLater() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"int $a;"});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);

        IWorkerPool workerPool = mock(IWorkerPool.class);
        when(workerPool.numberOfPendingRequests()).thenReturn(100);
        IWorkerPoolFactory workerPoolFactory = mock(IWorkerPoolFactory.class);
        when(workerPoolFactory.create(anyInt(), anyMap(), anyInt())).thenReturn(workerPool);

        //Act
        Handler handler = createHandler(workerPoolFactory);
        handler.doPost(request, response);

        verify(printWriter).print("{\"error\": \"Too many requests at the moment. Please try it again in a moment.\"}");
        verify(workerPool).start();
        verify(workerPool).numberOfPendingRequests();
        verifyNoMoreInteractions(workerPool);
    }


    @Test
    public void doPost_MaxRequestsExceeded_WriteTryLater() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"int $a;"});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);

        IWorkerPool workerPool = mock(IWorkerPool.class);
        when(workerPool.numberOfPendingRequests()).thenReturn(200);
        IWorkerPoolFactory workerPoolFactory = mock(IWorkerPoolFactory.class);
        when(workerPoolFactory.create(anyInt(), anyMap(), anyInt())).thenReturn(workerPool);

        //Act
        Handler handler = createHandler(workerPoolFactory);
        handler.doPost(request, response);

        verify(printWriter).print("{\"error\": \"Too many requests at the moment. Please try it again in a moment.\"}");
        verify(workerPool).start();
        verify(workerPool).numberOfPendingRequests();
        verifyNoMoreInteractions(workerPool);
    }

    protected Handler createHandler() {
        IWorkerPool workerPool = mock(IWorkerPool.class);
        IWorkerPoolFactory workerPoolFactory = mock(IWorkerPoolFactory.class);
        when(workerPoolFactory.create(anyInt(), anyMap(), anyInt())).thenReturn(workerPool);
        return createHandler(workerPoolFactory);
    }

    protected Handler createHandler(IWorkerPoolFactory workerPoolFactory) {
        return new Handler(workerPoolFactory, new Properties(), mock(File.class));
    }

}
