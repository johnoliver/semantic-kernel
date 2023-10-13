// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.connectors.websearch;

import com.microsoft.semantickernel.skilldefinition.annotations.DefineSKFunction;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionParameters;
import reactor.core.publisher.Mono;

/// <summary>
/// Web search engine skill (e.g. Bing)
/// </summary>
public class WebSearchEngineSkill {

    private final WebSearchEngineConnector connector;

    public WebSearchEngineSkill(WebSearchEngineConnector connector) {
        this.connector = connector;
    }

    @DefineSKFunction(description = "Perform a web search.", name = "search")
    public Mono<String> search(
            @SKFunctionParameters(description = "Text to search for", name = "input") String query,
            @SKFunctionParameters(
                            description = "Number of results",
                            name = "count",
                            defaultValue = "1",
                            type = Integer.class)
                    int count,
            @SKFunctionParameters(
                            description = "Number of results to skip",
                            name = "offset",
                            defaultValue = "0",
                            type = Integer.class)
                    int offset) {
        return connector
                .searchAsync(query, count, offset)
                .switchIfEmpty(
                        Mono.error(
                                new RuntimeException(
                                        "Failed to get a response from the web search engine.")))
                .single();
    }
}
