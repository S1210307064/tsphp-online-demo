package ch.tsphp.onlinedemo.test.integration;

import ch.tsphp.onlinedemo.CompileRequestDto;
import ch.tsphp.onlinedemo.CompileResponseDto;
import ch.tsphp.onlinedemo.IWorkerPool;
import ch.tsphp.onlinedemo.WorkerFactory;
import ch.tsphp.onlinedemo.WorkerPool;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class WorkerPoolTest
{


    @Test
    public void success() throws InterruptedException {
        File file = new File("NonExistingFile");
        Map<String, CompileResponseDto> compileResponses = new HashMap<String, CompileResponseDto>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String ticket = "1";
        CompileRequestDto dto = new CompileRequestDto(ticket, "int $a;", countDownLatch);

        IWorkerPool workerPool = createWorkerPool(file, compileResponses);
        workerPool.start();
        workerPool.execute(dto);
        //I assume there is an error if it needs longer than 2 seconds
        countDownLatch.await(2L, TimeUnit.SECONDS);
        workerPool.shutdown();

        assertThat(compileResponses, org.hamcrest.collection.IsMapContaining.hasKey(ticket));
        assertThat(compileResponses.size(), is(1));
        CompileResponseDto responseDto = compileResponses.get(ticket);
        assertFalse(responseDto.hasFoundError);
        assertThat(responseDto.php.replaceAll("\r", ""), is("<?php\nnamespace{\n    $a;\n}\n?>"));
        assertThat(responseDto.console, not(""));
    }


    @Test
    public void error() throws InterruptedException {
        File file = new File("NonExistingFile");
        Map<String, CompileResponseDto> compileResponses = new HashMap<String, CompileResponseDto>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String ticket = "2";
        CompileRequestDto dto = new CompileRequestDto(ticket, "asdf;", countDownLatch);

        IWorkerPool workerPool = createWorkerPool(file, compileResponses);
        workerPool.start();
        workerPool.execute(dto);
        //I assume there is an error if it needs longer than 2 seconds
        countDownLatch.await(2L, TimeUnit.SECONDS);
        workerPool.shutdown();

        assertThat(compileResponses, org.hamcrest.collection.IsMapContaining.hasKey(ticket));
        assertThat(compileResponses.size(), is(1));
        CompileResponseDto responseDto = compileResponses.get(ticket);
        assertTrue(responseDto.hasFoundError);
        assertThat(responseDto.console, not(""));
    }


    protected WorkerPool createWorkerPool(File file, Map<String, CompileResponseDto> compileResponses) {
        return new WorkerPool(
                new WorkerFactory(file, file),
                new ArrayBlockingQueue<CompileRequestDto>(10),
                compileResponses,
                2
        );
    }
}
