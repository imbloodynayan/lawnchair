/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.launcher3.views;

import static androidx.core.content.ContextCompat.getColorStateList;

import static com.android.launcher3.LauncherState.EDIT_MODE;
import static com.android.launcher3.config.FeatureFlags.ENABLE_MATERIAL_U_POPUP;
import static com.android.launcher3.config.FeatureFlags.MULTI_SELECT_EDIT_MODE;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.IGNORE;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_SETTINGS_BUTTON_TAP_OR_LONGPRESS;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_WIDGETSTRAY_BUTTON_TAP_OR_LONGPRESS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.logging.StatsLogManager.EventEnum;
import com.android.launcher3.model.WidgetsModel;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.popup.ArrowPopup;
import com.android.launcher3.shortcuts.DeepShortcutView;
import com.android.launcher3.testing.TestLogging;
import com.android.launcher3.testing.shared.TestProtocol;
import com.android.launcher3.widget.picker.WidgetsFullSheet;
import com.patrykmichalik.opto.domain.Preference;
import com.patrykmichalik.opto.core.PreferenceExtensionsKt;

import java.util.ArrayList;
import java.util.List;

import app.lawnchair.preferences2.PreferenceManager2;

/**
 * Popup shown on long pressing an empty space in launcher
 *
 * @param <T> The context showing this popup.
 */
