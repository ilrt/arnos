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
package org.wf.arnos.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;
import static org.junit.Assert.*;
import org.springframework.ui.Model;
import org.wf.arnos.cachehandler.CacheHandlerInterface;
import org.wf.arnos.controller.model.Endpoint;
import org.wf.arnos.controller.model.Project;
import org.wf.arnos.controller.model.ProjectsManager;
import org.wf.arnos.exception.ResourceNotFoundException;
import org.wf.arnos.utils.Sparql;

/**
 *
 * @author Chris Bailey (c.bailey@bristol.ac.uk)
 */
public class EndpointControllerTest extends EasyMockSupport {
    EndpointController controller;
    static String PROJECT_NAME = "testproject";
    Project p;
    
    @Before
    public void setUp() throws IOException
    {
        DOMConfigurator.configure("./src/main/webapp/WEB-INF/log4j.xml");

        File f = File.createTempFile("jnuit_endpoint", "test");
        f.deleteOnExit();

        ProjectsManager manager = new ProjectsManager();
        manager.logger = LogFactory.getLog(ProjectsManager.class);
        manager.setFileName(f.getAbsolutePath());

        // setup default projects
        p = new Project(PROJECT_NAME);
        p.addEndpoint(Sparql.ENDPOINT1_URL);
        p.addEndpoint(Sparql.ENDPOINT2_URL);
        manager.addProject(p);

        controller = new EndpointController();
        controller.manager = manager;
        EndpointController.logger = LogFactory.getLog(EndpointController.class);
        Logger.getLogger("org.wf.arnos.controller.EndpointController").setLevel(Level.ALL);
    }

    @Test
    public void testListEndpoints()
    {
        assertEquals(2,getNumOfEndpoints());
    }

    @Test
    public void testAddEndpoint()
    {
        Model model = new ExtendedModelMap();
        assertEquals(2,getNumOfEndpoints());

        String result = controller.addEndpoint(PROJECT_NAME, Sparql.ENDPOINT3_URL, model);

        Endpoint p = new Endpoint(Sparql.ENDPOINT3_URL);
        String expectedResult = p.getIdentifier();

        // check result
        assertEquals(3,getNumOfEndpoints());
        assertEquals("",result);
        assertEquals(true,model.asMap().get("message").toString().contains(expectedResult));
    }

    @Test
    public void testRemoveEndpoint()
    {
        Model model = new ExtendedModelMap();
        
        assertEquals(2,getNumOfEndpoints());
        controller.removeEndpoint(PROJECT_NAME, Sparql.ENDPOINT1_URL, model);

        assertEquals(1,getNumOfEndpoints());
    }

    private int getNumOfEndpoints()
    {
        Model model = new ExtendedModelMap();
        assertEquals("",controller.listEndpoints(PROJECT_NAME, (Model)model));
        List<Endpoint> endpointList = (List<Endpoint>) model.asMap().get("endpoints");

        return endpointList.size();
    }

    @Test
    public void testEdgeCases()
    {
        Model model = new ExtendedModelMap();
        try
        {
            controller.listEndpoints(null, (Model)model);
            fail("ResourceNotFoundException not thrown");
        }
        catch (ResourceNotFoundException e)
        {
            // expected result
        }

        try
        {
            controller.listEndpoints(PROJECT_NAME+"missing", (Model)model);
            fail("ResourceNotFoundException not thrown");
        }
        catch (ResourceNotFoundException e)
        {
            // expected result
        }

        try
        {
            controller.listEndpoints(null, (Model)model);
            fail("ResourceNotFoundException not thrown");
        }
        catch (ResourceNotFoundException e)
        {
            // expected result
        }

        model = new ExtendedModelMap();
        assertEquals("",controller.addEndpoint(PROJECT_NAME, null, (Model)model));
        assertEquals("Missing endpoint",model.asMap().get("message"));

        model = new ExtendedModelMap();
        assertEquals("",controller.addEndpoint(PROJECT_NAME, "", (Model)model));
        assertEquals("Missing endpoint",model.asMap().get("message"));

        model = new ExtendedModelMap();
        assertEquals("",controller.removeEndpoint(PROJECT_NAME, "", (Model)model));
        assertEquals("Missing endpoint",model.asMap().get("message"));

        model = new ExtendedModelMap();
        assertEquals("",controller.removeEndpoint(PROJECT_NAME, "", (Model)model));
        assertEquals("Missing endpoint",model.asMap().get("message"));
    }

    @Test
    public void testDisableDebugMode()
    {
        System.out.println("testDisableDebugMode");

        org.apache.log4j.LogManager.getLogger("org.wf.arnos.controller.EndpointController").setLevel(org.apache.log4j.Level.OFF);
        Model model = new ExtendedModelMap();

        try
        {
            controller.listEndpoints("", model);
            fail("Exception not thrown");
        }
        catch (Exception e){}
        try
        {
            controller.listEndpoints(PROJECT_NAME+"missing", model);
            fail("Exception not thrown");
        }
        catch (Exception e){}
        controller.addEndpoint(PROJECT_NAME, Sparql.ENDPOINT1_URL, model);
        controller.removeEndpoint(PROJECT_NAME, Sparql.ENDPOINT1_URL, model);

        assertEquals(1,getNumOfEndpoints());
    }

    @Test
    public void testFlushEndpoint()
    {
        Model model = new ExtendedModelMap();
        Endpoint ep = new Endpoint(Sparql.ENDPOINT1_URL);

        replayAll();

        controller.flushEndpoint(PROJECT_NAME, ep.getLocation(), model);

        verifyAll();
        
        CacheHandlerInterface mockCache = createMock(CacheHandlerInterface.class);

        controller.cacheHandler = mockCache;

        mockCache.flush(PROJECT_NAME, ep);

        replayAll();

        controller.flushEndpoint(PROJECT_NAME, ep.getLocation(), model);

        verifyAll();

        resetAll();

        replayAll();

        controller.flushEndpoint(PROJECT_NAME, null, model);

        verifyAll();
    }
}