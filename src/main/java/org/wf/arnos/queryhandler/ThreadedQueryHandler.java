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
package org.wf.arnos.queryhandler;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wf.arnos.cachehandler.CacheHandlerInterface;
import org.wf.arnos.controller.model.Endpoint;
import org.wf.arnos.controller.model.sparql.Result;

/**
 * A query handler that uses multithreading to handle endpoint quering.
 *
 * @author Chris Bailey (c.bailey@bristol.ac.uk)
 */
public class ThreadedQueryHandler implements QueryHandlerInterface
{
    /**
     * The cache handler, autowired in.
     */
    @Autowired(required = false)
    private transient CacheHandlerInterface cacheHandler;

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(ThreadedQueryHandler.class);

    /**
     * How long the main thread should wait when checking for thread completion.
     */
    private static final long MAIN_THREAD_WAIT = 50;

    /**
     * Default length for results stringbuffer constructor.
     */
    private static final int DEFAULT_SB_LENGTH = 133;

    /**
     * Flag to indicate how many threads have completed their processing.
     */
    private transient int theadsCompleted;

    /**
     * Public accessor for cache (if present).
     * @return CacheHandler supplied by spring, or <code>null</code> otherwise
     */
    public final CacheHandlerInterface getCache()
    {
        return cacheHandler;
    }
    
    /**
     * Notifies the SimpleQueryHandler that another thread has finished processing.
     */
    public final void setCompleted()
    {
        synchronized (this)
        {
            theadsCompleted++;
        }
    }

    /**
     * A container for SPARQL SELECT results gathered by the threads.
     */
    private transient List<Result> resultList;

    /**
     * Adds a result.
     * @param r A single SPARQL SELECT result object.
     */
    public final void addResult(final Result r)
    {
        synchronized (this)
        {
            resultList.add(r);
        }
    }

    /**
     * This implementation, simple contatinates all query results.
     * @param queryString SPARQL query to execute
     * @param endpoints List of endpoint urls to run the query against
     * @return An RDF model
     */
    public final String handleQuery(final String queryString, final List<Endpoint> endpoints)
    {
        LOG.debug("Querying against  " + endpoints.size() + " endpoints");

        // process the SPARQL query to best determin how to handle this query
        Query query = QueryFactory.create(queryString);

        if (query.getQueryType() == Query.QueryTypeSelect)
        {
            // this is a simple select query. Results can be appended, and limited as required
            LOG.debug("Is a select query");
            return handleSelect(query, endpoints);

        } // if (query.getQueryType() == Query.QueryTypeSelect)
        else
        {
            LOG.warn("Unable to handle this query type");
        }

        return "";
    }

    /**
     * This method handles a SELECT SPARQL query.
     * It uses threads to query each endpoint and then combines the responses.
     * @param query SPARQL SELECT query
     * @param endpoints List of endpoints to query over
     * @return Response string
     */
    private String handleSelect(final Query query, final List<Endpoint> endpoints)
    {
        // reset conditions
        theadsCompleted = 0;
        resultList = new LinkedList<Result>();
        int totalThreads = endpoints.size();

        // fire off a thread to handle quering each endpoint
        for (Endpoint ep : endpoints)
        {
            String url = ep.getLocation();
            LOG.debug("Querying " + url);

            Thread t = new FetchSPARQLResponseThread(this, query, url);
            t.start();
        }

        // now wait for all thread to finish processing
        LOG.debug("Threads started, going to sleep");
        while (theadsCompleted != totalThreads)
        {
            try
            {
                Thread.sleep(MAIN_THREAD_WAIT);
            }
            catch (InterruptedException ex)
            {
                LOG.warn("Exception while waiting for threads to complete", ex);
            }
        }

        // once threads have compeleted, construct results
        LOG.debug("Threads completed, constructing results");

        StringBuffer content = new StringBuffer(DEFAULT_SB_LENGTH);

        content.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");

        // add head info
        List<String> vars = query.getResultVars();
        for (String var : vars)
        {
            content.append("<varaible name=\"");
            content.append(var);
            content.append("\"/>");
        }
        content.append("</head><results>");

        // collate all responses
        boolean hasLimit = false;
        long limit = -1;

        if (query.hasLimit())
        {
            limit = query.getLimit();
            hasLimit = true;
        }

        for (Result r : resultList)
        {
            if (!hasLimit || limit > 0)
            {
                if (hasLimit) limit--;
                content.append(r.toXML());
            }
        }

        content.append("</results></sparql>");

        return content.toString();
    }


    class FetchSPARQLResponseThread extends Thread
    {

        ThreadedQueryHandler handler;
        String url;
        Query query;

        FetchSPARQLResponseThread(final ThreadedQueryHandler handler, final Query query,  final String url)
        {
            this.handler = handler;
            this.query = query;
            this.url = url;
        }

        public void run()
        {
//            if (handler.cacheHandler != null)
//            {
//                String handler.cacheHandler.get(query+url);
//            }

            QueryEngineHTTP qehttp = QueryExecutionFactory.createServiceRequest(url, query);

            try
            {
                ResultSet resultSet = qehttp.execSelect();

                while (resultSet.hasNext())
                {
                    QuerySolution sol = resultSet.next();
                    Result r = new Result(sol);
                    handler.addResult(r);
                }
            }
            catch (QueryExceptionHTTP qhttpe)
            {
                LOG.error("Unable to execute query against " + url);
            }
            finally
            {
                qehttp.close();
                handler.setCompleted();
            }

            
        }
    }

}
