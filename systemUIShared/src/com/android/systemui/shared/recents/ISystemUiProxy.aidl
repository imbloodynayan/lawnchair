/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.shared.recents;

import android.graphics.Bitmap;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.MotionEvent;
import com.android.internal.util.ScreenshotRequest;

import com.android.systemui.shared.recents.model.Task;

/**
 * Temporary callbacks into SystemUI.
 */
interface ISystemUiProxy {

    /**
     * Begins screen pinning on the provided {@param taskId}.
     */
    oneway void startScreenPinning(int taskId) = 1;

    /**
     * Notifies SystemUI that Overview is shown.
     */
    oneway void onOverviewShown(boolean fromHome) = 6;

    /**
     * Proxies motion events from the homescreen UI to the status bar. Only called when
     * swipe down is detected on WORKSPACE. The sender guarantees the following order of events on
     * the tracking pointer.
     *
     * Normal gesture: DOWN, MOVE/POINTER_DOWN/POINTER_UP)*, UP or CANCLE
     */
    oneway void onStatusBarTouchEvent(in MotionEvent event) = 9;

    /**
     * Proxies the assistant gesture's progress started from navigation bar.
     */
    oneway void onAssistantProgress(float progress) = 12;

    /**
    * Proxies the assistant gesture fling velocity (in pixels per millisecond) upon completion.
    * Velocity is 0 for drag gestures.
    */
    oneway void onAssistantGestureCompletion(float velocity) = 18;

    /**
    * Get the secondary split screen app's rectangle when not minimized.
    */
    Rect getNonMinimizedSplitScreenSecondaryBounds() = 7;

    /**
    * Creates a new gesture monitor
    */
    Bundle monitorGestureInput(String name, int displayId) = 14;

    /**
     * Start the assistant.
     */
    oneway void startAssistant(in Bundle bundle) = 13;

    /**
     * Indicates that the given Assist invocation types should be handled by Launcher via
     * OverviewProxy#onAssistantOverrideInvoked and should not be invoked by SystemUI.
     *
     * @param invocationTypes The invocation types that will henceforth be handled via
     *         OverviewProxy (Launcher); other invocation types should be handled by SysUI.
     */
    oneway void setAssistantOverridesRequested(in int[] invocationTypes) = 53;

    /**
     * Notifies that the accessibility button in the system's navigation area has been clicked
     */
    oneway void notifyAccessibilityButtonClicked(int displayId) = 15;

    /**
     * Notifies that the accessibility button in the system's navigation area has been long clicked
     */
    oneway void notifyAccessibilityButtonLongClicked() = 16;

    /**
     * Ends the system screen pinning.
     */
    oneway void stopScreenPinning() = 17;

    /**
     * Handle the provided image as if it was a screenshot.
     *
     * Deprecated, use handleImageBundleAsScreenshot with image bundle and UserTask
     * @deprecated
     */
     void handleImageAsScreenshot(in Bitmap screenImage, in Rect locationInScreen,
                  in Insets visibleInsets, int taskId) = 21;

     /**
     * Sets the split-screen divider minimized state
     */
     void setSplitScreenMinimized(boolean minimized) = 22;

     /*
     * Notifies that the swipe-to-home (recents animation) is finished.
     */
    void notifySwipeToHomeFinished() = 23;

    /** Notifies that a swipe-up gesture has started */
    void notifySwipeUpGestureStarted() = 46;

    /**
     * Notifies that quickstep will switch to a new task
     * @param rotation indicates which Surface.Rotation the gesture was started in
     */
    oneway void notifyPrioritizedRotation(int rotation) = 25;

    /**
     * Notifies to expand notification panel.
     */
    oneway void expandNotificationPanel() = 29;

    /**
     * Notifies SystemUI to invoke Back.
     */
    oneway void onBackPressed() = 44;

    /** Sets home rotation enabled. */
    oneway void setHomeRotationEnabled(boolean enabled) = 45;

    /** Notifies when taskbar status updated */
    oneway void notifyTaskbarStatus(boolean visible, boolean stashed) = 47;

    /**
     * Notifies sysui when taskbar requests autoHide to stop auto-hiding
     * If called to suspend, caller is also responsible for calling this method to un-suspend
     * @param suspend should be true to stop auto-hide, false to resume normal behavior
     */
    oneway void notifyTaskbarAutohideSuspend(boolean suspend) = 48;

    /**
     * Notifies SystemUI to invoke IME Switcher.
     */
    oneway void onImeSwitcherPressed() = 49;

    /**
     * Notifies to toggle notification panel.
     */
    oneway void toggleNotificationPanel() = 50;

    /**
     * Handle the screenshot request.
     */
    oneway void takeScreenshot(in ScreenshotRequest request) = 51;

    /**
     * Handle the provided image as if it was a screenshot.
     */
    void handleImageBundleAsScreenshot(in Bundle screenImageBundle, in Rect locationInScreen,
                  in Insets visibleInsets, in Task.TaskKey task) = 26;

    /**
     * Dispatches trackpad status bar motion event to the notification shade. Currently these events
     * are from the input monitor in {@link TouchInteractionService}. This is different from
     * {@link #onStatusBarTouchEvent} above in that, this directly dispatches motion events to the
     * notification shade, while {@link #onStatusBarTouchEvent} relies on setting the launcher
     * window slippery to allow the frameworks to route those events after passing the initial
     * threshold.
     */
    oneway void onStatusBarTrackpadEvent(in MotionEvent event) = 52;

    // Next id = 54
}
