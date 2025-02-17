// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.connectors.ai.openai.textcompletion;

import com.azure.ai.openai.models.Choice;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.microsoft.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.ai.AIException;
import com.microsoft.semantickernel.connectors.ai.openai.azuresdk.ClientBase;
import com.microsoft.semantickernel.textcompletion.CompletionRequestSettings;
import com.microsoft.semantickernel.textcompletion.TextCompletion;

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/// <summary>
/// OpenAI text completion service.
/// TODO: forward ETW logging to ILogger, see
// https://learn.microsoft.com/en-us/dotnet/azure/sdk/logging
/// </summary>
public class OpenAITextCompletion extends ClientBase implements TextCompletion {
    /// <summary>
    /// Create an instance of the OpenAI text completion connector
    /// </summary>
    /// <param name="modelId">Model name</param>
    /// <param name="apiKey">OpenAI API Key</param>
    /// <param name="organization">OpenAI Organization Id (usually optional)</param>
    /// <param name="handlerFactory">Retry handler factory for HTTP requests.</param>
    /// <param name="log">Application logger</param>
    public OpenAITextCompletion(OpenAIAsyncClient client, String modelId) {
        super(client, modelId);
    }

    @Override
    public Mono<List<String>> completeAsync(
            String text, CompletionRequestSettings requestSettings) {
        return this.internalCompleteTextAsync(text, requestSettings);
    }

    protected Mono<List<String>> internalCompleteTextAsync(
            String text, CompletionRequestSettings requestSettings) {
        // TODO

        if (requestSettings.getMaxTokens() < 1) {
            throw new AIException(AIException.ErrorCodes.InvalidRequest, "Max tokens must be >0");
        }

        CompletionsOptions completionsOptions =
                new CompletionsOptions(Collections.singletonList(text))
                        .setMaxTokens(requestSettings.getMaxTokens())
                        .setTemperature(requestSettings.getTemperature())
                        .setTopP(requestSettings.getTopP())
                        .setFrequencyPenalty(requestSettings.getFrequencyPenalty())
                        .setPresencePenalty(requestSettings.getPresencePenalty())
                        .setModel(getModelId())
                        .setUser(null);

        return getClient()
                .getCompletions(getModelId(), completionsOptions)
                .flatMapIterable(Completions::getChoices)
                .mapNotNull(Choice::getText)
                .collectList();
    }
}
