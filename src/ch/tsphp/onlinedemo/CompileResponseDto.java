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