public class OptionsPopupView<T extends Context & ActivityContext> extends ArrowPopup<T>
        implements OnClickListener, OnLongClickListener {

    // An intent extra to indicate the horizontal scroll of the wallpaper.
    private static final String EXTRA_WALLPAPER_OFFSET = "com.android.launcher3.WALLPAPER_OFFSET";
    private static final String EXTRA_WALLPAPER_FLAVOR = "com.android.launcher3.WALLPAPER_FLAVOR";
    // An intent extra to indicate the launch source by launcher.
    private static final String EXTRA_WALLPAPER_LAUNCH_SOURCE = "com.android.wallpaper.LAUNCH_SOURCE";

    private final ArrayMap<View, OptionItem> mItemMap = new ArrayMap<>();
    private RectF mTargetRect;
    private boolean mShouldAddArrow;

    public OptionsPopupView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptionsPopupView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTargetRect(RectF targetRect) {
        mTargetRect = targetRect;
    }

    @Override
    public void onClick(View view) {
        handleViewClick(view);
    }

    @Override
    public boolean onLongClick(View view) {
        return handleViewClick(view);
    }

    private boolean handleViewClick(View view) {
        OptionItem item = mItemMap.get(view);
        if (item == null) {
            return false;
        }
        if (item.eventId.getId() > 0) {
            mActivityContext.getStatsLogManager().logger().log(item.eventId);
        }
        if (item.clickListener.onLongClick(view)) {
            close(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        if (getPopupContainer().isEventOverView(this, ev)) {
            return false;
        }
        close(true);
        return true;
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_OPTIONS_POPUP) != 0;
    }

    public void setShouldAddArrow(boolean shouldAddArrow) {
        mShouldAddArrow = shouldAddArrow;
    }

    @Override
    protected boolean shouldAddArrow() {
        return mShouldAddArrow;
    }

    @Override
    protected void getTargetObjectLocation(Rect outPos) {
        mTargetRect.roundOut(outPos);
    }

    @Override
    public void assignMarginsAndBackgrounds(ViewGroup viewGroup) {
        if (ENABLE_MATERIAL_U_POPUP.get()) {
            assignMarginsAndBackgrounds(viewGroup,
                    mColors[0]);
        } else {
            assignMarginsAndBackgrounds(viewGroup, Color.TRANSPARENT);
        }
    }

    public static <T extends Context & ActivityContext> OptionsPopupView<T> show(
            ActivityContext activityContext,
            RectF targetRect,
            List<OptionItem> items,
            boolean shouldAddArrow) {
        return show(activityContext, targetRect, items, shouldAddArrow, 0 /* width */);
    }

    public static <T extends Context & ActivityContext> OptionsPopupView<T> show(
            ActivityContext activityContext,
            RectF targetRect,
            List<OptionItem> items,
            boolean shouldAddArrow,
            int width) {
        OptionsPopupView<T> popup = (OptionsPopupView<T>) activityContext.getLayoutInflater()
                .inflate(R.layout.longpress_options_menu, activityContext.getDragLayer(), false);
        popup.mTargetRect = targetRect;
        popup.setShouldAddArrow(shouldAddArrow);

        for (OptionItem item : items) {
            DeepShortcutView view = popup.inflateAndAdd(R.layout.system_shortcut, popup);
            if (width > 0) {
                view.getLayoutParams().width = width;
            }
            view.getIconView().setBackgroundDrawable(item.icon);
            view.getBubbleText().setText(item.label);
            view.setOnClickListener(popup);
            view.setOnLongClickListener(popup);
            popup.mItemMap.put(view, item);
        }

        popup.show();
        return popup;
    }

    /**
     * Returns the list of supported actions
     */
    public static ArrayList<OptionItem> getOptions(Launcher launcher) {
        PreferenceManager2 preferenceManager2 = PreferenceManager2.getInstance(launcher);
        boolean lockHomeScreen = PreferenceExtensionsKt.firstBlocking(preferenceManager2.getLockHomeScreen());
        boolean showLockToggle = PreferenceExtensionsKt
                .firstBlocking(preferenceManager2.getLockHomeScreenButtonOnPopUp());
        boolean showSystemSettings = PreferenceExtensionsKt
                .firstBlocking(preferenceManager2.getShowSystemSettingsEntryOnPopUp());
        boolean showEditMode = PreferenceExtensionsKt.firstBlocking(preferenceManager2.getEditHomeScreenButtonOnPopUp());

        ArrayList<OptionItem> options = new ArrayList<>();
        if (showLockToggle) {
            options.add(new OptionItem(launcher,
                    lockHomeScreen ? R.string.home_screen_unlock : R.string.home_screen_lock,
                    lockHomeScreen ? R.drawable.ic_lock_open : R.drawable.ic_lock,
                    IGNORE,
                    OptionsPopupView::toggleHomeScreenLock));
        }
        if (showSystemSettings) {
            options.add(new OptionItem(launcher,
                    R.string.system_settings,
                    R.drawable.ic_setting,
                    IGNORE,
                    OptionsPopupView::startSystemSettings));
        }

        options.add(new OptionItem(launcher,
                R.string.styles_wallpaper_button_text,
                R.drawable.ic_palette,
                IGNORE,
                OptionsPopupView::startWallpaperPicker));
        if (!lockHomeScreen && !WidgetsModel.GO_DISABLE_WIDGETS) {
            options.add(new OptionItem(launcher,
                    R.string.widget_button_text,
                    R.drawable.ic_widget,
                    LAUNCHER_WIDGETSTRAY_BUTTON_TAP_OR_LONGPRESS,
                    OptionsPopupView::onWidgetsClicked));
        }
        if (!lockHomeScreen && showEditMode) {
            options.add(new OptionItem(launcher,
                    R.string.edit_home_screen,
                    R.drawable.enter_home_gardening_icon,
                    LAUNCHER_SETTINGS_BUTTON_TAP_OR_LONGPRESS,
                    OptionsPopupView::enterHomeGardening));
        }
        options.add(new OptionItem(launcher,
                R.string.settings_button_text,
                R.drawable.ic_home_screen,
                LAUNCHER_SETTINGS_BUTTON_TAP_OR_LONGPRESS,
                OptionsPopupView::startSettings));
        return options;
    }

    private static boolean enterHomeGardening(View view) {
        Launcher launcher = Launcher.getLauncher(view.getContext());
        launcher.getStateManager().goToState(EDIT_MODE);
        return true;
    }

    private static boolean onWidgetsClicked(View view) {
        return openWidgets(Launcher.getLauncher(view.getContext())) != null;
    }

    /** Returns WidgetsFullSheet that was opened, or null if nothing was opened. */
    @Nullable
    public static WidgetsFullSheet openWidgets(Launcher launcher) {
        if (launcher.getPackageManager().isSafeMode()) {
            Toast.makeText(launcher, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
            return null;
        } else {
            AbstractFloatingView floatingView = AbstractFloatingView.getTopOpenViewWithType(
                    launcher, TYPE_WIDGETS_FULL_SHEET);
            if (floatingView != null) {
                return (WidgetsFullSheet) floatingView;
            }
            return WidgetsFullSheet.show(launcher, true /* animated */);
        }
    }

    private static boolean startSettings(View view) {
        TestLogging.recordEvent(TestProtocol.SEQUENCE_MAIN, "start: startSettings");
        Launcher launcher = Launcher.getLauncher(view.getContext());
        Intent intent = new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                .setPackage(launcher.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        launcher.startActivitySafely(view, intent, placeholderInfo(intent));
        return true;
    }

    /**
     * Event handler for the wallpaper picker button that appears after a long press
     * on the home screen.
     */
    private static boolean startWallpaperPicker(View v) {
        Launcher launcher = Launcher.getLauncher(v.getContext());
        if (!Utilities.isWallpaperAllowed(launcher)) {
            String message = launcher.getStringCache() != null
                    ? launcher.getStringCache().disabledByAdminMessage
                    : launcher.getString(R.string.msg_disabled_by_admin);
            Toast.makeText(launcher, message, Toast.LENGTH_SHORT).show();
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_WALLPAPER_OFFSET,
                        launcher.getWorkspace().getWallpaperOffsetForCenterPage())
                .putExtra(EXTRA_WALLPAPER_LAUNCH_SOURCE, "app_launched_launcher");
        if (!Utilities.showStyleWallpapers(launcher)) {
            intent.putExtra(EXTRA_WALLPAPER_FLAVOR, "wallpaper_only");
        } else {
            intent.putExtra(EXTRA_WALLPAPER_FLAVOR, "focus_wallpaper");
        }
        return launcher.startActivitySafely(v, intent, placeholderInfo(intent)) != null;
    }

    private static boolean toggleHomeScreenLock(View v) {
        Context context = v.getContext();
        PreferenceManager2 preferenceManager2 = PreferenceManager2.getInstance(context);
        boolean oldValue = PreferenceExtensionsKt.firstBlocking(preferenceManager2.getLockHomeScreen());
        PreferenceExtensionsKt.setBlocking(preferenceManager2.getLockHomeScreen(), !oldValue);
        return true;
    }

    private static boolean startSystemSettings(View view) {
        final Launcher launcher = Launcher.getLauncher(view.getContext());
        final Intent intent = new Intent(Settings.ACTION_SETTINGS);
        launcher.startActivity(intent);
        return true;
    }

    static WorkspaceItemInfo placeholderInfo(Intent intent) {
        WorkspaceItemInfo placeholderInfo = new WorkspaceItemInfo();
        placeholderInfo.intent = intent;
        placeholderInfo.container = LauncherSettings.Favorites.CONTAINER_SETTINGS;
        return placeholderInfo;
    }

    public static class OptionItem {

        // Used to create AccessibilityNodeInfo in AccessibilityActionsView.java.
        public final int labelRes;

        public final CharSequence label;
        public final Drawable icon;
        public final EventEnum eventId;
        public final OnLongClickListener clickListener;

        public OptionItem(Context context, int labelRes, int iconRes, EventEnum eventId,
                OnLongClickListener clickListener) {
            this.labelRes = labelRes;
            this.label = context.getText(labelRes);
            this.icon = ContextCompat.getDrawable(context, iconRes);
            this.eventId = eventId;
            this.clickListener = clickListener;
        }

        public OptionItem(CharSequence label, Drawable icon, EventEnum eventId,
                OnLongClickListener clickListener) {
            this.labelRes = 0;
            this.label = label;
            this.icon = icon;
            this.eventId = eventId;
            this.clickListener = clickListener;
        }
    }
}
