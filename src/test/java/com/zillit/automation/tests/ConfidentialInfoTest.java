package com.zillit.automation.tests;

import org.junit.jupiter.api.Order;

/**
 * Tools → "Confidential Info" module. A Bulletin-style chat; all cases come from
 * {@link ToolChatBaseTest}.
 */
@Order(48)
class ConfidentialInfoTest extends ToolChatBaseTest {
    @Override protected String moduleName() { return "Confidential Info"; }
}
