package github.com.gengyoubo.TCt.client;

import github.com.gengyoubo.TCt.TCt;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 根据材料挑选法杖 3D 模型的贴图（TC4 的 32x32 展开布局），优先级：
 * 1. 材料变种自己的贴图（如 rock_granite、wood_crimson）；
 * 2. 装了 MFCL 和神秘时代 4 时，木杖柄、铜/铁杖端直接用 TC4 原贴图；
 * 3. 本模组为该材料生成的贴图；
 * 4. 默认：木杖柄、铁杖端。
 */
public final class WandTextures {
    private WandTextures() {}

    private static final boolean MFCL = ModList.get().isLoaded("mfcl");
    private static final Map<String, Optional<ResourceLocation>> OWN = new ConcurrentHashMap<>();

    /** 匠魂材料 -> TC4 贴图名 */
    private static final Map<String, String> TC4_RODS = Map.of("tconstruct:wood", "wand_rod_wood");
    private static final Map<String, String> TC4_CAPS = Map.of(
            "tconstruct:copper", "wand_cap_copper",
            "tconstruct:iron", "wand_cap_iron");

    public static ResourceLocation rod(@Nullable MaterialVariantId material) {
        return resolve("rod", material, TC4_RODS, "tconstruct:wood");
    }

    public static ResourceLocation cap(@Nullable MaterialVariantId material) {
        return resolve("cap", material, TC4_CAPS, "tconstruct:iron");
    }

    private static ResourceLocation resolve(String part, @Nullable MaterialVariantId material,
                                            Map<String, String> tc4, String fallback) {
        if (material != null) {
            if (material.hasVariant()) {
                Optional<ResourceLocation> variant = own(part, suffix(material.getLocation('_')));
                if (variant.isPresent()) {
                    return variant.get();
                }
            }
            Optional<ResourceLocation> found = tc4OrOwn(part, material.getId().toString(), tc4);
            if (found.isPresent()) {
                return found.get();
            }
        }
        return tc4OrOwn(part, fallback, tc4).orElseGet(() -> texture(part, fallback.replace(':', '_')));
    }

    private static Optional<ResourceLocation> tc4OrOwn(String part, String material, Map<String, String> tc4) {
        String tc4Name = tc4.get(material);
        if (tc4Name != null && MFCL) {
            Optional<ResourceLocation> legacy = Tc4Textures.get(tc4Name);
            if (legacy.isPresent()) {
                return legacy;
            }
        }
        return own(part, material.replace(':', '_'));
    }

    private static String suffix(ResourceLocation id) {
        return id.getNamespace() + "_" + id.getPath();
    }

    private static Optional<ResourceLocation> own(String part, String suffix) {
        return OWN.computeIfAbsent(part + "/" + suffix, key -> {
            ResourceLocation texture = texture(part, suffix);
            return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()
                    ? Optional.of(texture) : Optional.empty();
        });
    }

    private static ResourceLocation texture(String part, String suffix) {
        return TCt.getResource("textures/item/wand_model/" + part + "_" + suffix + ".png");
    }
}
