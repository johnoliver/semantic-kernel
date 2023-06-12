// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.skilldefinition;

import java.util.Collections;
import java.util.List;

/// <summary>
/// Class used to copy and export data from the skill collection.
/// The data is mutable, but changes do not affect the skill collection.
/// </summary>
public class FunctionView {
    /// <summary>
    /// Name of the function. The name is used by the skill collection and in prompt templates e.g.
    // {{skillName.functionName}}
    /// </summary>
    public final String name;

    /// <summary>
    /// Name of the skill containing the function. The name is used by the skill collection and in
    // prompt templates e.g. {{skillName.functionName}}
    /// </summary>
    public final String skillName;

    /// <summary>
    /// Function description. The description is used in combination with embeddings when searching
    // relevant functions.
    /// </summary>
    public final String description;

    /// <summary>
    /// Whether the delegate points to a semantic function
    /// </summary>
    public final boolean isSemantic;

    /// <summary>
    /// Whether the delegate is an asynchronous function
    /// </summary>
    public final boolean isAsynchronous;

    /// <summary>
    /// List of function parameters
    /// </summary>
    public final List<ParameterView> parameters;

    /// <summary>
    /// Create a function view.
    /// </summary>
    /// <param name="name">Function name</param>
    /// <param name="skillName">Skill name, e.g. the function namespace</param>
    /// <param name="description">Function description</param>
    /// <param name="parameters">List of function parameters provided by the skill developer</param>
    /// <param name="isSemantic">Whether the function is a semantic one (or native is False)</param>
    /// <param name="isAsynchronous">Whether the function is async. Note: all semantic functions are
    // async.</param>
    public FunctionView(
            String name,
            String skillName,
            String description,
            List<ParameterView> parameters,
            boolean isSemantic,
            boolean isAsynchronous) {
        this.name = name;
        this.skillName = skillName;
        this.description = description;
        this.parameters = parameters;
        this.isSemantic = isSemantic;
        this.isAsynchronous = isAsynchronous;
    }

    public List<ParameterView> getParameters() {
        return Collections.unmodifiableList(parameters);
    }
}
