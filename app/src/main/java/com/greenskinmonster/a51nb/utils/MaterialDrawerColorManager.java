package com.greenskinmonster.a51nb.utils;

import android.graphics.Color;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.holder.ColorHolder;
import com.mikepenz.materialdrawer.model.BaseDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MaterialDrawerColorManager {
    // TODO: 可能弱引用才对
    protected ArrayList<Drawer> mDrawers;

    public boolean addDrawer(Drawer drawer) {
        if (drawer == null)
            return false;

        if (mDrawers == null)
            mDrawers = new ArrayList<>();

        return mDrawers.add(drawer);
    }

    public boolean removeDrawer(Drawer drawer) {
        if (drawer == null)
            return true;

        if (mDrawers == null)
            return false;

        return mDrawers.remove(drawer);
    }

    //
    // MaterialDrawer 的 BaseDrawerItem 类中共有七个颜色，其设置方式都一样
    // 七种颜色是：
    // 1. selectedColor
    // 2. textColor
    // 3. selectedTextColor
    // 4. disabledtextColor
    // 5. iconColor
    // 6. selectedIconColor
    // 7. disabledIconColor
    //

    public enum ColorType {
        SELECTED_COLOR,
        TEXT_COLOR,
        SELECTED_TEXT_COLOR,
        DISABLED_TEXT_COLOR,
        ICON_COLOR,
        SELECTED_ICON_COLOR,
        DISABLED_ICON_COLOR,
    }

    // 0. 通用处理方式
    public boolean setColor(int color, ColorType ct) {
        if (mDrawers == null)
            return true;

        for (Drawer drawer : mDrawers)
            setColor(drawer, color, ct);

        return true;
    }

    public static boolean setColor(Drawer drawer, int color, ColorType ct) {
        if (drawer == null)
            return false;

        List<IDrawerItem> items = drawer.getDrawerItems();
        if (items == null)
            return true;

        for (IDrawerItem item : items) {
            BaseDrawerItem bdi;
            try {
                bdi = (BaseDrawerItem) item;
            } catch (Exception e) {
                continue;
            }

            setItemColor(bdi, color, ct);
        }

        return true;
    }

    protected static final String[] mColorFieldNames = {
            "selectedColor",
            "textColor",
            "selectedTextColor",
            "disabledTextColor",
            "iconColor",
            "selectedIconColor",
            "disabledIconColor",
    };

    public static boolean setItemColor(BaseDrawerItem item, int color, ColorType ct) {
        if (item == null)
            return false;

        ColorHolder ch = ColorHolder.fromColor(color);

        try {
            Class cls = BaseDrawerItem.class;
            Field mask = cls.getDeclaredField(mColorFieldNames[ct.ordinal()]);
            mask.setAccessible(true);
            mask.set(item, ch);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    // 1. 处理 selectedColor
    public boolean setSelectedColor(int color) {
        return setColor(color, ColorType.SELECTED_COLOR);
    }

    public static boolean setSelectedColor(Drawer drawer, int color) {
        return setColor(drawer, color, ColorType.SELECTED_COLOR);
    }

    public static boolean setItemSelectedColor(BaseDrawerItem item, int color) {
        return setItemColor(item, color, ColorType.SELECTED_COLOR);
    }

    // 2. 处理 textColor
    public boolean setTextColor(int color) {
        return setColor(color, ColorType.TEXT_COLOR);
    }

    public static boolean setTextColor(Drawer drawer, int color) {
        return setColor(drawer, color, ColorType.TEXT_COLOR);
    }

    public static boolean setItemTextColor(BaseDrawerItem item, int color) {
        return setItemColor(item, color, ColorType.TEXT_COLOR);
    }

    // 3. 处理 selectedTextColor
    public boolean setSelectedTextColor(int color) {
        return setColor(color, ColorType.SELECTED_TEXT_COLOR);
    }

    public static boolean setSelectedTextColor(Drawer drawer, int color) {
        return setColor(drawer, color, ColorType.SELECTED_TEXT_COLOR);
    }

    public static boolean setItemSelectedTextColor(BaseDrawerItem item, int color) {
        return setItemColor(item, color, ColorType.SELECTED_TEXT_COLOR);
    }

    // 4. 处理 disabledTextColor
    public boolean setDisabledTextColor(int color) {
        return setColor(color, ColorType.DISABLED_TEXT_COLOR);
    }

    public static boolean setDisabledTextColor(Drawer drawer, int color) {
        return setColor(drawer, color, ColorType.DISABLED_TEXT_COLOR);
    }

    public static boolean setItemDisabledTextColor(BaseDrawerItem item, int color) {
        return setItemColor(item, color, ColorType.DISABLED_TEXT_COLOR);
    }

    // 5. 处理 iconColor
    public boolean setIconColor(int color) {
        return setColor(color, ColorType.ICON_COLOR);
    }

    public static boolean setIconColor(Drawer drawer, int color) {
        return setColor(drawer, color, ColorType.ICON_COLOR);
    }

    public static boolean setItemIconColor(BaseDrawerItem item, int color) {
        return setItemColor(item, color, ColorType.ICON_COLOR);
    }

    // 6. 处理 selectedIconColor
    public boolean setSelectedIconColor(int color) {
        return setColor(color, ColorType.SELECTED_ICON_COLOR);
    }

    public static boolean setSelectedIconColor(Drawer drawer, int color) {
        return setColor(drawer, color, ColorType.SELECTED_ICON_COLOR);
    }

    public static boolean setItemSelectedIconColor(BaseDrawerItem item, int color) {
        return setItemColor(item, color, ColorType.SELECTED_ICON_COLOR);
    }

    // 7. 处理 disabledIconColor
    public boolean setDisabledIconColor(int color) {
        return setColor(color, ColorType.DISABLED_ICON_COLOR);
    }

    public static boolean setDisabledIconColor(Drawer drawer, int color) {
        return setColor(drawer, color, ColorType.DISABLED_ICON_COLOR);
    }

    public static boolean setItemDisabledIconColor(BaseDrawerItem item, int color) {
        return setItemColor(item, color, ColorType.DISABLED_ICON_COLOR);
    }

    /*
    // 1. 处理 selectedColor
    public boolean setSelectedColor(int color) {
        if (mDrawers == null)
            return true;

        for (Drawer drawer : mDrawers)
            setSelectedColor(drawer, color);

        return true;
    }

    public static boolean setSelectedColor(Drawer drawer, int color) {
        if (drawer == null)
            return false;

        List<IDrawerItem> items = drawer.getDrawerItems();
        if (items == null)
            return true;

        for (IDrawerItem item : items) {
            BaseDrawerItem bdi = (BaseDrawerItem) item;
            if (bdi == null)
                continue;

            setItemSelectedColor(bdi, color);
        }

        return true;
    }

    public static boolean setItemSelectedColor(BaseDrawerItem item, int color) {
        if (item == null)
            return false;

        ColorHolder ch = ColorHolder.fromColor(color);

        try {
            Class cls = BaseDrawerItem.class;
            Field mask = cls.getDeclaredField("selectedColor");
            mask.setAccessible(true);
            mask.set(item, ch);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
    // */

    // 工具方法
    public static int getRandomColor() {
        int r = 0;
        int g = 0;
        int b = 0;

        Random random = new Random();
        for(int i=0; i<2; i++) {
            int t = random.nextInt(16);
            r = r * 16 + t;

            t = random.nextInt(16);
            g = g * 16 + t;

            t = random.nextInt(16);
            b = b * 16 + t;
        }

        return Color.rgb(r, g, b);
    }
}
