package org.mycore.iview.tests.base;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mycore.iview.tests.ViewerTestBase;
import org.mycore.iview.tests.controller.ImageOverviewController;
import org.mycore.iview.tests.controller.ImageViewerController;
import org.mycore.iview.tests.controller.SideBarController;
import org.mycore.iview.tests.controller.ToolBarController;
import org.mycore.iview.tests.model.TestDerivate;

import java.text.MessageFormat;

@Category(org.mycore.iview.tests.groups.ImageViewerTests.class)
public class SideBarIT extends ViewerTestBase {

    private static final Logger LOGGER = Logger.getLogger(SideBarIT.class);

    @Test
    public void testSideBarPresent() throws Exception {
        this.setTestName(getClassname() + "-testSideBarPresent");
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();
        SideBarController sbController = controller.getSideBarController();

        tbController.pressButton(ToolBarController.BUTTON_ID_SIDEBAR_CONTROLL);
        tbController.clickElementById(ImageOverviewController.IMAGE_OVERVIEW_SELECTOR);
        Assert.assertTrue("SideBar should be present!", sbController.assertSideBarPresent());

        tbController.clickElementByXpath(SideBarController.SIDEBAR_CLOSE_SELECTOR);
        Assert.assertFalse("SideBar should not be present!", sbController.assertSideBarPresent());
    }

    @Test
    public void testSideBarResize() throws Exception {
        this.setTestName(getClassname() + "-testSideBarResize");
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();
        SideBarController sbController = controller.getSideBarController();

        tbController.pressButton(ToolBarController.BUTTON_ID_SIDEBAR_CONTROLL);
        tbController.clickElementById(ImageOverviewController.IMAGE_OVERVIEW_SELECTOR);

        int sbWidthStart = sbController.getSideBarWidth();

        sbController.dragAndDropByXpath("//div[contains(@class,\"sidebar\")]/span[@class=\"resizer\"]", 50, 0);

        int sbWidthEnd = sbController.getSideBarWidth();

        assertLess(sbWidthEnd, sbWidthStart, "Sidebar width schould be increased!");

    }

    @Test
    public void testOverviewLayout() throws InterruptedException {
        this.setTestName(getClassname() + "-testOvervieLayout");
        this.getDriver();
        this.getAppController().openViewer(this.getDriver(), getTestDerivate());

        ImageViewerController controller = this.getViewerController();

        ToolBarController tbController = controller.getToolBarController();
        SideBarController sbController = controller.getSideBarController();

        tbController.pressButton(ToolBarController.BUTTON_ID_SIDEBAR_CONTROLL);
        tbController.clickElementById(ImageOverviewController.IMAGE_OVERVIEW_SELECTOR);

        int before = sbController.countThumbnails();
        sbController.dragAndDropByXpath("//div[contains(@class,'sidebar')]/span[@class='resizer']", 300, 0);
        Thread.sleep(1000);
        int after = sbController.countThumbnails();

        Assert.assertEquals(2 * before, after);
    }

    private void assertLess(int moreValue, int lessValue, String messagePattern) {
        String message = MessageFormat.format(messagePattern, lessValue, moreValue);
        LOGGER.debug(message);
        Assert.assertTrue(message, lessValue < moreValue);
    }

    @Override
    public TestDerivate getTestDerivate() {
        return BaseTestConstants.RGB_TEST_DERIVATE;
    }
}
