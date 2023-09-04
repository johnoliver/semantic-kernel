// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.samples.syntaxexamples;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.SamplesConfig;
import com.microsoft.semantickernel.connectors.ai.openai.util.SettingsMap;
import com.microsoft.semantickernel.connectors.websearch.WebSearchEngineSkill;
import com.microsoft.semantickernel.connectors.websearch.bing.BingConnector;
import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.samples.syntaxexamples.Example04_CombineLLMPromptsAndNativeCode.SearchEngineSkill;
import com.microsoft.semantickernel.textcompletion.TextCompletion;
import reactor.core.publisher.Mono;

public class Example07_BingAndGoogleSkills {

  public static void main(String[] args) throws ConfigurationException {

    OpenAIAsyncClient client = SamplesConfig.getClient();

    TextCompletion textCompletion = SKBuilders.chatCompletion()
        .withModelId("gpt-35-turbo")
        .withOpenAIClient(client)
        .build();

    Kernel kernel = SKBuilders.kernel().withDefaultAIService(textCompletion).build();

    kernel.importSkill(new SearchEngineSkill(), null);

    String bingApiKey = SettingsMap.getDefault().get("bing.apiKey");
    var bingConnector = new BingConnector(bingApiKey);
    var bing = new WebSearchEngineSkill(bingConnector);
    kernel.importSkill(bing, "bing");
    example1Async(kernel, "bing");
  }

  private static void example1Async(Kernel kernel, String searchSkillId) {
    System.out.println("======== Bing and Google Search Skill ========");

    // Run
    var question = "What's the largest building in the world?";
    Mono<SKContext> result = kernel.getFunction(searchSkillId, "search").invokeAsync(question);

    System.out.println(question);
    System.out.println(result.block().getResult());

  }
}
