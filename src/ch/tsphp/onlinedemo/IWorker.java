/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

package ch.tsphp.onlinedemo;

import ch.tsphp.common.ICompilerListener;
import ch.tsphp.common.IErrorLogger;

/**
 * Responsible to work off incoming compile requests.
 * <p/>
 * That means it uses the TSPHP compiler and stores the result somehow.
 */
public interface IWorker extends ICompilerListener, IErrorLogger
{
    void shutdown();

    void compile(CompileRequestDto dto);
}
