package com.microsoft.semantickernel.textcompletion;

import com.microsoft.semantickernel.KernelContent;

public class TextContent extends KernelContent {

    private final String content;

    public TextContent(String content) {
        super(content, null, null);
        this.content = content;
    }

    public String getValue() {
        return content;
    }

    @Override
    public String getContent() {
        return content;
    }
}
