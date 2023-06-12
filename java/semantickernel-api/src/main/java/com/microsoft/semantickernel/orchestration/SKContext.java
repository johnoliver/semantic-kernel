// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.orchestration;

import com.microsoft.semantickernel.memory.SemanticTextMemory;
import com.microsoft.semantickernel.skilldefinition.ReadOnlySkillCollection;

import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import javax.annotation.CheckReturnValue;

/** Semantic Kernel context. */
public interface SKContext {

    SKContext build(
            ContextVariables variables,
            @Nullable SemanticTextMemory memory,
            @Nullable ReadOnlySkillCollection skills);

    /**
     * Obtain the result of the execution that produced this context. This will be the "input" entry
     * in the variables.
     *
     * @return the "input" entry in the variables
     */
    @Nullable
    String getResult();

    /**
     * Return a copy of all variables within the context
     *
     * @return a clone of the variables
     */
    ContextVariables getVariables();

    /** Provides access to the contexts semantic memory */
    @Nullable
    SemanticTextMemory getSemanticMemory();

    /**
     * Provides access to the skills within this context
     *
     * @return
     */
    ReadOnlySkillCollection getSkills();

    /**
     * Sets the given variable
     *
     * @param key if null defaults to the "input" key
     * @param content
     * @return Context for fluent calls
     */
    SKContext setVariable(@NonNull String key, @NonNull String content);

    /**
     * Appends data to the given key
     *
     * @param key
     * @param content
     * @return Context for fluent calls
     */
    SKContext appendToVariable(@NonNull String key, @NonNull String content);

    /**
     * Updates the input entry with the given data
     *
     * @param content
     * @return Context for fluent calls
     */
    @CheckReturnValue
    SKContext update(@NonNull String content);

    /**
     * Merges in the given variables. Duplicate keys provided by newData will overwrite existing
     * entries.
     *
     * @param newData
     * @return Context for fluent calls
     */
    @CheckReturnValue
    SKContext update(@NonNull ContextVariables newData);

    SKContext copy();

    interface Builder {
        SKContext build(ReadOnlySkillCollection skills);

        SKContext build();

        SKContext build(Class<? extends SKContext> clazz);

        Builder with(ContextVariables variables);

        Builder with(ReadOnlySkillCollection skills);

        Builder with(SemanticTextMemory memory);
    }
}
