package com.zillit.automation.tests;

import org.junit.jupiter.api.Order;

/**
 * Tools → "Info" module. A Bulletin-style chat; all cases come from
 * {@link ToolChatBaseTest}.
 */
@Order(49)
class InfoTest extends ToolChatBaseTest {
    @Override protected String moduleName() { return "Info"; }
}
