// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.syntaxexamples;

import static com.microsoft.semantickernel.DefaultKernelTest.mockCompletionOpenAIAsyncClient;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.builders.SKBuilders;
import com.microsoft.semantickernel.extensions.KernelExtensions;
import com.microsoft.semantickernel.planner.actionplanner.ActionPlanner;
import com.microsoft.semantickernel.planner.actionplanner.Plan;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import reactor.util.function.Tuples;

public class Example28ActionPlanner {
    @Test
    public void runActionPlan() {

        OpenAIAsyncClient client =
                mockCompletionOpenAIAsyncClient(
                        Tuples.of("Generate a short funny", "A-RESULT"),
                        Tuples.of(
                                "plan",
                                "{\"plan\":{\n"
                                    + "\"rationale\": \"the list contains a function that can turn"
                                    + " a scenario into a short and entertaining poem\",\n"
                                    + "\"function\": \"WriterSkill.ShortPoem\",\n"
                                    + "\"parameters\": {\n"
                                    + "\"input\": \"Cleopatra\"\n"
                                    + "}}}"));

        Kernel kernel =
                SKBuilders.kernel()
                        .setKernelConfig(
                                SKBuilders.kernelConfig()
                                        .addTextCompletionService(
                                                "text-davinci-002",
                                                kernel1 ->
                                                        SKBuilders.textCompletionService()
                                                                .build(client, "text-davinci-002"))
                                        .build())
                        .build();

        kernel.importSkill(
                "SummarizeSkill",
                KernelExtensions.importSemanticSkillFromDirectory(
                        "../../samples/skills", "SummarizeSkill"));

        kernel.importSkill(
                "WriterSkill",
                KernelExtensions.importSemanticSkillFromDirectory(
                        "../../samples/skills", "WriterSkill"));

        // Create an instance of ActionPlanner.
        // The ActionPlanner takes one goal and returns a single function to execute.
        ActionPlanner planner = new ActionPlanner(kernel, null);

        // We're going to ask the planner to find a function to achieve this goal.
        String goal = "Write a poem about Cleopatra.";

        // The planner returns a plan, consisting of a single function
        // to execute and achieve the goal requested.
        Plan plan = planner.createPlanAsync(goal).block();

        // Show the result, which should match the given goal
        String planResult = plan.invokeAsync(goal).block().getResult();

        Assertions.assertEquals("A-RESULT", planResult);
    }
}
