/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.onlinedemo;

public class CompileResponseDto
{
    public boolean hasFoundError = false;
    public String php;
    public String console;

    public CompileResponseDto(boolean wereErrorsFound, String thePHPCode, String theConsoleOutput) {
        hasFoundError = wereErrorsFound;
        php = thePHPCode;
        console = theConsoleOutput;
    }
}
