// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.connectors.websearch.bing;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.Verify;
import com.microsoft.semantickernel.connectors.websearch.WebSearchEngineConnector;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/// <summary>
/// Bing API connector.
/// </summary>
public class BingConnector implements WebSearchEngineConnector {

    private final HttpClient httpClient;
    private final String apiKey;

    /// <summary>
    /// Initializes a new instance of the <see cref="BingConnector"/> class.
    /// </summary>
    /// <param name="apiKey">The API key to authenticate the connector.</param>
    /// <param name="logger">An optional logger to log connector-related information.</param>
    public BingConnector(String apiKey) {
        this(apiKey, HttpClient.createDefault());
    }

    public BingConnector(String apiKey, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    public Flux<String> searchAsync(String query, int count, int offset) {
        Flux<WebPage> pages = getWebPages(query, count, offset);
        return pages.map(it -> it.snippet);
    }

    private Flux<WebPage> getWebPages(String query, int count, int offset) {
        Flux<WebPage> pages;
        try {
            if (count <= 0) {
                throw new IllegalStateException("Count must be greater than 0.");
            }

            if (count >= 50) {
                throw new IllegalStateException("Count must be less than 50.");
            }

            if (offset < 0) {
                throw new IllegalArgumentException("Offset must be greater than or equal to 0.");
            }

            String q = URLEncoder.encode(query);
            URL uri =
                    new URL(
                            "https://api.bing.microsoft.com/v7.0/search?q="
                                    + q
                                    + "&count="
                                    + count
                                    + "&offset="
                                    + offset);

            pages =
                    this.sendGetRequest(uri)
                            .flatMap(
                                    response -> {
                                        if (response.getStatusCode() >= 400) {
                                            return Mono.error(
                                                    new RuntimeException(
                                                            "Error response received: "
                                                                    + response.getStatusCode()));
                                        }

                                        return response.getBodyAsString();
                                    })
                            .flatMapMany(
                                    json -> {
                                        try {
                                            BingSearchResponse data =
                                                    new ObjectMapper()
                                                            .readValue(
                                                                    json, BingSearchResponse.class);

                                            WebPage[] results = data.webPages.value;

                                            return Flux.fromArray(results);
                                        } catch (JsonProcessingException e) {
                                            return Flux.error(e);
                                        }
                                    });
        } catch (MalformedURLException e) {
            pages = Flux.error(e);
        }
        return pages;
    }

    @Override
    public Flux<String> searchUrlAsync(String query, int count, int offset) {
        Flux<WebPage> pages = getWebPages(query, count, offset);
        return pages.map(it -> it.url);
    }

    /// <summary>
    /// Sends a GET request to the specified URI.
    /// </summary>
    /// <param name="uri">The URI to send the request to.</param>
    /// <param name="cancellationToken">A cancellation token to cancel the request.</param>
    /// <returns>A <see cref="HttpResponseMessage"/> representing the response from the
    // request.</returns>
    private Mono<HttpResponse> sendGetRequest(URL uri) {
        HttpRequest httpRequestMessage = new HttpRequest(HttpMethod.GET, uri);

        if (!Verify.isNullOrEmpty(this.apiKey)) {
            httpRequestMessage.setHeader("Ocp-Apim-Subscription-Key", this.apiKey);
        }

        return httpClient.send(httpRequestMessage);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BingSearchResponse {

        private final WebPages webPages;

        public BingSearchResponse(@JsonProperty("webPages") WebPages webPages) {
            this.webPages = webPages;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WebPages {

        public final WebPage[] value;

        WebPages(@JsonProperty("value") WebPage[] value) {
            this.value = value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WebPage {

        public final String name;
        public final String url;
        public final String snippet;

        WebPage(
                @JsonProperty("name") String name,
                @JsonProperty("url") String url,
                @JsonProperty("snippet") String snippet) {
            this.name = name;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
