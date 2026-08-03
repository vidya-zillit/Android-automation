package com.zillit.automation.tests;

import com.zillit.automation.core.TestOrder;

import org.junit.jupiter.api.Order;

/**
 * Tools → "Info" module. A Bulletin-style chat; all cases come from
 * {@link ToolChatBaseTest}.
 */
@Order(TestOrder.TOOL_INFO)
class InfoTest extends ToolChatBaseTest {
    @Override protected String moduleName() { return "Info"; }
}
