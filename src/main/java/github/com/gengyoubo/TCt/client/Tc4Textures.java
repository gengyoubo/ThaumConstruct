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

/**
 * 运行时通过 MFCL 读取神秘时代 4 jar 里的法杖贴图并注册成动态贴图。
 * 只有装了 MFCL 时才会加载这个类；TC4 的贴图不随本模组分发。
 */
final class Tc4Textures {
    private Tc4Textures() {}

    private static final Map<String, Optional<ResourceLocation>> CACHE = new HashMap<>();

    static Optional<ResourceLocation> get(@Nullable String name) {
        if (name == null) {
            return Optional.empty();
        }
        Optional<ResourceLocation> cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        Optional<byte[]> bytes;
        try {
            bytes = MfclApi.legacyResource("assets/thaumcraft/textures/models/" + name + ".png");
        } catch (RuntimeException e) {
            bytes = Optional.empty();
        }
        if (bytes.isEmpty()) {
            // 旧版运行时可能还没就绪，这时不缓存，之后再试
            if (MfclApi.runtime().isPresent()) {
                CACHE.put(name, Optional.empty());
            }
            return Optional.empty();
        }
        Optional<ResourceLocation> result;
        try {
            NativeImage image = NativeImage.read(bytes.get());
            ResourceLocation id = TCt.getResource("dynamic/tc4/" + name);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            result = Optional.of(id);
        } catch (Exception e) {
            result = Optional.empty();
        }
        CACHE.put(name, result);
        return result;
    }
}
