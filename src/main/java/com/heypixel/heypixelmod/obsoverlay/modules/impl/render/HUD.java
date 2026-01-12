package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;

import java.awt.*;

@ModuleInfo(
        name = "HUD",
        cnName = "抬头显示器",
        description = "Displays information on your screen",
        category = Category.RENDER
)
public class HUD extends Module {
    public static final int headerColor = new Color(150, 45, 45, 255).getRGB();
    public static final int bodyColor = new Color(0, 0, 0, 120).getRGB();
    public static final int backgroundColor = new Color(0, 0, 0, 40).getRGB();
    public BooleanValue moduleToggleSound = ValueBuilder.create(this, "Module Toggle Sound").setDefaultBooleanValue(true).build().getBooleanValue();
//    public BooleanValue arrayList = ValueBuilder.create(this, "Array List").setDefaultBooleanValue(true).build().getBooleanValue();
//    public BooleanValue prettyModuleName = ValueBuilder.create(this, "Pretty Module Name")
//            .setOnUpdate(value -> Module.update = true)
//            .setVisibility(this.arrayList::getCurrentValue)
//            .setDefaultBooleanValue(false)
//            .build()
//            .getBooleanValue();
//    public BooleanValue hideRenderModules = ValueBuilder.create(this, "Hide Render Modules")
//            .setOnUpdate(value -> Module.update = true)
//            .setVisibility(this.arrayList::getCurrentValue)
//            .setDefaultBooleanValue(false)
//            .build()
//            .getBooleanValue();
//    public BooleanValue rainbow = ValueBuilder.create(this, "Rainbow")
//            .setDefaultBooleanValue(true)
//            .setVisibility(this.arrayList::getCurrentValue)
//            .build()
//            .getBooleanValue();
//    public FloatValue rainbowSpeed = ValueBuilder.create(this, "Rainbow Speed")
//            .setVisibility(this.arrayList::getCurrentValue)
//            .setMinFloatValue(1.0F)
//            .setMaxFloatValue(20.0F)
//            .setDefaultFloatValue(10.0F)
//            .setFloatStep(0.1F)
//            .build()
//            .getFloatValue();
//    public FloatValue rainbowOffset = ValueBuilder.create(this, "Rainbow Offset")
//            .setVisibility(this.arrayList::getCurrentValue)
//            .setMinFloatValue(1.0F)
//            .setMaxFloatValue(20.0F)
//            .setDefaultFloatValue(10.0F)
//            .setFloatStep(0.1F)
//            .build()
//            .getFloatValue();
//    public ModeValue arrayListDirection = ValueBuilder.create(this, "ArrayList Direction")
//            .setVisibility(this.arrayList::getCurrentValue)
//            .setDefaultModeIndex(0)
//            .setModes("Right", "Left")
//            .build()
//            .getModeValue();
//    public FloatValue xOffset = ValueBuilder.create(this, "X Offset")
//            .setVisibility(this.arrayList::getCurrentValue)
//            .setMinFloatValue(-100.0F)
//            .setMaxFloatValue(100.0F)
//            .setDefaultFloatValue(1.0F)
//            .setFloatStep(1.0F)
//            .build()
//            .getFloatValue();
//    public FloatValue yOffset = ValueBuilder.create(this, "Y Offset")
//            .setVisibility(this.arrayList::getCurrentValue)
//            .setMinFloatValue(1.0F)
//            .setMaxFloatValue(100.0F)
//            .setDefaultFloatValue(1.0F)
//            .setFloatStep(1.0F)
//            .build()
//            .getFloatValue();
//    public FloatValue arrayListSize = ValueBuilder.create(this, "ArrayList Size")
//            .setVisibility(this.arrayList::getCurrentValue)
//            .setDefaultFloatValue(0.4F)
//            .setFloatStep(0.01F)
//            .setMinFloatValue(0.1F)
//            .setMaxFloatValue(1.0F)
//            .build()
//            .getFloatValue();
//    List<Module> renderModules;
//    float width;
//    float watermarkHeight;
//    List<Vector4f> blurMatrices = new ArrayList<>();
//
//    public String getModuleDisplayName(Module module) {
//        String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
//        return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
//    }
//
//    @EventTarget
//    public void onShader(EventShader e) {
//
//        if (this.arrayList.getCurrentValue()) {
//            for (Vector4f blurMatrix : this.blurMatrices) {
//                RenderUtils.fillBound(e.stack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
//            }
//        }
//    }
//
//    @EventTarget
//    public void onSkia(EventRenderSkia event) {
//
//        if (this.arrayList.getCurrentValue()) {
//            Path path = new Path();
//            for (Vector4f blurMatrix : this.blurMatrices) {
//                path.addRect(Rect.makeXYWH(blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w()));
//            }
//
//            Paint paint = new Paint();
//            paint.setImageFilter(ImageFilter.makeBlur(2.5F, 2.5F, FilterTileMode.DECAL));
//
//            Skia.save();
//
//            Skia.clipPath(path, ClipMode.DIFFERENCE, true);
//
//            for (Vector4f blurMatrix : this.blurMatrices) {
//
//                int colorValue = RenderUtils.getRainbowOpaque(
//                        (int) (-blurMatrix.y() * this.rainbowOffset.getCurrentValue()),
//                        1.0F,
//                        1.0F,
//                        (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
//                );
//
//                Color rectColor = new Color(colorValue);
//                if (!rainbow.getCurrentValue()) rectColor = Color.white;
//
//                paint.setColor(rectColor.getRGB());
//
//                Skia.getCanvas().drawRect(
//                        Rect.makeXYWH(blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w()),
//                        paint
//                );
//
//            }
//
//            Skia.restore();
//
//        }
//    }
//
//    @EventTarget
//    public void onRender(EventRender2D e) {
//        CustomTextRenderer font = Fonts.miSans;
//
//        this.blurMatrices.clear();
//        if (this.arrayList.getCurrentValue()) {
//            e.stack().pushPose();
//            ModuleManager moduleManager = Naven.getInstance().getModuleManager();
//            if (update || this.renderModules == null) {
//                this.renderModules = new ArrayList<>(moduleManager.getModules());
//                if (this.hideRenderModules.getCurrentValue()) {
//                    this.renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
//                }
//
//                this.renderModules.sort((o1, o2) -> {
//                    float o1Width = font.getWidth(this.getModuleDisplayName(o1), this.arrayListSize.getCurrentValue());
//                    float o2Width = font.getWidth(this.getModuleDisplayName(o2), this.arrayListSize.getCurrentValue());
//                    return Float.compare(o2Width, o1Width);
//                });
//            }
//
//            float maxWidth = this.renderModules.isEmpty()
//                    ? 0.0F
//                    : font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), this.arrayListSize.getCurrentValue());
//            float arrayListX = this.arrayListDirection.isCurrentMode("Right")
//                    ? (float) mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F + this.xOffset.getCurrentValue()
//                    : 3.0F + this.xOffset.getCurrentValue();
//            float arrayListY = this.yOffset.getCurrentValue();
//            float height = 0.0F;
//            double fontHeight = font.getHeight(true, this.arrayListSize.getCurrentValue());
//
//            for (Module module : this.renderModules) {
//                SmoothAnimationTimer animation = module.getAnimation();
//                if (module.isEnabled()) {
//                    animation.target = 100.0F;
//                } else {
//                    animation.target = 0.0F;
//                }
//
//                animation.update(true);
//                if (animation.value > 0.0F) {
//                    String displayName = this.getModuleDisplayName(module);
//                    float stringWidth = font.getWidth(displayName, this.arrayListSize.getCurrentValue());
//                    float left = -stringWidth * (1.0F - animation.value / 100.0F);
//                    float right = maxWidth - stringWidth * (animation.value / 100.0F);
//                    float innerX = this.arrayListDirection.isCurrentMode("Left") ? left : right;
//                    RenderUtils.fillBound(
//                            e.stack(),
//                            arrayListX + innerX,
//                            arrayListY + height + 2.0F,
//                            stringWidth + 3.0F,
//                            (float) ((double) (animation.value / 100.0F) * fontHeight),
//                            backgroundColor
//                    );
//                    this.blurMatrices
//                            .add(
//                                    new Vector4f(arrayListX + innerX, arrayListY + height + 2.0F, stringWidth + 3.0F, (float) ((double) (animation.value / 100.0F) * fontHeight))
//                            );
//                    int color = -1;
//                    if (this.rainbow.getCurrentValue()) {
//                        color = RenderUtils.getRainbowOpaque(
//                                (int) (-height * this.rainbowOffset.getCurrentValue()), 1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
//                        );
//                    }
//
//                    float alpha = animation.value / 100.0F;
//                    font.setAlpha(alpha);
//                    font.render(
//                            e.stack(),
//                            displayName,
//                            arrayListX + innerX + 1.5F,
//                            arrayListY + height + 1.0F,
//                            new Color(color),
//                            true,
//                            this.arrayListSize.getCurrentValue()
//                    );
//                    height += (float) ((double) (animation.value / 100.0F) * fontHeight);
//                }
//            }
//
//            font.setAlpha(1.0F);
//            e.stack().popPose();
//        }
//    }
}
