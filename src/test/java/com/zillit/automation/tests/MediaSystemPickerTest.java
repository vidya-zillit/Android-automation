package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.HomeChatPage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Document + Audio send through Android's SYSTEM pickers (opt-in, best-effort).
 *
 * These drive Android's own file/audio picker, whose UI varies by OS version
 * and vendor, so they are INHERENTLY less reliable than the app's-own-UI media
 * tests. To avoid false failures they SKIP (not fail) whenever the system
 * picker can't be navigated on the device under test. The runner pushes the
 * fixtures (zl_test_doc.pdf → /sdcard/Documents, zl_test_audio.wav →
 * /sdcard/Music) before this suite.
 */
@Order(TestOrder.MEDIA_SYSTEM_PICKER)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MediaSystemPickerTest extends BaseTest {

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    @BeforeEach
    void enterProject() {
        if (insideProject) {
            dismissPermissionPrompts();
            for (int i = 0; i < 5 && !new HomeChatPage(driver).isInputBarVisible(); i++) {
                driver.navigate().back();
                sleep(600);
            }
            return;
        }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "Could not enter a project");
        HomeChatPage home = new HomeChatPage(driver);
        assumeTrue(home.isLoaded(), "Home units strip not visible");
        assumeTrue(home.selectFirstChatUnit(), "No chat unit available");
        assumeTrue(home.isInputBarVisible(), "No posting rights — skipping");
        insideProject = true;
    }

    private HomeChatPage openTypeInSystemPicker(String optionLabel) {
        HomeChatPage home = new HomeChatPage(driver);
        home.tapAttachment();
        assumeTrue(home.isAttachmentPickerVisible(), "Attachment sheet did not open");
        assumeTrue(home.tapAttachmentOption(optionLabel), optionLabel + " option missing");
        // Grant storage permission if the system picker asks.
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        return home;
    }

    @Test
    @Order(1)
    @DisplayName("Document: pick the test PDF from the system picker and send it")
    void sendDocument() {
        HomeChatPage home = openTypeInSystemPicker("Document");
        assumeTrue(home.pickSystemFileByName("zl_test", Duration.ofSeconds(12)),
                "System document picker could not surface the test file on this device — skipping");
        // Not "posted OR uploading": an upload that never finishes satisfies that
        // forever. Require the finished state — no progress bar, no Retry, no clock.
        assertTrue(home.waitAttachmentSent(Duration.ofSeconds(60)),
                "The document should finish sending — it is " + home.attachmentSendState());
        screenshot("system-document-sent");
    }

    @Test
    @Order(2)
    @DisplayName("Audio: pick the test audio from the system picker and send it")
    void sendAudio() {
        HomeChatPage home = openTypeInSystemPicker("Audio");
        assumeTrue(home.pickSystemFileByName("zl_test", Duration.ofSeconds(12)),
                "System audio picker could not surface the test file on this device — skipping");
        // Not "posted OR uploading": an upload that never finishes satisfies that
        // forever. Require the finished state — no progress bar, no Retry, no clock.
        assertTrue(home.waitAttachmentSent(Duration.ofSeconds(60)),
                "The audio should finish sending — it is " + home.attachmentSendState());
        screenshot("system-audio-sent");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
