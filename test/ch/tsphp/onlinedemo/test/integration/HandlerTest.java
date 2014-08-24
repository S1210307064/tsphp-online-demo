/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.onlinedemo.test.integration;

import ch.tsphp.onlinedemo.Handler;
import ch.tsphp.onlinedemo.WorkerPoolFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletException;
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
import java.util.Properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HandlerTest
{

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

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

    @Test
    public void doPost_Standard_WriteCounterLog() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"int $a;"});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);
        File counterLog = folder.newFile("unitTestDummyCounterLog.txt");

        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(counterLog), StandardCharsets.ISO_8859_1);
        writer.write("0");
        writer.close();

        //Act
        Handler handler = createHandler(counterLog);
        handler.doPost(request, response);
        handler.destroy();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(counterLog), StandardCharsets.ISO_8859_1));
        Integer counter = Integer.parseInt(reader.readLine());
        reader.close();
        assertThat(counter, is(1));
    }

    @Test
    public void doPost_EmailLimitReachedPropertiesEmpty_TryToSendEmailButStop() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"int $a;"});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);
        Properties emailProperties = mock(Properties.class);
        when(emailProperties.isEmpty()).thenReturn(true);
        File counterLog = folder.newFile("unitTestDummyCounterLog.txt");

        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(counterLog), StandardCharsets.ISO_8859_1);
        writer.write("99");
        writer.close();

        //Act
        Handler handler = createHandler(emailProperties, counterLog, mock(File.class));
        handler.doPost(request, response);
        handler.destroy();

        verify(emailProperties).isEmpty();
        verifyNoMoreInteractions(emailProperties);
    }

    @Test
    public void doPost_EmailLimitReached_SendEmail() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"int $a;"});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);
        Properties emailProperties = mock(Properties.class);
        when(emailProperties.isEmpty()).thenReturn(false);
        when(emailProperties.getProperty("mail.debug")).thenReturn("false");
        when(emailProperties.getProperty("mail.smtp.host")).thenReturn("nonExistingHost");
        when(emailProperties.getProperty("mail.smtp.timeout")).thenReturn("10");
        when(emailProperties.getProperty("mail.smtp.connectiontimeout")).thenReturn("10");
        File counterLog = folder.newFile("unitTestDummyCounterLog.txt");

        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(counterLog), StandardCharsets.ISO_8859_1);
        writer.write("99");
        writer.close();

        //Act
        Handler handler = createHandler(emailProperties, counterLog, mock(File.class));
        handler.doPost(request, response);
        handler.destroy();

        verify(emailProperties).isEmpty();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emailProperties, atLeast(4)).getProperty(captor.capture());
        assertThat(captor.getAllValues(), hasItem("mail.debug"));
        assertThat(captor.getAllValues(), hasItem("mail.smtp.host"));
        assertThat(captor.getAllValues(), hasItem("mail.smtp.timeout"));
        assertThat(captor.getAllValues(), hasItem("mail.smtp.connectiontimeout"));
    }


    @Test
    public void doPost_NumberFormatExceptionDuringReadingCounterLog_DoesResetCounterWriteCounterException()
            throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("tsphp")).thenReturn(new String[]{"int $a;"});
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);
        File counterLog = folder.newFile("unitTestDummyCounterLog.txt");
        File counterExceptionsLog = folder.newFile("unitTestDummyCounterExceptionsLog.txt");

        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(counterLog), StandardCharsets.ISO_8859_1);
        writer.write("a");
        writer.close();

        writer = new OutputStreamWriter(new FileOutputStream(counterExceptionsLog), StandardCharsets.ISO_8859_1);
        writer.write("");
        writer.close();

        //Act
        Handler handler = createHandler(counterLog, counterExceptionsLog);
        handler.doPost(request, response);
        handler.destroy();

        //Assert
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(counterLog), StandardCharsets.ISO_8859_1));
        Integer counter = Integer.parseInt(reader.readLine());
        reader.close();
        assertThat(counter, is(0));

        reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(counterExceptionsLog), StandardCharsets.ISO_8859_1));
        String log = reader.readLine();
        reader.close();
        assertThat(log, not(""));
    }

    @Test
    public void
    doPost_NumberFormatExceptionDuringReadingCounterLogAndSecurityExceptionDuringCounterException_Continues()
            throws IOException {
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
        File counterLog = folder.newFile("unitTestDummyCounterLog.txt");

        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(counterLog), StandardCharsets.ISO_8859_1);
        writer.write("a");
        writer.close();

        File counterExceptions = mock(File.class);
        when(counterExceptions.exists()).thenReturn(true);
        when(counterExceptions.getPath()).thenThrow(new SecurityException());

        //Act
        Handler handler = createHandler(counterLog, counterExceptions);
        handler.doPost(request, response);
        handler.destroy();

        assertThat(stringBuilder.toString(), containsString("\"php\":\"<?php\\nnamespace{\\n    $a;\\n}\\n?>\""));
    }

    @Test
    public void doPost_ReadCounterLogSecurityExceptionOccurs_DoesNotWriteAndStillContinues() throws IOException {
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
        File counterLogFile = mock(File.class);
        when(counterLogFile.exists()).thenReturn(true);
        when(counterLogFile.getPath()).thenThrow(new SecurityException());

        Handler handler = createHandler(counterLogFile);
        handler.doPost(request, response);
        handler.destroy();

        verify(counterLogFile).exists();
        verify(counterLogFile).getPath();
        verifyNoMoreInteractions(counterLogFile);
        assertThat(stringBuilder.toString(), not(""));
    }

    @Test
    public void doPost_WriteCounterLogSecurityExceptionOccurs_DoesNotWriteAndStillContinues() throws IOException {
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
        File counterLog = spy(folder.newFile("unitTestDummyCounterLog.txt"));
        when(counterLog.getPath()).thenCallRealMethod().thenThrow(new SecurityException());

        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(counterLog), StandardCharsets.ISO_8859_1);
        writer.write("0");
        writer.close();


        //Act
        Handler handler = createHandler(counterLog);
        handler.doPost(request, response);
        handler.destroy();


        verify(counterLog).exists();
        verify(counterLog, times(2)).getPath();
        verifyNoMoreInteractions(counterLog);
        assertThat(stringBuilder.toString(), not(""));
    }

    protected Handler createHandler() {
        return createHandler(new File("NonExistingFile"));
    }

    protected Handler createHandler(File counterLog) {
        return createHandler(counterLog, mock(File.class));
    }

    protected Handler createHandler(File counterLog, File counterExceptionsLog) {
        return createHandler(new Properties(), counterLog, counterExceptionsLog);
    }

    protected Handler createHandler(Properties emailProperties, File counterLog, File counterExceptionsLog) {
        File file = new File("NonExistingFile");
        return new Handler(new WorkerPoolFactory(file, file), emailProperties, counterLog, counterExceptionsLog);
    }
}
