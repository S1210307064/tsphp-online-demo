package ch.tsphp.onlinedemo;

import java.util.concurrent.CountDownLatch;

public class CompileRequestDto
{
    public String ticket;
    public String tsphp;
    public CountDownLatch latch;

    public CompileRequestDto(String theTicketNumber, String theTSPHPCode, CountDownLatch theLatch) {
        ticket = theTicketNumber;
        tsphp = theTSPHPCode;
        latch = theLatch;
    }
}
