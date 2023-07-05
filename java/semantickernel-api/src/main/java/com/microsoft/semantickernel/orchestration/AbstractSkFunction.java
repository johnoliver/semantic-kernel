// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.orchestration;

import com.microsoft.semantickernel.builders.SKBuilders;
import com.microsoft.semantickernel.memory.NullMemory;
import com.microsoft.semantickernel.memory.SemanticTextMemory;
import com.microsoft.semantickernel.skilldefinition.KernelSkillsSupplier;
import com.microsoft.semantickernel.skilldefinition.ParameterView;
import com.microsoft.semantickernel.skilldefinition.ReadOnlySkillCollection;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/** Abstract implementation of the SKFunction interface. */
public abstract class AbstractSkFunction<RequestConfiguration>
        implements SKFunction<RequestConfiguration>, RegistrableSkFunction {

    private final List<ParameterView> parameters;
    private final String skillName;
    private final String functionName;
    private final String description;
    @Nullable private KernelSkillsSupplier skillsSupplier;

    public AbstractSkFunction(
            List<ParameterView> parameters,
            String skillName,
            String functionName,
            String description,
            @Nullable KernelSkillsSupplier skillsSupplier) {

        this.parameters = new ArrayList<>(parameters);
        this.skillName = skillName;
        this.functionName = functionName;
        this.description = description;
        this.skillsSupplier = skillsSupplier;
    }

    protected void assertSkillSupplierRegistered() {
        if (skillsSupplier == null) {
            throw new FunctionNotRegisteredException(getName());
        }
    }

    protected void setSkillsSupplier(@Nullable KernelSkillsSupplier skillsSupplier) {
        this.skillsSupplier = skillsSupplier;
    }

    @Nullable
    public KernelSkillsSupplier getSkillsSupplier() {
        return skillsSupplier;
    }

    @Override
    public Mono<SKContext> invokeAsync(
            @Nullable String input,
            @Nullable SKContext context,
            @Nullable RequestConfiguration settings) {
        if (context == null) {
            assertSkillSupplierRegistered();

            context =
                    SKBuilders.context()
                            .with(NullMemory.getInstance())
                            .with(skillsSupplier == null ? null : skillsSupplier.get())
                            .build();
        } else {
            context = context.copy();
        }

        if (input != null) {
            context = context.update(input);
        }

        return this.invokeAsync(context, settings);
    }

    @Override
    public Mono<SKContext> invokeAsync(String input) {
        return invokeAsync(input, null, null);
    }

    @Override
    public Mono<SKContext> invokeAsync(
            @Nullable SKContext context, @Nullable RequestConfiguration settings) {
        if (context == null) {
            context =
                    SKBuilders.context()
                            .with(SKBuilders.variables().build())
                            .with(NullMemory.getInstance())
                            .build();
        } else {
            context = context.copy();
        }

        return this.invokeAsyncInternal(context, settings);
    }

    protected abstract Mono<SKContext> invokeAsyncInternal(
            SKContext context, @Nullable RequestConfiguration settings);

    @Override
    public String getSkillName() {
        return skillName;
    }

    @Override
    public String getName() {
        return functionName;
    }

    public List<ParameterView> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * The function to create a fully qualified name for
     *
     * @return A fully qualified name for a function
     */
    @Override
    public String toFullyQualifiedName() {
        return skillName + "." + functionName;
    }

    @Override
    @Nullable
    public String getDescription() {
        return description;
    }

    @Override
    public String toEmbeddingString() {
        String inputs =
                parameters.stream()
                        .map(p -> "    - " + p.getName() + ": " + p.getDescription())
                        .collect(Collectors.joining("\n"));

        return getName() + ":\n  description: " + getDescription() + "\n  inputs:\n" + inputs;
    }

    @Override
    public String toManualString() {
        String inputs =
                parameters.stream()
                        .map(
                                parameter -> {
                                    String defaultValueString;
                                    if (parameter.getDefaultValue() == null
                                            || parameter.getDefaultValue().isEmpty()) {
                                        defaultValueString = "";
                                    } else {
                                        defaultValueString =
                                                " (default value: "
                                                        + parameter.getDefaultValue()
                                                        + ")";
                                    }

                                    return "  - "
                                            + parameter.getName()
                                            + ": "
                                            + parameter.getDescription()
                                            + defaultValueString;
                                })
                        .collect(Collectors.joining("\n"));

        return toFullyQualifiedName()
                + ":\n"
                + "  description: "
                + getDescription()
                + "\n"
                + "  inputs:\n"
                + inputs;
    }

    protected SKContext buildContext() {
        assertSkillSupplierRegistered();
        return SKBuilders.context()
                .with(SKBuilders.variables().build())
                .with(skillsSupplier == null ? null : skillsSupplier.get())
                .build();
    }

    @Override
    public Mono<SKContext> invokeWithCustomInputAsync(
            ContextVariables input,
            @Nullable SemanticTextMemory semanticMemory,
            @Nullable ReadOnlySkillCollection skills) {
        SKContext tmpContext =
                SKBuilders.context().with(input).with(semanticMemory).with(skills).build();
        return invokeAsync(tmpContext, null);
    }

    @Override
    public Mono<SKContext> invokeAsync() {
        return invokeAsync(null, null, null);
    }

    @Override
    public Mono<SKContext> invokeAsync(SKContext context) {
        return invokeAsync(context, null);
    }
}
