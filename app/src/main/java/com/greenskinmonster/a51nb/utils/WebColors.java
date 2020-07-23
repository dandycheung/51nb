package com.greenskinmonster.a51nb.utils;

import java.util.HashMap;
import java.util.Locale;

public class WebColors {
    public static int parseColor(String colorString) {
        if (colorString.charAt(0) == '#') {
            // Use a long to avoid rollovers on #ffXXXXXX
            long color = Long.parseLong(colorString.substring(1), 16);
            if (colorString.length() == 7) {
                // Set the alpha value
                color |= 0x00000000ff000000;
            } else if (colorString.length() != 9) {
                throw new IllegalArgumentException("Unknown color");
            }
            return (int)color;
        } else {
            Integer color = sColorNameMap.get(colorString.toLowerCase(Locale.ROOT));
            if (color != null) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown color");
    }

    private static final HashMap<String, Integer> sColorNameMap;

    static {
        sColorNameMap = new HashMap<String, Integer>();
        sColorNameMap.put("AliceBlue", 0xFFF0F8FF);
        sColorNameMap.put("AntiqueWhite", 0xFFFAEBD7);
        sColorNameMap.put("Aqua", 0xFF00FFFF);
        sColorNameMap.put("Aquamarine", 0xFF7FFFD4);
        sColorNameMap.put("Azure", 0xFFF0FFFF);
        sColorNameMap.put("Beige", 0xFFF5F5DC);
        sColorNameMap.put("Bisque", 0xFFFFE4C4);
        sColorNameMap.put("Black", 0xFF000000);
        sColorNameMap.put("BlanchedAlmond", 0xFFFFEBCD);
        sColorNameMap.put("Blue", 0xFF0000FF);
        sColorNameMap.put("BlueViolet", 0xFF8A2BE2);
        sColorNameMap.put("Brown", 0xFFA52A2A);
        sColorNameMap.put("BurlyWood", 0xFFDEB887);
        sColorNameMap.put("CadetBlue", 0xFF5F9EA0);
        sColorNameMap.put("Chartreuse", 0xFF7FFF00);
        sColorNameMap.put("Chocolate", 0xFFD2691E);
        sColorNameMap.put("Coral", 0xFFFF7F50);
        sColorNameMap.put("CornflowerBlue", 0xFF6495ED);
        sColorNameMap.put("Cornsilk", 0xFFFFF8DC);
        sColorNameMap.put("Crimson", 0xFFDC143C);
        sColorNameMap.put("Cyan", 0xFF00FFFF);
        sColorNameMap.put("DarkBlue", 0xFF00008B);
        sColorNameMap.put("DarkCyan", 0xFF008B8B);
        sColorNameMap.put("DarkGoldenRod", 0xFFB8860B);
        sColorNameMap.put("DarkGray", 0xFFA9A9A9);
        sColorNameMap.put("DarkGreen", 0xFF006400);
        sColorNameMap.put("DarkKhaki", 0xFFBDB76B);
        sColorNameMap.put("DarkMagenta", 0xFF8B008B);
        sColorNameMap.put("DarkOliveGreen", 0xFF556B2F);
        sColorNameMap.put("DarkOrange", 0xFFFF8C00);
        sColorNameMap.put("DarkOrchid", 0xFF9932CC);
        sColorNameMap.put("DarkRed", 0xFF8B0000);
        sColorNameMap.put("DarkSalmon", 0xFFE9967A);
        sColorNameMap.put("DarkSeaGreen", 0xFF8FBC8F);
        sColorNameMap.put("DarkSlateBlue", 0xFF483D8B);
        sColorNameMap.put("DarkSlateGray", 0xFF2F4F4F);
        sColorNameMap.put("DarkTurquoise", 0xFF00CED1);
        sColorNameMap.put("DarkViolet", 0xFF9400D3);
        sColorNameMap.put("DeepPink", 0xFFFF1493);
        sColorNameMap.put("DeepSkyBlue", 0xFF00BFFF);
        sColorNameMap.put("DimGray", 0xFF696969);
        sColorNameMap.put("DodgerBlue", 0xFF1E90FF);
        sColorNameMap.put("FireBrick", 0xFFB22222);
        sColorNameMap.put("FloralWhite", 0xFFFFFAF0);
        sColorNameMap.put("ForestGreen", 0xFF228B22);
        sColorNameMap.put("Fuchsia", 0xFFFF00FF);
        sColorNameMap.put("Gainsboro", 0xFFDCDCDC);
        sColorNameMap.put("GhostWhite", 0xFFF8F8FF);
        sColorNameMap.put("Gold", 0xFFFFD700);
        sColorNameMap.put("GoldenRod", 0xFFDAA520);
        sColorNameMap.put("Gray", 0xFF808080);
        sColorNameMap.put("Green", 0xFF008000);
        sColorNameMap.put("GreenYellow", 0xFFADFF2F);
        sColorNameMap.put("HoneyDew", 0xFFF0FFF0);
        sColorNameMap.put("HotPink", 0xFFFF69B4);
        sColorNameMap.put("IndianRed", 0xFFCD5C5C);
        sColorNameMap.put("Indigo", 0xFF4B0082);
        sColorNameMap.put("Ivory", 0xFFFFFFF0);
        sColorNameMap.put("Khaki", 0xFFF0E68C);
        sColorNameMap.put("Lavender", 0xFFE6E6FA);
        sColorNameMap.put("LavenderBlush", 0xFFFFF0F5);
        sColorNameMap.put("LawnGreen", 0xFF7CFC00);
        sColorNameMap.put("LemonChiffon", 0xFFFFFACD);
        sColorNameMap.put("LightBlue", 0xFFADD8E6);
        sColorNameMap.put("LightCoral", 0xFFF08080);
        sColorNameMap.put("LightCyan", 0xFFE0FFFF);
        sColorNameMap.put("LightGoldenRodYellow", 0xFFFAFAD2);
        sColorNameMap.put("LightGray", 0xFFD3D3D3);
        sColorNameMap.put("LightGreen", 0xFF90EE90);
        sColorNameMap.put("LightPink", 0xFFFFB6C1);
        sColorNameMap.put("LightSalmon", 0xFFFFA07A);
        sColorNameMap.put("LightSeaGreen", 0xFF20B2AA);
        sColorNameMap.put("LightSkyBlue", 0xFF87CEFA);
        sColorNameMap.put("LightSlateGray", 0xFF778899);
        sColorNameMap.put("LightSteelBlue", 0xFFB0C4DE);
        sColorNameMap.put("LightYellow", 0xFFFFFFE0);
        sColorNameMap.put("Lime", 0xFF00FF00);
        sColorNameMap.put("LimeGreen", 0xFF32CD32);
        sColorNameMap.put("Linen", 0xFFFAF0E6);
        sColorNameMap.put("Magenta", 0xFFFF00FF);
        sColorNameMap.put("Maroon", 0xFF800000);
        sColorNameMap.put("MediumAquaMarine", 0xFF66CDAA);
        sColorNameMap.put("MediumBlue", 0xFF0000CD);
        sColorNameMap.put("MediumOrchid", 0xFFBA55D3);
        sColorNameMap.put("MediumPurple", 0xFF9370DB);
        sColorNameMap.put("MediumSeaGreen", 0xFF3CB371);
        sColorNameMap.put("MediumSlateBlue", 0xFF7B68EE);
        sColorNameMap.put("MediumSpringGreen", 0xFF00FA9A);
        sColorNameMap.put("MediumTurquoise", 0xFF48D1CC);
        sColorNameMap.put("MediumVioletRed", 0xFFC71585);
        sColorNameMap.put("MidnightBlue", 0xFF191970);
        sColorNameMap.put("MintCream", 0xFFF5FFFA);
        sColorNameMap.put("MistyRose", 0xFFFFE4E1);
        sColorNameMap.put("Moccasin", 0xFFFFE4B5);
        sColorNameMap.put("NavajoWhite", 0xFFFFDEAD);
        sColorNameMap.put("Navy", 0xFF000080);
        sColorNameMap.put("OldLace", 0xFFFDF5E6);
        sColorNameMap.put("Olive", 0xFF808000);
        sColorNameMap.put("OliveDrab", 0xFF6B8E23);
        sColorNameMap.put("Orange", 0xFFFFA500);
        sColorNameMap.put("OrangeRed", 0xFFFF4500);
        sColorNameMap.put("Orchid", 0xFFDA70D6);
        sColorNameMap.put("PaleGoldenRod", 0xFFEEE8AA);
        sColorNameMap.put("PaleGreen", 0xFF98FB98);
        sColorNameMap.put("PaleTurquoise", 0xFFAFEEEE);
        sColorNameMap.put("PaleVioletRed", 0xFFDB7093);
        sColorNameMap.put("PapayaWhip", 0xFFFFEFD5);
        sColorNameMap.put("PeachPuff", 0xFFFFDAB9);
        sColorNameMap.put("Peru", 0xFFCD853F);
        sColorNameMap.put("Pink", 0xFFFFC0CB);
        sColorNameMap.put("Plum", 0xFFDDA0DD);
        sColorNameMap.put("PowderBlue", 0xFFB0E0E6);
        sColorNameMap.put("Purple", 0xFF800080);
        sColorNameMap.put("Red", 0xFFFF0000);
        sColorNameMap.put("RosyBrown", 0xFFBC8F8F);
        sColorNameMap.put("RoyalBlue", 0xFF4169E1);
        sColorNameMap.put("SaddleBrown", 0xFF8B4513);
        sColorNameMap.put("Salmon", 0xFFFA8072);
        sColorNameMap.put("SandyBrown", 0xFFF4A460);
        sColorNameMap.put("SeaGreen", 0xFF2E8B57);
        sColorNameMap.put("SeaShell", 0xFFFFF5EE);
        sColorNameMap.put("Sienna", 0xFFA0522D);
        sColorNameMap.put("Silver", 0xFFC0C0C0);
        sColorNameMap.put("SkyBlue", 0xFF87CEEB);
        sColorNameMap.put("SlateBlue", 0xFF6A5ACD);
        sColorNameMap.put("SlateGray", 0xFF708090);
        sColorNameMap.put("Snow", 0xFFFFFAFA);
        sColorNameMap.put("SpringGreen", 0xFF00FF7F);
        sColorNameMap.put("SteelBlue", 0xFF4682B4);
        sColorNameMap.put("Tan", 0xFFD2B48C);
        sColorNameMap.put("Teal", 0xFF008080);
        sColorNameMap.put("Thistle", 0xFFD8BFD8);
        sColorNameMap.put("Tomato", 0xFFFF6347);
        sColorNameMap.put("Turquoise", 0xFF40E0D0);
        sColorNameMap.put("Violet", 0xFFEE82EE);
        sColorNameMap.put("Wheat", 0xFFF5DEB3);
        sColorNameMap.put("White", 0xFFFFFFFF);
        sColorNameMap.put("WhiteSmoke", 0xFFF5F5F5);
        sColorNameMap.put("Yellow", 0xFFFFFF00);
        sColorNameMap.put("YellowGreen", 0xFF9ACD32);
    }
}