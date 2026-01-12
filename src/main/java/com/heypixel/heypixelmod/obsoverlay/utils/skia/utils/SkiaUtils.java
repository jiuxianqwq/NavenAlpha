package com.heypixel.heypixelmod.obsoverlay.utils.skia.utils;

import io.github.humbleui.skija.Data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class SkiaUtils {

    public static Optional<byte[]> convertToBytes(String path) {
        try (InputStream inputStream = getResourceAsStream(path)) {
            return Optional.of(inputStream.readAllBytes());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Optional<Data> convertToData(String path) {
        return convertToBytes(path).map(Data::makeFromBytes);
    }

    public static InputStream getResourceAsStream(String path) {
        InputStream inputStream = SkiaUtils.class.getResourceAsStream(path);
        if (inputStream == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        return inputStream;
    }
}
