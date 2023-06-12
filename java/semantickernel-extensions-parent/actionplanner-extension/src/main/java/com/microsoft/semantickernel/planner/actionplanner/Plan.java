// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.planner.actionplanner;

import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.Verify;
import com.microsoft.semantickernel.builders.SKBuilders;
import com.microsoft.semantickernel.memory.SemanticTextMemory;
import com.microsoft.semantickernel.orchestration.*;
import com.microsoft.semantickernel.skilldefinition.FunctionView;
import com.microsoft.semantickernel.skilldefinition.KernelSkillsSupplier;
import com.microsoft.semantickernel.skilldefinition.ReadOnlySkillCollection;
import com.microsoft.semantickernel.textcompletion.CompletionRequestSettings;
import com.microsoft.semantickernel.textcompletion.CompletionSKFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/// <summary>
/// Standard Semantic Kernel callable plan.
/// Plan is used to create trees of <see cref="ISKFunction"/>s.
/// </summary>
public class Plan extends AbstractSkFunction<CompletionRequestSettings> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Plan.class);

    private SKFunction function;
    private List<Plan> steps = new ArrayList<>();

    /// <summary>
    /// Outputs for the plan, used to pass information to the caller
    /// </summary>
    public List<String> outputs = new ArrayList<>();

    /// <summary>
    /// Parameters for the plan, used to pass information to the next step
    /// </summary>
    @Nullable public ContextVariables parameters = null;

    /// <summary>
    /// State of the plan
    /// </summary>
    public ContextVariables state;

    public Plan(
            String goal,
            ContextVariables state,
            @Nullable KernelSkillsSupplier kernelSkillsSupplier) {
        super(
                DelegateTypes.ContextSwitchInSKContextOutTaskSKContext,
                new ArrayList<>(),
                Plan.class.getName(),
                "",
                goal,
                kernelSkillsSupplier);
        this.state = state;
    }

    public Plan(String goal, @Nullable KernelSkillsSupplier kernelSkillsSupplier) {
        this(goal, SKBuilders.variables().build(), kernelSkillsSupplier);
    }

    public Plan(
            SKFunction function,
            ContextVariables state,
            List<String> functionOutputs,
            KernelSkillsSupplier kernelSkillsSupplier) {
        super(
                DelegateTypes.ContextSwitchInSKContextOutTaskSKContext,
                function.describe().getParameters(),
                function.getSkillName(),
                function.getName(),
                function.getDescription(),
                kernelSkillsSupplier);

        this.function = function;
        this.outputs = functionOutputs;
        this.state = state;
    }

    public Plan(
            CompletionSKFunction function,
            List<String> functionOutputs,
            KernelSkillsSupplier kernelSkillsSupplier) {
        this(function, SKBuilders.variables().build(), functionOutputs, kernelSkillsSupplier);
    }

    public Plan(CompletionSKFunction function, KernelSkillsSupplier kernelSkillsSupplier) {
        this(function, SKBuilders.variables().build(), new ArrayList<>(), kernelSkillsSupplier);
    }

    public Plan(
            String goal, KernelSkillsSupplier kernelSkillsSupplier, CompletionSKFunction... steps) {
        this(goal, kernelSkillsSupplier);
        this.addSteps(steps);
    }

    /*
       @Override
       protected Mono<CompletionSKContext> invokeAsyncInternal(
               CompletionSKContext context, @Nullable CompletionRequestSettings settings) {
           if (function != null) {
               return function.invokeAsync(context, settings);
           } else {
               return Flux.fromIterable(steps)
                       .reduceWith(
                               () -> Mono.just(context),
                               (context2, step) -> {
                                   return context2.flatMap(c -> step.invokeAsync(c, settings));
                               })
                       .flatMap(it -> it);
           }
       }

    */

    @Override
    public void registerOnKernel(Kernel kernel) {
        kernel.registerSemanticFunction(this);
    }

    @Override
    public FunctionView describe() {
        return function.describe();
    }

    @Override
    public Class getType() {
        return function.getType();
    }

    @Override
    public SKContext buildContext(
            ContextVariables variables,
            @Nullable SemanticTextMemory memory,
            @Nullable ReadOnlySkillCollection skills) {
        if (function == null) {
            return steps.get(0).buildContext(variables, memory, skills);
        }
        return function.buildContext(variables, memory, skills);
    }

    /// <summary>
    /// Gets whether the plan has a next step.
    /// </summary>
    public boolean hasNextStep() {
        return this.nextStepIndex < this.steps.size();
    }

    /// <summary>
    /// Gets the next step index.
    /// </summary>
    public int nextStepIndex;

    /*
        #region ISKFunction implementation

        /// <inheritdoc/>
        [JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;

        /// <inheritdoc/>
        [JsonPropertyName("skill_name")]
        public string SkillName { get; set; } = string.Empty;

        /// <inheritdoc/>
        [JsonPropertyName("description")]
        public string Description { get; set; } = string.Empty;

        /// <inheritdoc/>
        [JsonIgnore]
        public bool IsSemantic { get; private set; }

        /// <inheritdoc/>
        [JsonIgnore]
        public CompleteRequestSettings RequestSettings { get; private set; } = new();

        #endregion ISKFunction implementation

        /// <summary>
        /// Initializes a new instance of the <see cref="Plan"/> class with a goal description.
        /// </summary>
        /// <param name="goal">The goal of the plan used as description.</param>
        public Plan(string goal)
        {
            this.Description = goal;
            this.SkillName = this.GetType().FullName;
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="Plan"/> class with a goal description and steps.
        /// </summary>
        /// <param name="goal">The goal of the plan used as description.</param>
        /// <param name="steps">The steps to add.</param>
        public Plan(string goal, params ISKFunction[] steps) : this(goal)
        {
            this.AddSteps(steps);
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="Plan"/> class with a goal description and steps.
        /// </summary>
        /// <param name="goal">The goal of the plan used as description.</param>
        /// <param name="steps">The steps to add.</param>
        public Plan(string goal, params Plan[] steps) : this(goal)
        {
            this.AddSteps(steps);
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="Plan"/> class with a function.
        /// </summary>
        /// <param name="function">The function to execute.</param>
        public Plan(ISKFunction function)
        {
            this.SetFunction(function);
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="Plan"/> class with a function and steps.
        /// </summary>
        /// <param name="name">The name of the plan.</param>
        /// <param name="skillName">The name of the skill.</param>
        /// <param name="description">The description of the plan.</param>
        /// <param name="nextStepIndex">The index of the next step.</param>
        /// <param name="state">The state of the plan.</param>
        /// <param name="parameters">The parameters of the plan.</param>
        /// <param name="outputs">The outputs of the plan.</param>
        /// <param name="steps">The steps of the plan.</param>
        [JsonConstructor]
        public Plan(
            string name,
            string skillName,
            string description,
            int nextStepIndex,
            ContextVariables state,
            ContextVariables parameters,
            IList<string> outputs,
            IReadOnlyList<Plan> steps)
        {
            this.Name = name;
            this.SkillName = skillName;
            this.Description = description;
            this.NextStepIndex = nextStepIndex;
            this.State = state;
            this.Parameters = parameters;
            this.Outputs = outputs;
            this._steps.Clear();
            this.AddSteps(steps.ToArray());
        }

        /// <summary>
        /// Deserialize a JSON string into a Plan object.
        /// TODO: the context should never be null, it's required internally
        /// </summary>
        /// <param name="json">JSON string representation of a Plan</param>
        /// <param name="context">The context to use for function registrations.</param>
        /// <returns>An instance of a Plan object.</returns>
        /// <remarks>If Context is not supplied, plan will not be able to execute.</remarks>
        public static Plan FromJson(string json, SKContext? context = null)
        {
            var plan = JsonSerializer.Deserialize<Plan>(json, new JsonSerializerOptions { IncludeFields = true }) ?? new Plan(string.Empty);

            if (context != null)
            {
                plan = SetAvailableFunctions(plan, context);
            }

            return plan;
        }

        /// <summary>
        /// Get JSON representation of the plan.
        /// </summary>
        /// <param name="indented">Whether to emit indented JSON</param>
        /// <returns>Plan serialized using JSON format</returns>
        public string ToJson(bool indented = false)
        {
            return JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = indented });
        }
    */
    /// <summary>
    /// Adds one or more existing plans to the end of the current plan as steps.
    /// </summary>
    /// <param name="steps">The plans to add as steps to the current plan.</param>
    /// <remarks>
    /// When you add a plan as a step to the current plan, the steps of the added plan are executed
    // after the steps of the current plan have completed.
    /// </remarks>
    public void addSteps(Plan... steps) {
        this.steps.addAll(Arrays.asList(steps));
    }

    /// <summary>
    /// Adds one or more new steps to the end of the current plan.
    /// </summary>
    /// <param name="steps">The steps to add to the current plan.</param>
    /// <remarks>
    /// When you add a new step to the current plan, it is executed after the previous step in the
    // plan has completed. Each step can be a function call or another plan.
    /// </remarks>
    public void addSteps(CompletionSKFunction... steps) {
        List<Plan> plans =
                Arrays.stream(steps)
                        .map(step -> new Plan(step, getSkillsSupplier()))
                        .collect(Collectors.toList());
        this.steps.addAll(plans);
    }

    public void setOutputs(List<String> functionOutputs) {
        this.outputs = Collections.unmodifiableList(functionOutputs);
    }

    public void setParameters(ContextVariables functionVariables) {
        this.parameters = functionVariables;
    }

    public void addOutputs(List<String> outputs) {
        this.outputs.addAll(outputs);
    }

    /// <summary>
    /// Runs the next step in the plan using the provided kernel instance and variables.
    /// </summary>
    /// <param name="kernel">The kernel instance to use for executing the plan.</param>
    /// <param name="variables">The variables to use for the execution of the plan.</param>
    /// <param name="cancellationToken">The cancellation token to cancel the execution of the
    // plan.</param>
    /// <returns>A task representing the asynchronous execution of the plan's next step.</returns>
    /// <remarks>
    /// This method executes the next step in the plan using the specified kernel instance and
    // context variables. The context variables contain the necessary information for executing the
    // plan, such as the memory, skills, and logger. The method returns a task representing the
    // asynchronous execution of the plan's next step.
    /// </remarks>
    /*
    public Mono<Plan> runNextStepAsync(Kernel kernel, ContextVariables variables) {
        SKContext context = SKBuilders
                .context()
                .with(variables)
                .with(kernel.getSkills())
                .with(kernel.getMemoryStore())
                .build();
        return this.invokeNextStepAsync(context);
    }

    /// <summary>
    /// Invoke the next step of the plan
    /// </summary>
    /// <param name="context">Context to use</param>
    /// <returns>The updated plan</returns>
    /// <exception cref="KernelException">If an error occurs while running the plan</exception>

    public Mono<Plan> invokeNextStepAsync(SKContext context) {
        if (this.hasNextStep()) {
            Plan step = this.steps.get(this.nextStepIndex);

            // Merge the state with the current context variables for step execution
            ContextVariables functionVariables = this.getNextStepVariables(context.getVariables(), step);

            // Execute the step
            SKContext<?> functionContext = SKBuilders.context()
                    .with(functionVariables)
                    .with(context.getSemanticMemory())
                    .with(context.getSkills())
                    .build();

            Mono<SKContext<?>> result = step.invokeAsync(functionContext, null);
            var resultValue = result.Result.Trim();

            if (result.ErrorOccurred) {
                throw new KernelException(KernelException.ErrorCodes.FunctionInvokeError,
                        $"Error occurred while running plan step: {result.LastErrorDescription}", result.LastException);
            }

            #region Update State

            // Update state with result
            this.State.Update(resultValue);

            // Update Plan Result in State with matching outputs (if any)
            if (this.Outputs.Intersect(step.Outputs).Any()) {
                this.State.Get(DefaultResultKey, out var currentPlanResult);
                this.State.Set(DefaultResultKey, string.Join("\n", currentPlanResult.Trim(), resultValue));
            }

            // Update state with outputs (if any)
            foreach(var item in step.Outputs)
            {
                if (result.Variables.Get(item, out var val)) {
                    this.State.Set(item, val);
                } else {
                    this.State.Set(item, resultValue);
                }
            }

            #endregion Update State

            this.NextStepIndex++;
        }

        return this;
    }


     */
    /*
            #region ISKFunction implementation

            /// <inheritdoc/>
            public FunctionView Describe()
            {
                // TODO - Eventually, we should be able to describe a plan and its expected inputs/outputs
                return this.Function?.Describe() ?? new();
            }
    */
    /// <inheritdoc/>
    public Mono<SKContext> invokeAsync(
            @Nullable String input,
            @Nullable CompletionRequestSettings settings,
            @Nullable SemanticTextMemory memory) {

        WritableContextVariables variables = state.writableClone();

        if (input != null) {
            variables.update(input);
        }

        SKContext context =
                SKBuilders.context()
                        .with(variables)
                        .with(memory)
                        .with(getSkillsSupplier().get())
                        .build();

        return this.invokeAsync(context, settings);
    }

    @Override
    public Mono<SKContext> invokeAsyncInternal(
            SKContext contextIn, @Nullable CompletionRequestSettings settings) {

        return inv(function, contextIn, state, steps, settings);
        /*
        if (function != null) {
            SKContext<?> merged = addVariablesToContext(this.state, contextIn);
            return function.invokeAsync(merged, settings);
        } else {
            return Flux.fromIterable(steps)
                    .reduceWith(
                            () -> Mono.just(contextIn),
                            (context, step) -> {
                                return context
                                        .flatMap(functionContext -> {
                                            c = Plan.<SKContext<?>>addVariablesToContext(this.state, functionContext, functionContext.getClass());
                                            return step.invokeAsync(c, settings);
                                            //this.UpdateContextWithOutputs(context);
                                        });
                            })
                    .flatMap(it -> it);
        }*/
    }

    private static Mono<SKContext> inv(
            SKFunction function,
            SKContext contextIn,
            ContextVariables state,
            List<Plan> steps,
            @Nullable CompletionRequestSettings settings) {
        if (function != null) {
            SKContext merged = Plan.addVariablesToContext(state, contextIn);
            return function.invokeAsync(merged, settings);
        } else {
            return Flux.fromIterable(steps)
                    .reduceWith(
                            () -> Mono.just(contextIn),
                            (context, step) -> {
                                return context.flatMap(
                                        functionContext -> {
                                            SKContext c =
                                                    Plan.addVariablesToContext(
                                                            state, functionContext);
                                            return step.invokeAsync(c, settings);
                                            // this.UpdateContextWithOutputs(context);
                                        });
                            })
                    .flatMap(
                            it -> {
                                return it;
                            });
        }
    }

    /*
        /// <inheritdoc/>
        public Mono<CompletionSKContext> invokeAsync(
                CompletionSKContext context,
                @Nullable
                CompletionRequestSettings settings) {
            if (this.function != null) {
                return function
                        .invokeAsync(context, settings)
                        .doOnError(result -> {
                            LOGGER.error("Something went wrong in plan step {}.{}:'{}'", this.getSkillName(), this.getName(), result.getMessage());
                        })
                        .map(result -> {
                            return context.copy().update(result.getResult());
                        });

            } else {
                // loop through steps and execute until completion
                while (this.hasNextStep()) {
                    CompletionSKContext functionContext = context;

                    functionContext = addVariablesToContext(this.state, functionContext);

                    this.invokeNextStepAsync(functionContext)
                            .map(result -> {
                                return updateContextWithOutputs(context);
                            });
                }
            }

            return context;
        }
    */
    /*
        /// <inheritdoc/>
        public ISKFunction SetDefaultSkillCollection(IReadOnlySkillCollection skills)
        {
            return this.Function is null
                ? throw new NotImplementedException()
                : this.Function.SetDefaultSkillCollection(skills);
        }

        /// <inheritdoc/>
        public ISKFunction SetAIService(Func<ITextCompletion> serviceFactory)
        {
            return this.Function is null
                ? throw new NotImplementedException()
                : this.Function.SetAIService(serviceFactory);
        }

        /// <inheritdoc/>
        public ISKFunction SetAIConfiguration(CompleteRequestSettings settings)
        {
            return this.Function is null
                ? throw new NotImplementedException()
                : this.Function.SetAIConfiguration(settings);
        }

        #endregion ISKFunction implementation
    */
    /// <summary>
    /// Expand variables in the input string.
    /// </summary>
    /// <param name="variables">Variables to use for expansion.</param>
    /// <param name="input">Input string to expand.</param>
    /// <returns>Expanded string.</returns>
    String expandFromVariables(ContextVariables variables, String input) {
        String result = input;
        Matcher matches = s_variablesRegex.matcher(input);
        while (matches.find()) {

            String varName = matches.group(1);

            String value = variables.get(varName);

            if (value == null) {
                value = state.get(varName);
            }

            if (value == null) {
                value = "";
            }

            result = result.replaceAll(varName, value);
        }

        return result;
    }

    /*
            /// <summary>
            /// Set functions for a plan and its steps.
            /// </summary>
            /// <param name="plan">Plan to set functions for.</param>
            /// <param name="context">Context to use.</param>
            /// <returns>The plan with functions set.</returns>
            private static Plan SetAvailableFunctions(Plan plan, SKContext context)
            {
                if (plan.Steps.Count == 0)
                {
                    if (context.Skills == null)
                    {
                        throw new KernelException(
                            KernelException.ErrorCodes.SkillCollectionNotSet,
                            "Skill collection not found in the context");
                    }

                    if (context.Skills.TryGetFunction(plan.SkillName, plan.Name, out var skillFunction))
                    {
                        plan.SetFunction(skillFunction);
                    }
                }
                else
                {
                    foreach (var step in plan.Steps)
                    {
                        SetAvailableFunctions(step, context);
                    }
                }

                return plan;
            }
    */
    /// <summary>
    /// Add any missing variables from a plan state variables to the context.
    /// </summary>
    private static SKContext addVariablesToContext(ContextVariables vars, SKContext context) {
        SKContext clone = context.copy();
        vars.asMap()
                .entrySet()
                .forEach(
                        entry -> {
                            if (Verify.isNullOrEmpty(clone.getVariables().get(entry.getKey()))) {
                                clone.setVariable(entry.getKey(), entry.getValue());
                            }
                        });

        return clone;
    }

    /*

    /// <summary>
    /// Update the context with the outputs from the current step.
    /// </summary>
    /// <param name="context">The context to update.</param>
    /// <returns>The updated context.</returns>
    private CompletionSKContext updateContextWithOutputs(CompletionSKContext context) {

        String resultString = this.state.get(DefaultResultKey);
        if (Verify.isNullOrEmpty(resultString)) {

            //TODO tostring???
            resultString = state.toString();
        }

        context.update(resultString);

        String finalResultString = resultString;
        steps.get(nextStepIndex - 1).outputs
                .forEach(item -> {
                    if (!Verify.isNullOrEmpty(state.get(item))) {
                        context.setVariable(item, state.get(item));
                    } else {
                        context.setVariable(item, finalResultString);
                    }
                });

        return context;
    }
    /// <summary>
    /// Get the variables for the next step in the plan.
    /// </summary>
    /// <param name="variables">The current context variables.</param>
    /// <param name="step">The next step in the plan.</param>
    /// <returns>The context variables for the next step in the plan.</returns>
    private ContextVariables getNextStepVariables(ContextVariables variables, Plan step) {
        // Priority for Input
        // - Parameters (expand from variables if needed)
        // - SKContext.Variables
        // - Plan.State
        // - Empty if sending to another plan
        // - Plan.Description

        String input = "";
        if (!Verify.isNullOrEmpty(step.parameters.getInput())) {
            input = this.expandFromVariables(variables, step.parameters.getInput());
        } else if (!Verify.isNullOrEmpty(variables.getInput())) {
            input = variables.getInput();
        } else if (!Verify.isNullOrEmpty(this.state.getInput())) {
            input = this.state.getInput();
        } else if (step.steps.size() > 0) {
            input = "";
        } else if (!Verify.isNullOrEmpty(this.getDescription())) {
            input = this.getDescription();
        }

        WritableContextVariables stepVariables = SKBuilders.variables().build(input).writableClone();

        // Priority for remaining stepVariables is:
        // - Function Parameters (pull from variables or state by a key value)
        // - Step Parameters (pull from variables or state by a key value)
        FunctionView functionParameters = step.describe();
        for (ParameterView param : functionParameters.getParameters()) {
            if (param.getName().equals(ContextVariables.MAIN_KEY)) {
                continue;
            }
            String value;

            if (!Verify.isNullOrEmpty(variables.get(param.getName()))) {
                stepVariables.setVariable(param.getName(), variables.get(param.getName()));
            } else if (!Verify.isNullOrEmpty(this.state.get(param.getName()))) {
                stepVariables.setVariable(param.getName(), this.state.get(param.getName()));
            }
        }

        step.parameters.asMap().entrySet()
                .forEach(item -> {
                    // Don't overwrite variable values that are already set
                    if (!Verify.isNullOrEmpty(stepVariables.get(item.getKey()))) {
                        return;
                    }

                    String expandedValue = this.expandFromVariables(variables, item.getValue());
                    if (!expandedValue.equalsIgnoreCase(item.getValue())) {
                        stepVariables.setVariable(item.getKey(), expandedValue);
                    } else if (!Verify.isNullOrEmpty(variables.get(item.getKey()))) {
                        stepVariables.setVariable(item.getKey(), variables.get(item.getKey()));
                    } else if (!Verify.isNullOrEmpty(state.get(item.getKey()))) {
                        stepVariables.setVariable(item.getKey(), state.get(item.getKey()));
                    } else {
                        stepVariables.setVariable(item.getKey(), expandedValue);
                    }
                });

        return stepVariables;
    }

    /*
        private void SetFunction(ISKFunction function)
        {
            this.Function = function;
            this.Name = function.Name;
            this.SkillName = function.SkillName;
            this.Description = function.Description;
            this.IsSemantic = function.IsSemantic;
            this.RequestSettings = function.RequestSettings;
        }

        private ISKFunction? Function { get; set; } = null;

        private readonly List<Plan> _steps = new();
    */
    private static final Pattern s_variablesRegex = Pattern.compile("$(\\w+)", Pattern.MULTILINE);

    private static final String DefaultResultKey = "PLAN.RESULT";
    /*
    [DebuggerBrowsable(DebuggerBrowsableState.Never)]
    private string DebuggerDisplay
    {
        get
        {
            string display = this.Description;

            if (!string.IsNullOrWhiteSpace(this.Name))
            {
                display = $"{this.Name} ({display})";
            }

            if (this._steps.Count > 0)
            {
                display += $", Steps = {this._steps.Count}, NextStep = {this.NextStepIndex}";
            }

            return display;
        }
    }

     */
}
