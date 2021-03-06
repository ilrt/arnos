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
package org.wf.arnos.cachehandler;

import java.util.List;
import org.wf.arnos.controller.model.Endpoint;

/**
 * Provides a caching mechanism for the arnos application.
 * @author Chris Bailey (c.bailey@bristol.ac.uk)
 */
public interface CacheHandlerInterface
{
    /**
     * Adds an entry to the cache.
     * @param project Project name
     * @param e List of associated endpoints
     * @param key Identifier for cache
     * @param value Response to cache
     */
    void put(String project, List<Endpoint> e, String key, String value);

    /**
     * Gets a response from the cache.
     * @param project Project name
     * @param key Lookup key
     * @return Cached value, or null if missing
     */
    String get(String project, String key);

    /**
     * Checks for the existance of a cache object.
     * @param project Project name
     * @param key Lookup key
     * @return <code>true</code> if cache exists, <code>false</code> otherwise
     */
    boolean contains(String project, String key);

    /**
     * Remove a specific key from the cache.
     * @param project Project name
     * @param key Cache key
     */
    void flush(String project, String key);

    /**
     * Flush all caches associated with the specified project and endpoint
     * @param project Project name
     * @param e Endpoint
     */
    void flush(String project, Endpoint e);

    /**
     * Remove all keys from the cache for a given project
     * @param project Project name
     */
    void flushAll(String project);
}
