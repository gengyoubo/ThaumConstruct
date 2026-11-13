package github.com.gengyoubo.TCt.client;

import com.mojang.blaze3d.platform.NativeImage;
import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.mfcl.api.MfclApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * 运行时通过 MFCL 读取神秘时代 4 jar 里的法杖贴图并注册成动态贴图。
 * 只有装了 MFCL 时才会加载这个类；TC4 的贴图不随本模组分发。
 */
final class Tc4Textures {
    private Tc4Textures() {}

    private static final Map<String, Optional<ResourceLocation>> CACHE = new HashMap<>();

    static Optional<ResourceLocation> get(@Nullable String name) {
        return get("models", name);
    }

    static Optional<ResourceLocation> aspect(String name) {
        return get("aspects", name);
    }

    static Optional<ResourceLocation> gui(String name) {
        return get("gui", name);
    }

    /** 读取 TC4 贴图并先用 transform 改一遍再注册，id 区分同一张原图的不同改法。 */
    static Optional<ResourceLocation> gui(String name, String id, UnaryOperator<NativeImage> transform) {
        return get("gui", name, id, transform);
    }

    private static Optional<ResourceLocation> get(String folder, @Nullable String name) {
        return get(folder, name, name, UnaryOperator.identity());
    }

    private static Optional<ResourceLocation> get(String folder, @Nullable String name, String id, UnaryOperator<NativeImage> transform) {
        if (name == null) {
            return Optional.empty();
        }
        String key = folder + "/" + id;
        Optional<ResourceLocation> cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<byte[]> bytes;
        try {
            bytes = MfclApi.legacyResource("assets/thaumcraft/textures/" + folder + "/" + name + ".png");
        } catch (RuntimeException e) {
            bytes = Optional.empty();
        }
        if (bytes.isEmpty()) {
            // 旧版运行时可能还没就绪，这时不缓存，之后再试
            if (MfclApi.runtime().isPresent()) {
                CACHE.put(key, Optional.empty());
            }
            return Optional.empty();
        }
        Optional<ResourceLocation> result;
        try {
            NativeImage image = transform.apply(NativeImage.read(bytes.get()));
            ResourceLocation location = TCt.getResource("dynamic/tc4/" + folder + "/" + id);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            result = Optional.of(location);
        } catch (Exception e) {
            result = Optional.empty();
        }
        CACHE.put(key, result);
        return result;
    }
}
