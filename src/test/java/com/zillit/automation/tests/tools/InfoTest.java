package com.zillit.automation.tests.tools;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tools → "Info" module. A Bulletin-style chat (ConfidentialInfo.kt, the same activity as
 * Confidential Info); all cases come from {@link ToolChatBaseTest}, including the web-gap
 * feed cases (web-e2e/gaps/info.md), which this suite opts in to.
 */
@Order(TestOrder.TOOL_INFO)
@ExtendWith(ReasonLogger.class)
class InfoTest extends ToolChatBaseTest {
    @Override protected String moduleName() { return "Info"; }

    /** Info runs the ConfidentialInfo.kt feed cases (search, History, selection, …). */
    @Override protected boolean infoFeedCases() { return true; }

    /**
     * Info always offers all 8 pickers: Photo, Gallery, Video, Video Gallery, Audio,
     * Document, Location, Contact (ConfidentialInfo.kt onAttachmentClick).
     */
    @Override protected int minAttachmentOptions() { return 8; }
}
