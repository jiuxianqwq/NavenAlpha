package com.heypixel.heypixelmod.obsoverlay.utils.skia.image;

import com.heypixel.heypixelmod.obsoverlay.utils.skia.context.SkiaContext;
import com.heypixel.heypixelmod.obsoverlay.utils.skia.utils.SkiaUtils;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.SurfaceOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ImageHelper {

    private final Map<String, Image> images = new HashMap<>();
    private final Map<Integer, Image> textures = new HashMap<>();

    public boolean load(int texture, float width, float height, SurfaceOrigin origin) {

        if (!textures.containsKey(texture)) {
            Image image = Image.adoptGLTextureFrom(SkiaContext.getContext(), texture, GL11.GL_TEXTURE_2D, (int) width,
                    (int) height, GL11.GL_RGBA8, origin, ColorType.RGBA_8888);
            textures.put(texture, image);
        }

        return true;
    }

    public boolean load(ResourceLocation ResourceLocation) {

        if (!images.containsKey(ResourceLocation.getPath())) {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            Resource resource;
            try {
                resource = resourceManager.getResourceOrThrow(ResourceLocation);
                try (InputStream inputStream = resource.open()) {

                    byte[] imageData = inputStream.readAllBytes();
                    Image image = Image.makeDeferredFromEncodedBytes(imageData);
                    if (image == null) {
                        return false;
                    }
                    images.put(ResourceLocation.getPath(), image);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }
        return true;
    }

    public boolean load(String filePath) {
        if (!images.containsKey(filePath)) {
            Optional<byte[]> encodedBytes = SkiaUtils.convertToBytes(filePath);
            if (encodedBytes.isPresent()) {
                images.put(filePath, Image.makeDeferredFromEncodedBytes(encodedBytes.get()));
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean load(File file) {

        if (!images.containsKey(file.getName())) {

            try {
                byte[] encoded = org.apache.commons.io.IOUtils.toByteArray(new FileInputStream(file));
                images.put(file.getName(), Image.makeDeferredFromEncodedBytes(encoded));
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public Image get(String path) {

        if (images.containsKey(path)) {
            return images.get(path);
        }

        return null;
    }

    public Image get(int texture) {

        if (textures.containsKey(texture)) {
            return textures.get(texture);
        }

        return null;
    }
}
