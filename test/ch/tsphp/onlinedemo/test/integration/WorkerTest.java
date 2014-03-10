/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.onlinedemo.test.integration;

import ch.tsphp.HardCodedCompilerInitialiser;
import ch.tsphp.onlinedemo.CompileRequestDto;
import ch.tsphp.onlinedemo.CompileResponseDto;
import ch.tsphp.onlinedemo.IWorker;
import ch.tsphp.onlinedemo.Worker;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class WorkerTest
{

    private static File requestsLog = new File("unitTestDummyRequestLog.txt");

    @AfterClass
    public static void setUpClass() {
        if (requestsLog.exists()) {
            requestsLog.delete();
        }
    }

    @Test
    public void compile_Standard_LogRequest() throws IOException {
        Map<String, CompileResponseDto> compileResponses = new HashMap<String, CompileResponseDto>();
        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(requestsLog), StandardCharsets.ISO_8859_1);
        writer.write("");
        writer.close();

        IWorker worker = createWorker(compileResponses);
        worker.compile(new CompileRequestDto("1", "int $a;", new CountDownLatch(1)));

        Scanner scanner = new Scanner(requestsLog);
        String firstLine = scanner.useDelimiter("\\Z").next();
        scanner.close();
        assertThat(firstLine, containsString("int $a;"));
    }

    protected IWorker createWorker(Map<String, CompileResponseDto> compileResponses) {
        return new Worker(
                new HardCodedCompilerInitialiser(),
                compileResponses,
                requestsLog,
                mock(File.class)
        );
    }


}
