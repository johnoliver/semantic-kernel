// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.connectors.websearch;

import reactor.core.publisher.Flux;

/// <summary>
/// Web search engine connector interface.
/// </summary>
public interface WebSearchEngineConnector {

    /// <summary>
    /// Execute a web search engine search.
    /// </summary>
    /// <param name="query">Query to search.</param>
    /// <param name="count">Number of results.</param>
    /// <param name="offset">Number of results to skip.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for
    // cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>First snippet returned from search.</returns>
    Flux<String> searchAsync(String query, int count, int offset);

    /// <summary>
    /// Execute a web search engine search.
    /// </summary>
    /// <param name="query">Query to search.</param>
    /// <param name="count">Number of results.</param>
    /// <param name="offset">Number of results to skip.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for
    // cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Url returned from search.</returns>
    Flux<String> searchUrlAsync(String query, int count, int offset);
}
