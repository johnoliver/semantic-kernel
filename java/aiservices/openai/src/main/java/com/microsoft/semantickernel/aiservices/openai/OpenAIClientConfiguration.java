// Copyright (c) Microsoft Corporation. All rights reserved.
package com.microsoft.semantickernel.aiservices.openai;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.core.credential.KeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.util.ClientOptions;
import com.azure.core.util.Configuration;
import com.microsoft.semantickernel.services.AIServiceClientConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for creating a new instance of the OpenAIClient type.
 */
public class OpenAIClientConfiguration implements AIServiceClientConfiguration<OpenAIAsyncClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIClientConfiguration.class);

    private final List<HttpPipelinePolicy> pipelinePolicies;

    /*
     * The HTTP pipeline to send requests through.
     */
    private HttpPipeline pipeline;

    /*
     * The HTTP client used to send the request.
     */
    private HttpClient httpClient;

    /*
     * The logging configuration for HTTP requests and responses.
     */

    private HttpLogOptions httpLogOptions;

    /*
     * The client options such as application ID and custom headers to set on a request.
     */

    private ClientOptions clientOptions;

    /*
     * The retry options to configure retry policy for failed requests.
     */

    private RetryOptions retryOptions;

    /*
     * The configuration store that is used during construction of the service client.
     */

    private Configuration configuration;

    /*
     * The TokenCredential used for authentication.
     */

    private TokenCredential tokenCredential;

    /**
     * The KeyCredential used for OpenAi authentication. It could be either of Azure or Non-Azure
     * OpenAI API key.
     */
    private KeyCredential keyCredential;

    /*
     * The service endpoint
     */

    private String endpoint;

    /*
     * The retry policy that will attempt to retry failed requests, if applicable.
     */

    private RetryPolicy retryPolicy;

    /**
     * Create an instance of the OpenAIClientBuilder.
     */

    public OpenAIClientConfiguration() {
        this.pipelinePolicies = new ArrayList<>();
    }

    public OpenAIClientConfiguration pipeline(HttpPipeline pipeline) {
        if (this.pipeline != null && pipeline == null) {
            LOGGER.info("HttpPipeline is being set to 'null' when it was previously configured.");
        }
        this.pipeline = pipeline;
        return this;
    }

    public OpenAIClientConfiguration httpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public OpenAIClientConfiguration httpLogOptions(HttpLogOptions httpLogOptions) {
        this.httpLogOptions = httpLogOptions;
        return this;
    }

    public OpenAIClientConfiguration clientOptions(ClientOptions clientOptions) {
        this.clientOptions = clientOptions;
        return this;
    }

    public OpenAIClientConfiguration retryOptions(RetryOptions retryOptions) {
        this.retryOptions = retryOptions;
        return this;
    }

    public OpenAIClientConfiguration addPolicy(HttpPipelinePolicy customPolicy) {
        Objects.requireNonNull(customPolicy, "'customPolicy' cannot be null.");
        pipelinePolicies.add(customPolicy);
        return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenAIClientConfiguration configuration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public OpenAIClientConfiguration credential(TokenCredential tokenCredential) {
        this.tokenCredential = tokenCredential;
        return this;
    }

    public OpenAIClientConfiguration credential(KeyCredential keyCredential) {
        this.keyCredential = keyCredential;
        return this;
    }

    public OpenAIClientConfiguration endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Sets The retry policy that will attempt to retry failed requests, if applicable.
     *
     * @param retryPolicy the retryPolicy value.
     * @return the OpenAIClientBuilder.
     */

    public OpenAIClientConfiguration retryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    /**
     * Builds an instance of OpenAIAsyncClient class.
     *
     * @return an instance of OpenAIAsyncClient.
     */
    @Override
    public OpenAIAsyncClient buildAsyncClient() {
        com.azure.ai.openai.OpenAIClientBuilder builder = new com.azure.ai.openai.OpenAIClientBuilder();
        pipelinePolicies.forEach(builder::addPolicy);

        if (pipeline != null) {
            builder.pipeline(pipeline);
        }

        if (httpClient != null) {
            builder.httpClient(httpClient);
        }

        if (httpLogOptions != null) {
            builder.httpLogOptions(httpLogOptions);
        }

        if (clientOptions != null) {
            builder.clientOptions(clientOptions);
        }

        if (retryOptions != null) {
            builder.retryOptions(retryOptions);
        }

        if (configuration != null) {
            builder.configuration(configuration);
        }

        if (tokenCredential != null) {
            builder.credential(tokenCredential);
        }

        if (keyCredential != null) {
            builder.credential(keyCredential);
        }

        if (endpoint != null) {
            builder.endpoint(endpoint);
        }

        if (retryPolicy != null) {
            builder.retryPolicy(retryPolicy);
        }

        return builder.buildAsyncClient();
    }
}
