package com.microsoft.semantickernel.e2e;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.connectors.ai.openai.textcompletion.OpenAITextCompletion;
import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.memory.VolatileMemoryStore;
import com.microsoft.semantickernel.semanticfunctions.PromptTemplateConfig;
import com.microsoft.semantickernel.textcompletion.CompletionSKFunction;
import com.microsoft.semantickernel.textcompletion.CompletionType;
import com.microsoft.semantickernel.textcompletion.TextCompletion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullWireMockTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(FullWireMockTest.class);
  public static final int PORT = 8089;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(PORT);

  public static Kernel buildTextCompletionKernel() throws ConfigurationException {
    String model = "text-davinci-003";

    final OpenAIAsyncClient openAIClient = new OpenAIClientBuilder()
        .endpoint("http://localhost:" + PORT)
        .buildAsyncClient();

    TextCompletion textCompletion = new OpenAITextCompletion(openAIClient, model,
        CompletionType.NON_STREAMING);

    return SKBuilders.kernel()
        .withDefaultAIService(textCompletion)
        .withDefaultAIService(
            SKBuilders.textEmbeddingGeneration()
                .withOpenAIClient(openAIClient)
                .withModelId(model)
                .build())
        .withMemoryStorage(new VolatileMemoryStore())
        .build();
  }

  public void mockResponse() {
    String body = """
        {
          "id": "an-id",
          "object": "text_completion",
          "created": 1627985189,
          "model": "text-davinci-003",
          "choices": [
            {
              "text": "Summarised text",
              "index": 0,
              "logprobs": null,
              "finish_reason": "length"
            }
          ],
          "usage": {
            "prompt_tokens": 1,
            "completion_tokens": 1,
            "total_tokens": 2
          }
        }
        """;

    WireMock.stubFor(WireMock
        .post(new UrlPathPattern(
            new RegexPattern("/openai/deployments/text-davinci-003/completions"), true))
        .willReturn(WireMock.ok()
            .withBody(body)));
  }

  @Test
  public void runMockedTest() throws ConfigurationException {
    mockResponse();

    Kernel kernel = buildTextCompletionKernel();
    String prompt = "{{$input}}\nSummarize the content above.";

    CompletionSKFunction summarize =
        kernel.getSemanticFunctionBuilder()
            .withPromptTemplate(prompt)
            .withFunctionName("summarize")
            .withCompletionConfig(
                new PromptTemplateConfig.CompletionConfig(0.2, 0.5, 0, 0, 2000))
            .build();

    String text = "Some Text to summarize";

    String result = summarize.invokeAsync(text).block().getResult();

    Assertions.assertEquals("Summarised text", result);

    WireMock.verify(1, WireMock.postRequestedFor(
        WireMock.urlMatching("/openai/deployments/text-davinci-003/completions.*")));
  }

}
