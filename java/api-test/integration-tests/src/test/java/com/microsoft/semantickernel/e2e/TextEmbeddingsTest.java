package com.microsoft.semantickernel.e2e;// Copyright (c) Microsoft. All rights reserved.

import com.microsoft.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.KernelConfig;
import com.microsoft.semantickernel.ai.embeddings.EmbeddingGeneration;
import com.microsoft.semantickernel.builders.SKBuilders;
import com.microsoft.semantickernel.connectors.ai.openai.textembeddings.OpenAITextEmbeddingGeneration;
import com.microsoft.semantickernel.coreskills.TextMemorySkill;
import com.microsoft.semantickernel.memory.VolatileMemoryStore;
import com.microsoft.semantickernel.textcompletion.CompletionSKContext;
import com.microsoft.semantickernel.textcompletion.CompletionSKFunction;
import com.microsoft.semantickernel.textcompletion.TextCompletion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextEmbeddingsTest extends AbstractKernelTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextEmbeddingsTest.class);
    private static final int EXPECTED_EMBEDDING_SIZE = 1536;

    @Test
    @EnabledIf("isOpenAIComTestEnabled")
    public void testEmbeddingGenerationOpenAI() throws IOException {
        testEmbeddingGeneration(getOpenAIAPI(), EXPECTED_EMBEDDING_SIZE);
    }

    @Test
    @EnabledIf("isAzureTestEnabled")
    public void testEmbeddingGenerationAzure() throws IOException {
        testEmbeddingGeneration(getAzureOpenAIAPI(), EXPECTED_EMBEDDING_SIZE);
    }

    @Test
    @EnabledIf("isAzureTestEnabled")
    public void testEmbeddingGeneration() throws IOException {
        String model = "text-embedding-ada-002";
        EmbeddingGeneration<String, Double> embeddingGeneration =
                new OpenAITextEmbeddingGeneration(getOpenAIAPI(), model);

        List<String> data = new ArrayList<>();
        data.add("This is just a test");

        LOGGER.info(String.valueOf(embeddingGeneration.generateEmbeddingsAsync(data).block()));
    }

    @Test
    @EnabledIf("isAzureTestEnabled")
    public void testMemory() throws IOException {

        Kernel kernel = buildTextEmbeddingsKernel();

        kernel.importSkill(new TextMemorySkill(), null);

        String skPrompt =
                "\n"
                        + "ChatBot can have a conversation with you about any topic.\n"
                        + "It can give explicit instructions or say 'I don't know' if it does not have"
                        + " an answer.\n"
                        + "\n"
                        + "Information about me, from previous conversations:\n"
                        + "- {{$fact1}} {{recall $fact1}}\n"
                        + "- {{$fact2}} {{recall $fact2}}\n"
                        + "- {{$fact3}} {{recall $fact3}}\n"
                        + "- {{$fact4}} {{recall $fact4}}\n"
                        + "- {{$fact5}} {{recall $fact5}}\n"
                        + "\n"
                        + "Chat:\n"
                        + "{{$history}}\n"
                        + "User: {{$input}}\n"
                        + "ChatBot: ";


        CompletionSKFunction chat =
                SKBuilders.completionFunctions()
                        .createFunction(
                                skPrompt,
                                null,
                                null,
                                null,
                                2000,
                                0.2,
                                0.5,
                                0,
                                0,
                                new ArrayList<>())
                        .registerOnKernel(kernel);

        CompletionSKContext context = chat.buildContext(
                        SKBuilders.variables().build(),
                        kernel.getMemory(),
                        () -> kernel.getSkillCollection()
                )
                .setVariable("fact1", "what is my name?")
                .setVariable("fact2", "where do I live?")
                .setVariable("fact3", "where's my family from?")
                .setVariable("fact4", "where have I traveled?")
                .setVariable("fact5", "what do I do for work?")
                .setVariable("chat_history", "");

        context.getSemanticMemory()
                .saveInformationAsync("aboutMe", "My name is Andrea", "info1", null, null)
                .block();

        context.getSemanticMemory()
                .saveInformationAsync("aboutMe", "I currently work as a tour guide", "info2", null, null)
                .block();

        context.getSemanticMemory()
                .saveInformationAsync("aboutMe", "I've been living in Seattle since 2005", "info3", null, null)
                .block();

        context.getSemanticMemory()
                .saveInformationAsync("aboutMe", "I visited France and Italy five times since 2015", "info4", null, null)
                .block();

        context.getSemanticMemory()
                .saveInformationAsync("aboutMe", "My family is from New York", "info5", null, null)
                .block();

        CompletionSKContext result = chat.invokeAsync("where is Andreas family from?", context, null).block();

        System.out.println(result.getResult());
    }

    public void testEmbeddingGeneration(OpenAIAsyncClient client, int expectedEmbeddingSize) {
        String model = "text-embedding-ada-002";
        EmbeddingGeneration<String, Double> embeddingGeneration = new OpenAITextEmbeddingGeneration(client, model);

        List<String> data = new ArrayList<>();
        data.add("This is just");
        data.add("a test");

        embeddingGeneration
                .generateEmbeddingsAsync(data)
                .block()
                .forEach(
                        embedding -> {
                            Assertions.assertEquals(
                                    expectedEmbeddingSize, embedding.getVector().size());
                        });
    }

    private Kernel buildTextEmbeddingsKernel() throws IOException {
        String model = "text-embedding-ada-002";
        OpenAIAsyncClient client = getAzureOpenAIAPI();
        EmbeddingGeneration<String, Double> embeddingGeneration =
                new OpenAITextEmbeddingGeneration(client, model);

        TextCompletion textcompletion = SKBuilders
                .textCompletionService()
                .build(client, "text-davinci-003");

        KernelConfig kernelConfig =
                SKBuilders.kernelConfig()
                        .addTextEmbeddingsGenerationService(model, embeddingGeneration)
                        .addTextCompletionService("text-davinci-003", (kernel) -> textcompletion)
                        .build();

        return SKBuilders.kernel()
                .setKernelConfig(kernelConfig)
                .withMemoryStorage(new VolatileMemoryStore())
                .build();
    }
}
