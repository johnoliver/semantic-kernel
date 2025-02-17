// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel;

import com.azure.ai.openai.models.Choice;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.microsoft.openai.AzureOpenAIClient;
import com.microsoft.semantickernel.builders.SKBuilders;
import com.microsoft.semantickernel.connectors.ai.openai.textcompletion.OpenAITextCompletion;
import com.microsoft.semantickernel.extensions.KernelExtensions;
import com.microsoft.semantickernel.orchestration.ContextVariables;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.semanticfunctions.PromptTemplateConfig;
import com.microsoft.semantickernel.textcompletion.CompletionSKContext;
import com.microsoft.semantickernel.textcompletion.CompletionSKFunction;
import com.microsoft.semantickernel.textcompletion.TextCompletion;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultKernelTest {

    @Test
    void contextVariableTest() {
        String model = "a-model";

        List<Tuple2<String, String>> responses =
                Arrays.asList(Tuples.of("A", "a"), Tuples.of("user: Aauser: B", "b"));

        com.azure.ai.openai.OpenAIAsyncClient client = mockCompletionOpenAIAsyncClient(responses);

        Kernel kernel = buildKernel(model, client);

        String prompt = "{{$history}}user: {{$user_input}}\n";

        CompletionSKFunction chat =
                kernel.getSemanticFunctionBuilder()
                        .createFunction(
                                prompt,
                                "ChatBot",
                                null,
                                null,
                                new PromptTemplateConfig.CompletionConfig(
                                        0.7, 0.5, 0, 0, 2000, new ArrayList<>()));

        ContextVariables variables = SKBuilders.variables().build();

        CompletionSKContext readOnlySkContext =
                chat.buildContext(variables, null, null)
                        .setVariable("history", "")
                        .setVariable("user_input", "A");

        CompletionSKContext result = chat.invokeAsync(readOnlySkContext, null).block();

        if (result == null) {
            Assertions.fail();
        }

        Assertions.assertEquals("a", result.getResult());

        result =
                result.appendToVariable("history", "user: A" + result.getResult())
                        .setVariable("user_input", "B");

        result = chat.invokeAsync(result, null).block();
        if (result == null) {
            Assertions.fail();
        }
        Assertions.assertEquals("b", result.getResult());
    }

    @Test
    void tellAJoke() {
        String expectedResponse = "a result joke";
        com.azure.ai.openai.OpenAIAsyncClient client =
                mockCompletionOpenAIAsyncClient("WRITE", expectedResponse);
        String model = "a-model-name";
        Kernel kernel = buildKernel(model, client);

        CompletionSKFunction function =
                kernel.importSkill(
                                "FunSkill",
                                KernelExtensions.importSemanticSkillFromDirectory(
                                        "../../samples/skills", "FunSkill"))
                        .getFunction("joke", CompletionSKFunction.class);

        Mono<CompletionSKContext> mono = function.invokeAsync("time travel to dinosaur age");
        CompletionSKContext result = mono.block();

        String expected =
                "WRITE EXACTLY ONE JOKE or HUMOROUS STORY ABOUT THE TOPIC BELOW\n"
                        + "\n"
                        + "JOKE MUST BE:\n"
                        + "- G RATED\n"
                        + "- WORKPLACE/FAMILY SAFE\n"
                        + "NO SEXISM, RACISM OR OTHER BIAS/BIGOTRY\n"
                        + "\n"
                        + "BE CREATIVE AND FUNNY. I WANT TO LAUGH.\n"
                        + "\n"
                        + "+++++\n"
                        + "\n"
                        + "time travel to dinosaur age\n"
                        + "+++++\n";

        assertCompletionsWasCalledWithModelAndText(client, model, expected);

        assertTheResultEquals(result, expectedResponse);
    }

    public static Kernel buildKernel(
            String model, com.azure.ai.openai.OpenAIAsyncClient openAIAsyncClient) {

        com.microsoft.openai.OpenAIAsyncClient client = new AzureOpenAIClient(openAIAsyncClient);

        TextCompletion textCompletion = new OpenAITextCompletion(client, model);

        KernelConfig kernelConfig =
                SKBuilders.kernelConfig()
                        .addTextCompletionService(model, kernel -> textCompletion)
                        .build();

        return SKBuilders.kernel().setKernelConfig(kernelConfig).build();
    }

    private static com.azure.ai.openai.OpenAIAsyncClient mockCompletionOpenAIAsyncClient(
            String arg, String response) {
        List<Tuple2<String, String>> responses =
                Collections.singletonList(Tuples.of(arg, response));

        return mockCompletionOpenAIAsyncClient(responses);
    }

    /*
         Mocks a Text Completion client where if the prompt matches it will return the first arg it will return the response,
         i.e:

           mockCompletionOpenAIAsyncClient(
             List.<>of(
                     Tuples.of("Tell me a joke", "This is a joke")
             )
           );

          This if the client is prompted with "Tell me a joke", the mocked client would respond with "This is a joke"
    */
    public static com.azure.ai.openai.OpenAIAsyncClient mockCompletionOpenAIAsyncClient(
            List<Tuple2<String, String>> responses) {
        com.azure.ai.openai.OpenAIAsyncClient openAIAsyncClient =
                Mockito.mock(com.azure.ai.openai.OpenAIAsyncClient.class);

        for (Tuple2<String, String> response : responses) {

            Choice choice = Mockito.mock(Choice.class);
            Mockito.when(choice.getText()).thenReturn(response.getT2());

            Completions completions = Mockito.mock(Completions.class);

            Mockito.when(completions.getChoices()).thenReturn(Collections.singletonList(choice));

            Mockito.when(
                            openAIAsyncClient.getCompletions(
                                    Mockito.any(String.class),
                                    Mockito.<CompletionsOptions>argThat(
                                            it ->
                                                    it.getPrompt()
                                                            .get(0)
                                                            .contains(response.getT1()))))
                    .thenReturn(Mono.just(completions));
        }
        return openAIAsyncClient;
    }

    @Test
    void inlineFunctionTest() {

        String model = "a-model";

        String expectedResponse = "foo";
        com.azure.ai.openai.OpenAIAsyncClient openAIAsyncClient =
                mockCompletionOpenAIAsyncClient("block", expectedResponse);

        Kernel kernel = buildKernel(model, openAIAsyncClient);

        String text = "A block of text\n";

        String prompt = "{{$input}}\n" + "Summarize the content above.";

        CompletionSKFunction summarize =
                SKBuilders.completionFunctions()
                        .createFunction(
                                prompt,
                                "summarize",
                                null,
                                null,
                                new PromptTemplateConfig.CompletionConfig(
                                        0.2, 0.5, 0, 0, 2000, new ArrayList<>()));
        kernel.registerSemanticFunction(summarize);

        Mono<CompletionSKContext> mono = summarize.invokeAsync(text);
        CompletionSKContext result = mono.block();

        String expected = "A block of text\n\nSummarize the content above.";

        assertCompletionsWasCalledWithModelAndText(openAIAsyncClient, model, expected);
        assertTheResultEquals(result, expectedResponse);
    }

    private void assertTheResultEquals(SKContext result, String expected) {
        Assertions.assertEquals(expected, result.getResult());
    }

    private static void assertCompletionsWasCalledWithModelAndText(
            com.azure.ai.openai.OpenAIAsyncClient openAIAsyncClient,
            String model,
            String expected) {
        Mockito.verify(openAIAsyncClient, Mockito.times(1))
                .getCompletions(
                        Mockito.matches(model),
                        Mockito.<CompletionsOptions>argThat(
                                completionsOptions ->
                                        completionsOptions.getPrompt().size() == 1
                                                && completionsOptions
                                                        .getPrompt()
                                                        .get(0)
                                                        .equals(expected)));
    }
}
