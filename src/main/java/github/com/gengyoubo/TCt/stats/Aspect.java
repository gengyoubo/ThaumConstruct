package github.com.gengyoubo.TCt.stats;

import net.minecraft.network.chat.TextColor;

/** 神秘时代的六种初始要素，顺序和颜色与 TC4 法杖提示一致。 */
public enum Aspect {
    AER("aer", 0xFFFF7E),
    TERRA("terra", 0x56C000),
    IGNIS("ignis", 0xFF5A01),
    AQUA("aqua", 0x3CD4FC),
    ORDO("ordo", 0xD5D4EC),
    PERDITIO("perditio", 0x404040);

    private final String id;
    private final TextColor color;

    Aspect(String id, int color) {
        this.id = id;
        this.color = TextColor.fromRgb(color);
    }

    public String getId() {
        return id;
    }

    public TextColor getColor() {
        return color;
    }
}
