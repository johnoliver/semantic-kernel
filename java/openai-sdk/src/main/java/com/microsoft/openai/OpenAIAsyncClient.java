// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai; // Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

import com.azure.ai.openai.models.*;

import reactor.core.publisher.Mono;

/** Interface for an OpenAI client */
public interface OpenAIAsyncClient {
    Mono<Embeddings> getEmbeddings(String deploymentId, EmbeddingsOptions embeddingsOptions);

    Mono<Completions> getCompletions(String deploymentId, CompletionsOptions completionsOptions);

    Mono<ChatCompletions> getChatCompletions(
            String deploymentId, ChatCompletionsOptions chatCompletionsOptions);
}
