// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.semanticfunctions; // Copyright (c) Microsoft. All rights
// reserved.

import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.skilldefinition.ParameterView;
import com.microsoft.semantickernel.templateengine.PromptTemplateEngine;

import reactor.core.publisher.Mono;

import java.util.List;

/** Interface for prompt template */
public interface PromptTemplate {
    /**
     * Get the list of parameters required by the template, using configuration and template info
     *
     * @return List of parameters
     */
    List<ParameterView> getParameters();

    /**
     * Render the template using the information in the context
     *
     * @param executionContext Kernel execution context helpers
     * @param promptTemplateEngine
     * @return Prompt rendered to string
     */
    Mono<String> renderAsync(SKContext executionContext, PromptTemplateEngine promptTemplateEngine);

    interface Builder {
        PromptTemplate build(String promptTemplate, PromptTemplateConfig config);
    }
}
