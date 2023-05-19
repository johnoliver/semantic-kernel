// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.templateengine.blocks; // Copyright (c) Microsoft. All rights
// reserved.

import com.microsoft.semantickernel.orchestration.ContextVariables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.util.annotation.Nullable;

public class ValBlock extends Block implements TextRendering {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValBlock.class);

    // Cache the first and last char
    private char _first = '\0';
    private char _last = '\0';

    // Content, excluding start/end quote chars
    private String _value = "";

    public ValBlock(String content) {
        super(content.trim(), BlockTypes.Value);

        if (this.getContent().length() < 2) {
            LOGGER.error("A value must have single quotes or double quotes on both sides");
            return;
        }

        this._first = this.getContent().charAt(0);
        this._last = this.getContent().charAt(getContent().length() - 1);
        this._value = this.getContent().substring(1, this.getContent().length() - 2);
    }

    @Override
    @Nullable
    public String render(ContextVariables variables) {
        return _value;
    }

    @Override
    public boolean isValid() {
        // Content includes the quotes, so it must be at least 2 chars long
        if (this.getContent().length() < 2) {
            LOGGER.error("A value must have single quotes or double quotes on both sides");
            return false;
        }

        // Check if delimiting chars are consistent
        if (_first != _last) {
            LOGGER.error(
                    "A value must be defined using either single quotes or double quotes, not"
                            + " both");
            return false;
        }

        return true;
    }

    public static boolean hasValPrefix(@Nullable String text) {
        return text != null
                && text.length() > 0
                && (text.charAt(0) == Symbols.DblQuote || text.charAt(0) == Symbols.SglQuote);
    }
}
