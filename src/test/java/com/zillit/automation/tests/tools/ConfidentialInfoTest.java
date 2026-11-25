package com.zillit.automation.tests.tools;

import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tools → "Confidential Info" module. A Bulletin-style chat (ConfidentialInfo.kt, the same
 * activity as Info); all cases come from {@link ToolChatBaseTest}, including the web-gap
 * feed cases (web-e2e/gaps/confidential-info.md), which this suite opts in to.
 */
@Order(TestOrder.TOOL_CONFIDENTIAL)
@ExtendWith(ReasonLogger.class)
class ConfidentialInfoTest extends ToolChatBaseTest {
    @Override protected String moduleName() { return "Confidential Info"; }

    /** Confidential Info runs the ConfidentialInfo.kt feed cases (search, History, selection, …). */
    @Override protected boolean infoFeedCases() { return true; }

    /** All 8 pickers, as in Info (ConfidentialInfo.kt onAttachmentClick). */
    @Override protected int minAttachmentOptions() { return 8; }
}
