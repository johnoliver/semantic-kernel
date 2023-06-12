// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.planner;

import com.microsoft.semantickernel.SKException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/// <summary>
/// Exception thrown for errors related to planning.
/// </summary>
public class PlanningException extends SKException {
    @Nonnull private final ErrorCodes errorCode;

    public PlanningException(@Nonnull ErrorCodes error) {
        this(error, null, null);
    }

    public PlanningException(@Nonnull ErrorCodes errorCode, @Nullable String message) {
        this(errorCode, message, null);
    }

    public PlanningException(
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
    /// Error codes for <see cref="PlanningException"/>.
    /// </summary>
    public enum ErrorCodes {
        /// <summary>
        /// Unknown error.
        /// </summary>
        UnknownError("Unknown error"),

        /// <summary>
        /// Invalid goal.
        /// </summary>
        InvalidGoal("Invalid goal"),

        /// <summary>
        /// Invalid plan.
        /// </summary>
        InvalidPlan("Invalid plan"),

        /// <summary>
        /// Invalid configuration.
        /// </summary>
        InvalidConfiguration("Invalid configuration");

        private final String message;

        ErrorCodes(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
