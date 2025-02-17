// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel;

import com.microsoft.semantickernel.builders.BuildersSingleton;
import com.microsoft.semantickernel.exceptions.SkillsNotFoundException;
import com.microsoft.semantickernel.memory.SemanticTextMemory;
import com.microsoft.semantickernel.orchestration.ContextVariables;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.orchestration.SKFunction;
import com.microsoft.semantickernel.semanticfunctions.SemanticFunctionConfig;
import com.microsoft.semantickernel.skilldefinition.ReadOnlyFunctionCollection;
import com.microsoft.semantickernel.skilldefinition.ReadOnlySkillCollection;
import com.microsoft.semantickernel.templateengine.PromptTemplateEngine;
import com.microsoft.semantickernel.textcompletion.CompletionSKFunction;

import reactor.core.publisher.Mono;

import java.util.Map;

import javax.annotation.Nullable;

/** Interface for the semantic kernel. */
public interface Kernel {

    /**
     * Settings required to execute functions, including details about AI dependencies, e.g.
     * endpoints and API keys.
     */
    KernelConfig getConfig();

    /**
     * Reference to the engine rendering prompt templates
     *
     * @return
     */
    PromptTemplateEngine getPromptTemplateEngine();

    /**
     * Set the semantic memory to use.
     *
     * @param memory {@link SemanticTextMemory} instance
     */
    void registerMemory(SemanticTextMemory memory);

    /*
        /// <summary>
        /// Set the semantic memory to use
        /// </summary>
        /// <param name="memory">Semantic memory instance</param>
        void RegisterMemory(ISemanticTextMemory memory);
    */
    /**
     * Run a pipeline composed of synchronous and asynchronous functions.
     *
     * @param pipeline List of functions
     * @return Result of the function composition
     */
    Mono<SKContext<?>> runAsync(SKFunction... pipeline);

    /**
     * Run a pipeline composed of synchronous and asynchronous functions.
     *
     * @param input Input to process
     * @param pipeline List of functions
     * @return Result of the function composition
     */
    Mono<SKContext<?>> runAsync(String input, SKFunction... pipeline);

    /**
     * Run a pipeline composed of synchronous and asynchronous functions.
     *
     * @param variables variables to initialise the context with
     * @param pipeline List of functions
     * @return Result of the function composition
     */
    Mono<SKContext<?>> runAsync(ContextVariables variables, SKFunction... pipeline);

    /**
     * Import a set of skills
     *
     * @param skillName
     * @param skills
     * @return
     * @throws SkillsNotFoundException
     */
    ReadOnlyFunctionCollection importSkill(
            String skillName, Map<String, SemanticFunctionConfig> skills)
            throws SkillsNotFoundException;

    /**
     * Get function collection with the skill name
     *
     * @param skillName
     * @return
     * @throws SkillsNotFoundException
     */
    ReadOnlyFunctionCollection getSkill(String skillName) throws SkillsNotFoundException;

    /**
     * Imports the native functions annotated on the given object as a skill.
     *
     * @param nativeSkill
     * @param skillName
     * @return
     */
    ReadOnlyFunctionCollection importSkill(Object nativeSkill, @Nullable String skillName);

    /**
     * @return Reference to the read-only skill collection containing all the imported functions
     */
    ReadOnlySkillCollection getSkills();

    CompletionSKFunction.Builder getSemanticFunctionBuilder();

    /** Obtains the service with the given name and type */
    <T> T getService(@Nullable String name, Class<T> clazz) throws KernelException;

    /** Registers a semantic functon on this kernel */
    <
                    RequestConfiguration,
                    ContextType extends SKContext<ContextType>,
                    FunctionType extends SKFunction<RequestConfiguration, ContextType>>
            FunctionType registerSemanticFunction(FunctionType semanticFunctionDefinition);

    // <T extends ReadOnlySKContext<T>> T createNewContext();

    class Builder {
        @Nullable private KernelConfig kernelConfig = null;
        @Nullable private PromptTemplateEngine promptTemplateEngine = null;

        @Nullable private ReadOnlySkillCollection skillCollection = null;

        public Builder setKernelConfig(KernelConfig kernelConfig) {
            this.kernelConfig = kernelConfig;
            return this;
        }

        public Builder setPromptTemplateEngine(PromptTemplateEngine promptTemplateEngine) {
            this.promptTemplateEngine = promptTemplateEngine;
            return this;
        }

        public Builder setSkillCollection(@Nullable ReadOnlySkillCollection skillCollection) {
            this.skillCollection = skillCollection;
            return this;
        }

        public Kernel build() {
            if (kernelConfig == null) {
                throw new IllegalStateException("Must provide a kernel configuration");
            }

            return BuildersSingleton.INST
                    .getKernelBuilder()
                    .build(kernelConfig, promptTemplateEngine, skillCollection);
        }
    }

    interface InternalBuilder {
        Kernel build(
                KernelConfig kernelConfig,
                @Nullable PromptTemplateEngine promptTemplateEngine,
                @Nullable ReadOnlySkillCollection skillCollection);
    }
}
