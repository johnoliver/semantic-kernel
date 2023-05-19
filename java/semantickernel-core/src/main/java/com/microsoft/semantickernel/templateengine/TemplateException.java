// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.templateengine;

import com.microsoft.semantickernel.diagnostics.SKException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TemplateException extends SKException {

    @Nonnull private final ErrorCodes errorCode;

    public TemplateException(@Nonnull ErrorCodes error) {
        this(error, null, null);
    }

    public TemplateException(@Nonnull ErrorCodes errorCode, @Nullable String message) {
        this(errorCode, message, null);
    }

    public TemplateException(
            @Nonnull ErrorCodes errorCode,
            @Nullable String message,
            @Nullable Throwable innerException) {
        super(getDefaultMessage(errorCode, message), innerException);
        this.errorCode = errorCode;
    }

    public ErrorCodes getErrorCode() {
        return errorCode;
    }

    /* Translate the error code into a default message */
    private static String getDefaultMessage(
            @Nonnull ErrorCodes errorCode, @Nullable String message) {
        return String.format("%s: %s", errorCode.getMessage(), message);
    }

    /// <summary>
    /// Error codes for <see cref="TemplateException"/>.
    /// </summary>
    public enum ErrorCodes {
        /// <summary>
        /// Unknown error.
        /// </summary>
        UnknownError("Unknown error"),

        /// <summary>
        /// Syntax error, the template syntax used is not valid.
        /// </summary>
        SyntaxError("Syntax error, the template syntax used is not valid"),

        /// <summary>
        /// The block type produced be the tokenizer was not expected
        /// </summary>
        UnexpectedBlockType("The block type produced be the tokenizer was not expected"),

        /// <summary>
        /// The template requires an unknown function.
        /// </summary>
        FunctionNotFound("The template requires an unknown function"),

        /// <summary>
        /// The template execution failed, e.g. a function call threw an exception.
        /// </summary>
        RuntimeError("The template execution failed, e.g. a function call threw an exception"),
        ;

        private final String message;

        ErrorCodes(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
