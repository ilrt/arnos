/*
 * Copyright (c) 2009, University of Bristol
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the name of the University of Bristol nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.wf.arnos.queryhandler.task;

import java.util.concurrent.CountDownLatch;
import org.apache.log4j.xml.DOMConfigurator;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.wf.arnos.queryhandler.JenaQueryWrapper;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;
import org.wf.arnos.queryhandler.JenaQueryWrapperTest;
import org.wf.arnos.queryhandler.QueryHandlerInterface;
import org.wf.arnos.queryhandler.QueryWrapperInterface;

/**
 *
 * @author Chris Bailey (c.bailey@bristol.ac.uk)
 */
public class FetchBooleanResponseTaskTest extends EasyMockSupport
{

    QueryHandlerInterface mockThreadedQueryHandler;

    QueryWrapperInterface mockQueryWrapper;

    CountDownLatch doneSignal;

    @Before
    public void setUp()
    {
        DOMConfigurator.configure("./src/main/webapp/WEB-INF/log4j.xml");
        mockQueryWrapper = createMock(QueryWrapperInterface.class);
        mockThreadedQueryHandler = createMock(QueryHandlerInterface.class);
        doneSignal = new CountDownLatch(1);
    }

    @Test
    public void testFetchAskResponseTask()
    {
        System.out.println("testFetchAskResponseTask");

        String askQuery = JenaQueryWrapperTest.getAskQuery(JenaQueryWrapperTest.ENDPOINT1_URL);
        String askResult = JenaQueryWrapperTest.getAskResult(JenaQueryWrapperTest.ENDPOINT1_URL);

        FetchBooleanResponseTask fetcher = new FetchBooleanResponseTask(mockThreadedQueryHandler,
                JenaQueryWrapperTest.ENDPOINT1_URL,
                askQuery,
                doneSignal)
        {
            @Override
            protected QueryWrapperInterface getQueryWrapper()
            {
                return mockQueryWrapper;
            }
        };


        expect(mockThreadedQueryHandler.hasCache())
                .andReturn(false)
                .anyTimes();

        expect(mockQueryWrapper.execQuery((String) notNull(), (String) notNull()))
                .andReturn(askResult);

        expect(mockQueryWrapper.stringToBoolean(askResult))
                .andReturn(true);

        replayAll();

        fetcher.run();

        verifyAll();

        // now check we've got the expected number of results
        assertEquals("Latch correctly set", 0, doneSignal.getCount());

        resetAll();

        expect(mockThreadedQueryHandler.hasCache())
                .andReturn(false)
                .anyTimes();

        expect(mockQueryWrapper.execQuery((String) notNull(), (String) notNull()))
                .andReturn(askResult.replace(">", "\n "));

        expect(mockQueryWrapper.stringToBoolean((String) notNull()))
                .andReturn(true);
        replayAll();

        fetcher.run();

        verifyAll();
    }

}