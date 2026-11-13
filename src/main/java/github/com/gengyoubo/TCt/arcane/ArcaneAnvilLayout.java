package github.com.gengyoubo.TCt.arcane;

/**
 * GUI coordinates of the arcane anvil, shared by {@link ArcaneAnvilMenu} and the client screen.
 * The upper left holds the TC4 arcane workbench cloth (132x136, cut from TC4's gui_arcaneworkbench.png at
 * {@link #CLOTH_U},{@link #CLOTH_V}); a TC4 texture coordinate maps to the GUI by adding {@link #TC4_DX},{@link #TC4_DY}.
 */
public final class ArcaneAnvilLayout {
    private ArcaneAnvilLayout() { }

    public static final int WIDTH = 200, HEIGHT = 250;
    /** How far the player inventory moved down compared to the original 184px tinker station. */
    public static final int INVENTORY_SHIFT = HEIGHT - 184;

    public static final int CLOTH_X = 4, CLOTH_Y = 18, CLOTH_U = 6, CLOTH_V = 5, CLOTH_W = 132, CLOTH_H = 136;
    public static final int TC4_DX = CLOTH_X - CLOTH_U, TC4_DY = CLOTH_Y - CLOTH_V;
    /** Offset applied to Tinkers' own layouts, moving their slot area (centred on 42,50) to the cloth centre. */
    public static final int LAYOUT_DX = 72 + TC4_DX - 42, LAYOUT_DY = 72 + TC4_DY - 50;

    // slot item positions
    public static final int WAND_X = 143, WAND_Y = 23;
    public static final int RESULT_X = 171, RESULT_Y = 51;
    public static final int ARMOR_X = 177;
    /** Armor slots line up with the three inventory rows and the hotbar. */
    public static final int[] ARMOR_Y = {102 + INVENTORY_SHIFT, 120 + INVENTORY_SHIFT, 138 + INVENTORY_SHIFT, 160 + INVENTORY_SHIFT};
    // arrow between the cloth and the result, below the wand box
    public static final int ARROW_X = 140, ARROW_Y = 52;
}
