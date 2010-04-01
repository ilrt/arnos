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

import com.hp.hpl.jena.rdf.model.Model;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wf.arnos.queryhandler.QueryHandlerInterface;

/**
 * Handles obtaining construct query from endpoint and parsing result set.
 * @author Chris Bailey (c.bailey@bristol.ac.uk)
 */
public class FetchModelResponseTask extends AbstractResponseTask
{
    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(FetchModelResponseTask.class);

    /**
     * Constructor for thread.
     * @param paramHandler handling class
     * @param paramQuery SPARQL query
     * @param paramUrl Endpoint url
     * @param paramDoneSignal Latch signal to use to notify parent when completed
     */
    public FetchModelResponseTask(final QueryHandlerInterface paramHandler,
                                                        final String paramQuery,
                                                        final String paramUrl,
                                                        final CountDownLatch paramDoneSignal)
    {
        super(paramHandler, paramQuery, paramUrl, paramDoneSignal);
    }

    /**
     * Executes the query on the specified endpoint and processes the results.
     */
    @Override
    public final void run()
    {
        try
        {
            String resultsString = getFromCache();

            // check cache copy
            if (resultsString == null)
            {
                resultsString = getQueryWrapper().execQuery(query, url);
                putInCache(resultsString);
            }

            if (resultsString != null && resultsString.length() > 0)
            {
                Model model  = getQueryWrapper().stringToModel(resultsString);

                handler.addResult(model);
            }
        }
        catch (Exception ex)
        {
            LOG.error("Unable to execute query against " + url, ex);
        }
        finally
        {
            doneSignal.countDown();
        }
    }
}