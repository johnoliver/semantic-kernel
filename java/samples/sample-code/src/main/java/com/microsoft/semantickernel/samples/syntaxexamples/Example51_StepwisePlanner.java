// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.samples.syntaxexamples;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.SamplesConfig;
import com.microsoft.semantickernel.coreskills.TimeSkill;
import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.planner.stepwiseplanner.DefaultStepwisePlanner;
import com.microsoft.semantickernel.planner.stepwiseplanner.StepwisePlanner;
import com.microsoft.semantickernel.skilldefinition.annotations.DefineSKFunction;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionParameters;
import com.microsoft.semantickernel.textcompletion.TextCompletion;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * This example shows how to use Stepwise Planner to create a plan for a given goal.
 */
public class Example51_StepwisePlanner {

    public static class WebSearchEngineSkill {
        @DefineSKFunction(description = "Perform a web search.", name = "search")
        public Mono<String> search(
                @SKFunctionParameters(description = "Text to search for", name = "query")
                String input,
                @SKFunctionParameters(description = "Number of results", name = "count", defaultValue = "1", type = Integer.class)
                Integer count,
                @SKFunctionParameters(description = "Number of results to skip", name = "offset", defaultValue = "0", type = Integer.class)
                Integer offset) {

            if (input.toLowerCase(Locale.ROOT).contains("joe biden")) {
                return Mono.just("Joe Biden is 80 years old.");
            }

            return Mono.just("The current president of the United States is Joe Biden.");
        }
    }

    public static class AdvancedCalculator {
        @DefineSKFunction(description = "Useful for getting the result of a non-trivial math expression.", name = "Calculator")
        public Mono<String> Calculator(
                @SKFunctionParameters(description = "A valid mathematical expression that could be executed by a calculator capable of more advanced math functions like sin/cosine/floor.", name = "input")
                String input) {
            return Mono.just("40");
        }
    }

    public static void main(String[] args) throws ConfigurationException {

        String[] questions = new String[]{
                "Who is the current president of the United States? What is his current age divided by 2",
        };

        for (String question : questions) {
            System.out.println("Result: " + runChatCompletion(question).block().getResult());

            // Currently due to token limits, not supporting text completion
            //System.out.println("Result: " + runTextCompletion(question).block().getResult());
        }

    }

    private static Mono<SKContext> runTextCompletion(String question) throws ConfigurationException {
        System.out.println("RunTextCompletion");
        var kernel = getKernel(false);
        return runWithQuestion(kernel, question);
    }

    private static Mono<SKContext> runChatCompletion(String question) throws ConfigurationException {
        System.out.println("RunChatCompletion");
        var kernel = getKernel(true);
        return runWithQuestion(kernel, question);
    }

    private static Mono<SKContext> runWithQuestion(Kernel kernel, String question) {
        var webSearchEngineSkill = new WebSearchEngineSkill();

        kernel.importSkill(webSearchEngineSkill, "WebSearch");
        kernel.importSkill(new AdvancedCalculator(), "advancedCalculator");
        kernel.importSkill(new TimeSkill(), "time");

        System.out.println("*****************************************************");
        System.out.println("Question: " + question);

        StepwisePlanner planner = new DefaultStepwisePlanner(kernel, null, null, null);

        var plan = planner.createPlan(question);

        return plan.invokeAsync(SKBuilders.context().withKernel(kernel).build());
    }

    private static Kernel getKernel(boolean useChat) throws ConfigurationException {
        OpenAIAsyncClient client = SamplesConfig.getClient();
        TextCompletion textCompletion;

        if (useChat) {
            textCompletion = SKBuilders.chatCompletion()
                    .withOpenAIClient(client)
                    .setModelId("gpt-35-turbo")
                    .build();

        } else {
            textCompletion = SKBuilders.textCompletionService()
                    .withOpenAIClient(client)
                    .setModelId("text-davinci-003")
                    .build();
        }

        return SKBuilders.kernel()
                .withDefaultAIService(textCompletion)
                .build();
    }
}