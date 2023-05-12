// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.templateengine.blocks; // Copyright (c) Microsoft. All rights
// reserved.

import com.microsoft.semantickernel.orchestration.ReadOnlyContextVariables;
import com.microsoft.semantickernel.orchestration.ReadOnlySKContext;
import com.microsoft.semantickernel.orchestration.SKFunction;
import com.microsoft.semantickernel.skilldefinition.ReadOnlyFunctionCollection;
import com.microsoft.semantickernel.skilldefinition.ReadOnlySkillCollection;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.List;

// ReSharper disable TemplateIsNotCompileTimeConstantProblem
public class CodeBlock extends Block implements CodeRendering {
    private final List<Block> tokens;

    public CodeBlock(List<Block> tokens, String content) {
        super(content, BlockTypes.Code);
        this.tokens = tokens;
    }

    public CodeBlock(String content) {
        super(content, BlockTypes.Code);
        this.tokens = null;
    }

    @Override
    public boolean isValid() {
        // TODO
        return true;
    }

    @Override
    @Nullable
    public Mono<String> renderCodeAsync(ReadOnlySKContext context) {
        /* TODO
        }
            if (!this._validated && !this.IsValid(out var error))
            {
                throw new TemplateException(TemplateException.ErrorCodes.SyntaxError, error);
            }

             */

        // this.Log.LogTrace("Rendering code: `{0}`", this.Content);

        switch (this.tokens.get(0).getType()) {
            case Value:
            case Variable:
                return Mono.just(
                        ((TextRendering) this.tokens.get(0)).render(context.getVariables()));

            case FunctionId:
                return this.renderFunctionCallAsync((FunctionIdBlock) this.tokens.get(0), context);
        }

        throw new RuntimeException("Unknown type");
    }

    /*
        internal override BlockTypes Type => BlockTypes.Code;

        public CodeBlock(string? content, ILogger log)
            : this(new CodeTokenizer(log).Tokenize(content), content?.Trim(), log)
        {
        }

        public CodeBlock(List<Block> tokens, string? content, ILogger log)
            : base(content?.Trim(), log)
        {
            this._tokens = tokens;
        }

        public override bool IsValid(out string errorMsg)
        {
            errorMsg = "";

            foreach (Block token in this._tokens)
            {
                if (!token.IsValid(out errorMsg))
                {
                    this.Log.LogError(errorMsg);
                    return false;
                }
            }

            if (this._tokens.Count > 1)
            {
                if (this._tokens[0].Type != BlockTypes.FunctionId)
                {
                    errorMsg = $"Unexpected second token found: {this._tokens[1].Content}";
                    this.Log.LogError(errorMsg);
                    return false;
                }

                if (this._tokens[1].Type != BlockTypes.Value && this._tokens[1].Type != BlockTypes.Variable)
                {
                    errorMsg = "Functions support only one parameter";
                    this.Log.LogError(errorMsg);
                    return false;
                }
            }

            if (this._tokens.Count > 2)
            {
                errorMsg = $"Unexpected second token found: {this._tokens[1].Content}";
                this.Log.LogError(errorMsg);
                return false;
            }

            this._validated = true;

            return true;
        }

        public async Task<string> RenderCodeAsync(SKContext context)
        {
            if (!this._validated && !this.IsValid(out var error))
            {
                throw new TemplateException(TemplateException.ErrorCodes.SyntaxError, error);
            }

            this.Log.LogTrace("Rendering code: `{0}`", this.Content);

            switch (this._tokens[0].Type)
            {
                case BlockTypes.Value:
                case BlockTypes.Variable:
                    return ((ITextRendering)this._tokens[0]).Render(context.Variables);

                case BlockTypes.FunctionId:
                    return await this.RenderFunctionCallAsync((FunctionIdBlock)this._tokens[0], context);
            }

            throw new TemplateException(TemplateException.ErrorCodes.UnexpectedBlockType,
                $"Unexpected first token type: {this._tokens[0].Type:G}");
        }

        #region private ================================================================================

        private bool _validated;
        private readonly List<Block> _tokens;
    */
    private Mono<String> renderFunctionCallAsync(
            FunctionIdBlock fBlock, ReadOnlySKContext context) {
        // context.ThrowIfSkillCollectionNotSet();
        SKFunction function = this.getFunctionFromSkillCollection(context.getSkills(), fBlock);
        if (function == null) {
            // var errorMsg = $ "Function `{fBlock.Content}` not found";
            // this.Log.LogError(errorMsg);
            throw new RuntimeException("Function not found");
        }

        ReadOnlyContextVariables variablesClone = context.getVariables().copy();

        // If the code syntax is {{functionName $varName}} use $varName instead of $input
        // If the code syntax is {{functionName 'value'}} use "value" instead of $input
        if (this.tokens.size() > 1) {
            // TODO: PII
            // this.Log.LogTrace("Passing variable/value: `{0}`", this._tokens[1].Content);
            String input = ((TextRendering) this.tokens.get(1)).render(variablesClone);
            variablesClone = variablesClone.update(input);
        }

        Mono<ReadOnlySKContext> result =
                function.invokeWithCustomInputAsync(
                        variablesClone, context.getSemanticMemory(), context.getSkills());

        return result.map(
                it -> {
                    return it.getResult();
                });
    }

    private SKFunction getFunctionFromSkillCollection(
            ReadOnlySkillCollection skills, FunctionIdBlock fBlock) {
        String skillName = fBlock.getSkillName();
        // Function in the global skill
        if ((skillName == null || skillName.isEmpty())
                && skills.hasFunction(fBlock.getFunctionName())) {
            SKFunction<?, ?> function =
                    skills.getFunction(fBlock.getFunctionName(), SKFunction.class);
            return function;
        }

        // Function within a specific skill
        if (!(skillName == null || skillName.isEmpty())) {

            ReadOnlyFunctionCollection skill = skills.getFunctions(fBlock.getSkillName());

            if (skill != null) {
                return skill.getFunction(fBlock.getFunctionName());
            }
        }

        return null;
    }
}
