package com.swrve.sdk.conversations.engine.model;


import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.io.Serializable;

public abstract class ConversationAtom implements Serializable {
    public static final String TYPE_CONTENT_HTML = "html-fragment";
    public static final String TYPE_CONTENT_IMAGE = "image";
    public static final String TYPE_CONTENT_VIDEO = "video";
    public static final String TYPE_CONTENT_SPACER = "spacer";
    public static final String TYPE_INPUT_MULTIVALUE = "multi-value-input";
    public static final String TYPE_INPUT_STARRATING = "star-rating";

    protected String tag;
    protected String type;
    protected ConversationStyle style;

    public static ConversationAtom create(String tag, String type) {
        BareConversationAtom bca = new BareConversationAtom();
        bca.tag = tag;
        bca.type = type;
        return bca;
    }

    public String getTag() {
        return tag;
    }

    public ConversationStyle getStyle(){
        return this.style;
    }

    public String getType() {
        return type;
    }

    private static class BareConversationAtom extends ConversationAtom {
    }
}
