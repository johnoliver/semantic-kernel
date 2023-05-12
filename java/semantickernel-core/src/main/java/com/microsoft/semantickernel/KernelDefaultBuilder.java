// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel;

import com.microsoft.semantickernel.memory.MemoryStore;
import com.microsoft.semantickernel.memory.NullMemory;
import com.microsoft.semantickernel.memory.VolatileMemoryStore;
import com.microsoft.semantickernel.skilldefinition.DefaultReadOnlySkillCollection;
import com.microsoft.semantickernel.skilldefinition.ReadOnlySkillCollection;
import com.microsoft.semantickernel.templateengine.DefaultPromptTemplateEngine;
import com.microsoft.semantickernel.templateengine.PromptTemplateEngine;

import javax.annotation.Nullable;

public class KernelDefaultBuilder implements Kernel.InternalBuilder {

    @Override
    public Kernel build(
            KernelConfig kernelConfig,
            @Nullable PromptTemplateEngine promptTemplateEngine,
            @Nullable ReadOnlySkillCollection skillCollection) {
        return new KernelDefault(kernelConfig, promptTemplateEngine, skillCollection, null
        );
    }

    @Override
    public Kernel build(
            KernelConfig kernelConfig,
            @Nullable PromptTemplateEngine promptTemplateEngine,
            @Nullable ReadOnlySkillCollection skillCollection,
            @Nullable MemoryStore memoryStore) {
        if (promptTemplateEngine == null) {
            promptTemplateEngine = new DefaultPromptTemplateEngine();
        }

        if (skillCollection == null) {
            skillCollection = new DefaultReadOnlySkillCollection();
        }

        if (memoryStore == null) {
            memoryStore = new VolatileMemoryStore();
        }

        if (kernelConfig == null) {
            throw new IllegalArgumentException();
        }

        return new KernelDefault(kernelConfig, promptTemplateEngine, skillCollection, memoryStore);
    }
}
