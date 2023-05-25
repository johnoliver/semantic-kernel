// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.connectors.ai.openai.chatcompletion;

import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.microsoft.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Verify;
import com.microsoft.semantickernel.ai.AIException;
import com.microsoft.semantickernel.chatcompletion.ChatCompletion;
import com.microsoft.semantickernel.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.chatcompletion.ChatRequestSettings;
import com.microsoft.semantickernel.connectors.ai.openai.azuresdk.ClientBase;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/** OpenAI chat completion client. */
public class OpenAIChatCompletion extends ClientBase implements ChatCompletion<OpenAIChatHistory> {
    public OpenAIChatCompletion(OpenAIAsyncClient client, String modelId) {
        super(client, modelId);
    }

    public static class Builder implements ChatCompletion.Builder {
        public Builder() {}

        public ChatCompletion<OpenAIChatHistory> build(OpenAIAsyncClient client, String modelId) {
            return new OpenAIChatCompletion(client, modelId);
        }
    }

    public Mono<String> generateMessageAsync(
            OpenAIChatHistory chat, @Nullable ChatRequestSettings requestSettings) {

        if (requestSettings == null) {
            requestSettings = new ChatRequestSettings();
        }
        return this.internalGenerateChatMessageAsync(chat, requestSettings);
    }

    /**
     * Generate a new chat message
     *
     * @param chat Chat history
     * @param requestSettings AI request settings
     * @return
     */
    private Mono<String> internalGenerateChatMessageAsync(
            ChatHistory chat, ChatRequestSettings requestSettings) {
        Verify.notNull(chat);
        Verify.notNull(requestSettings);

        validateMaxTokens(requestSettings.getMaxTokens());
        ChatCompletionsOptions options = createChatCompletionsOptions(requestSettings, chat);

        return getClient()
                .getChatCompletions(getModelId(), options)
                .flatMap(
                        response -> {
                            if (response == null || response.getChoices().isEmpty()) {
                                return Mono.error(
                                        new AIException(
                                                AIException.ErrorCodes.InvalidResponseContent,
                                                "Chat completions not found"));
                            } else {
                                return Mono.just(
                                        response.getChoices().get(0).getMessage().getContent());
                            }
                        });
    }

    private static ChatCompletionsOptions createChatCompletionsOptions(
            ChatRequestSettings requestSettings, ChatHistory chat) {
        List<ChatMessage> messages =
                chat.getMessages().stream()
                        .map(
                                it ->
                                        new ChatMessage(toChatRole(it.getAuthorRoles()))
                                                .setContent(it.getContent()))
                        .collect(Collectors.toList());

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);

        options.setMaxTokens(requestSettings.getMaxTokens());
        options.setTemperature(requestSettings.getTemperature());
        options.setTopP(requestSettings.getTopP());
        options.setFrequencyPenalty(requestSettings.getFrequencyPenalty());
        options.setPresencePenalty(requestSettings.getPresencePenalty());
        options.setN(1);

        return options;
    }

    private static ChatRole toChatRole(ChatHistory.AuthorRoles authorRoles) {
        switch (authorRoles) {
            case System:
                return ChatRole.SYSTEM;
            case User:
                return ChatRole.USER;
            case Assistant:
                return ChatRole.ASSISTANT;
            default:
                throw new IllegalArgumentException(
                        "Invalid chat message author: " + authorRoles.name());
        }
    }

    @Override
    public OpenAIChatHistory createNewChat(@Nullable String instructions) {
        return internalCreateNewChat(instructions);
    }

    /**
     * Create a new empty chat instance
     *
     * @param instructions Optional chat instructions for the AI service
     * @return Chat object
     */
    private static OpenAIChatHistory internalCreateNewChat(@Nullable String instructions) {
        if (instructions == null) {
            instructions = "";
        }
        return new OpenAIChatHistory(instructions);
    }
}
