/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.onlinedemo;

import java.util.concurrent.CountDownLatch;

/**
 * Represents a compilation requests where the CountDownLatch is used as async completion indicator.
 */
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
