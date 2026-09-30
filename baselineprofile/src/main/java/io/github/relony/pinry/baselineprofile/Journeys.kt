package io.github.relony.pinry.baselineprofile

import android.content.Intent
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

const val PACKAGE = "io.github.relony.pinry"
private const val TIMEOUT = 15_000L

/**
 * Makes sure the app under test is logged in. Credentials come from instrumentation arguments
 * (`-Pandroid.testInstrumentationRunnerArguments.pinryServer=…`, `pinryUser`, `pinryPassword`),
 * so they never end up in the repository.
 */
fun ensureLoggedIn() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val device = UiDevice.getInstance(instrumentation)
    val launch = instrumentation.context.packageManager.getLaunchIntentForPackage(PACKAGE)!!
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    instrumentation.context.startActivity(launch)

    // Either the feed (its toolbar has a "Home" tab) or the login form shows up.
    val deadline = SystemClock.uptimeMillis() + TIMEOUT
    while (SystemClock.uptimeMillis() < deadline) {
        if (device.hasObject(By.pkg(PACKAGE).desc("Home"))) return waitForFeed(device)
        if (device.hasObject(By.pkg(PACKAGE).text("Log in"))) break
        SystemClock.sleep(200)
    }
    val args = InstrumentationRegistry.getArguments()
    val fields = device.findObjects(By.clazz("android.widget.EditText"))
    check(fields.size == 3) { "Expected the login form, found ${fields.size} text fields" }
    fields[0].text = requireNotNull(args.getString("pinryServer")) { "pinryServer argument missing" }
    fields[1].text = requireNotNull(args.getString("pinryUser")) { "pinryUser argument missing" }
    fields[2].text = requireNotNull(args.getString("pinryPassword")) { "pinryPassword argument missing" }
    device.findObject(By.text("Log in")).click()
    waitForFeed(device)
}

/** The feed is up once its toolbar and at least one pin card are on screen. */
fun waitForFeed(device: UiDevice) {
    checkNotNull(device.wait(Until.findObject(By.pkg(PACKAGE).desc("Home")), TIMEOUT)) { "Feed did not appear" }
    checkNotNull(device.wait(Until.findObject(By.scrollable(true).hasDescendant(By.clickable(true))), TIMEOUT)) {
        "No pins in the feed"
    }
}

/** Flings through the feed and back, the path whose frames matter most. */
fun scrollFeed(device: UiDevice) {
    // Compose replaces the grid's accessibility node as it scrolls, so it is looked up for every fling.
    fun fling(direction: Direction) {
        val grid = device.findObject(By.scrollable(true))
        // Keep away from the edges, where Android treats swipes as back gestures.
        grid.setGestureMargin(device.displayWidth / 5)
        grid.fling(direction)
        device.waitForIdle()
    }
    repeat(4) { fling(Direction.DOWN) }
    repeat(2) { fling(Direction.UP) }
}

/** Opens the first pin on screen and comes back: detail screen, shared element, image loading. */
fun openPinAndBack(device: UiDevice) {
    device.findObject(By.scrollable(true)).findObject(By.clickable(true)).click()
    checkNotNull(device.wait(Until.findObject(By.text("Save")), TIMEOUT)) { "Pin detail did not open" }
    device.pressBack()
    waitForFeed(device)
}
