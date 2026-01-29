package net.melbourne.utils.font;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.SneakyThrows;
import net.melbourne.Managers;
import net.melbourne.modules.impl.client.FontFeature;
import net.melbourne.utils.Globals;
import net.melbourne.utils.graphics.api.WorldContext;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.graphics.impl.elements.TexturedQuadElement;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

import java.awt.*;
import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FontRenderer implements Closeable, Globals {
    private final Map<GpuTextureView, ObjectList<DrawEntry>> glyphPages = new HashMap<>();
    private final ObjectList<GlyphMap> maps = new ObjectArrayList<>();
    private final float size;
    private final int charsPerPage;
    private final int padding;
    private final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
    private Font[] fonts;
    private int multiplier = 0;
    private int previousGameScale = -1;
    private boolean initialized;

    public FontRenderer(Font[] fonts, float size) {
        this(fonts, size, 256, 5);
    }

    public FontRenderer(Font[] fonts, float size, int charactersPerPage, int paddingBetweenCharacters) {
        Preconditions.checkArgument(size > 0, "size <= 0");
        Preconditions.checkArgument(fonts.length > 0, "fonts.length <= 0");
        Preconditions.checkArgument(charactersPerPage > 4, "Unreasonable charactersPerPage count");
        Preconditions.checkArgument(paddingBetweenCharacters > 0, "paddingBetweenCharacters <= 0");

        this.size = size;
        this.charsPerPage = charactersPerPage;
        this.padding = paddingBetweenCharacters;

        init(fonts, size);
    }

    public static String stripControlCodes(String text)
    {
        char[] chars = text.toCharArray();
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < chars.length; i++)
        {
            char character = chars[ i ];
            if (character == '§')
            {
                i++;
                continue;
            }

            builder.append(character);
        }

        return builder.toString();
    }

    public void drawString(DrawContext context, String text, float x, float y, int color, boolean dropShadow) {
        drawText(context, Text.literal(text).styled(it -> it.withParent(Style.EMPTY.withColor(color))).asOrderedText(), x, y, color, dropShadow);
    }

    public void drawText(DrawContext context, OrderedText text, float x, float y, int color, boolean dropShadow) {
        if (mc.getWindow().getScaleFactor() != this.previousGameScale) {
            close();
            init(this.fonts, this.size);
        }

        if (Managers.FEATURE != null && Managers.FEATURE.getFeatureFromClass(FontFeature.class).isEnabled())
        {
            x += Managers.FEATURE.getFeatureFromClass(FontFeature.class).xOffset.getValue().floatValue();
            y += Managers.FEATURE.getFeatureFromClass(FontFeature.class).yOffset.getValue().floatValue() - 2;
        }

        if ((color & -67108864) == 0) color |= -16777216;
        if (dropShadow) color = (color & 16579836) >> 2 | color & -16777216;

        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        float alpha = (float) (color >> 24 & 255) / 255.0F;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(10.0f, 10.0f);

        float[] currentX = {0};

        synchronized (glyphPages) {
            text.accept((index, style, codePoint) -> {
                char[] chars = Character.toChars(codePoint);

                float currentRed = red;
                float currentGreen = green;
                float currentBlue = blue;

                if (style.getColor() != null) {
                    int rgb = style.getColor().getRgb();

                    if ((rgb & -67108864) == 0) rgb |= -16777216;
                    if (dropShadow) rgb = (rgb & 16579836) >> 2 | rgb & -16777216;

                    currentRed = (float) ((rgb >> 16) & 0xFF) / 255.0F;
                    currentGreen = (float) ((rgb >> 8) & 0xFF) / 255.0F;
                    currentBlue = (float) (rgb & 0xFF) / 255.0F;
                }

                for (char character : chars) {
                    GlyphMap.Glyph glyph = locateGlyph(character);
                    if (glyph != null) {
                        if (glyph.value() != ' ') {
                            glyphPages.computeIfAbsent(glyph.parent().getTexture().getGlTextureView(), integer -> new ObjectArrayList<>()).add(new DrawEntry(currentX[0], 0, currentRed, currentGreen, currentBlue, glyph));
                        }

                        currentX[0] += glyph.width() - 1;
                    }
                }

                return true;
            });

            for (GpuTextureView glId : glyphPages.keySet()) {
                context.getMatrices().scale(1.0f / (this.multiplier * 10), 1.0f / (this.multiplier * 10));
                RenderSystem.setShaderTexture(0, glId);

                List<DrawEntry> objects = glyphPages.get(glId);
                glId.texture().setTextureFilter(FilterMode.LINEAR, true);

                for (DrawEntry object : objects) {
                    GlyphMap.Glyph glyph = object.toDraw;
                    GlyphMap parent = glyph.parent();

                    context.state.addPreparedTextElement(new TexturedQuadElement(RenderPipelines.GUI_TEXTURED,
                            TextureSetup.of(glId, glId),
                            new Matrix3x2f(context.getMatrices()),
                            (int) object.atX(),
                            0,
                            (int) (object.atX() + glyph.width()),
                            glyph.height(),
                            (float) glyph.u() / parent.getWidth(),
                            (float) (glyph.u() + glyph.width()) / parent.getWidth(),
                            (float) glyph.v() / parent.getHeight(),
                            (float) (glyph.v() + glyph.height()) / parent.getHeight(),
                            new Color(object.r, object.g, object.b, alpha).getRGB(), context.scissorStack.peekLast()));
                }
            }

            glyphPages.clear();
        }

        context.getMatrices().popMatrix();
    }

    public void drawString(WorldContext context, String text, float x, float y, int color, boolean dropShadow) {
        drawText(context, Text.literal(text).styled(it -> it.withParent(Style.EMPTY.withColor(color))).asOrderedText(), x, y, color, dropShadow);
    }

    public void drawText(WorldContext context, OrderedText text, float x, float y, int color, boolean dropShadow) {
        if (mc.getWindow().getScaleFactor() != this.previousGameScale) {
            close();
            init(this.fonts, this.size);
        }

        if (Managers.FEATURE != null && Managers.FEATURE.getFeatureFromClass(FontFeature.class).isEnabled())
        {
            x += Managers.FEATURE.getFeatureFromClass(FontFeature.class).xOffset.getValue().floatValue();
            y += Managers.FEATURE.getFeatureFromClass(FontFeature.class).yOffset.getValue().floatValue() - 2;
        }

        if ((color & -67108864) == 0) color |= -16777216;
        if (dropShadow) color = (color & 16579836) >> 2 | color & -16777216;

        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        float alpha = (float) (color >> 24 & 255) / 255.0F;

        context.getMatrix().push();
        context.getMatrix().translate(x, y, 0);
        context.getMatrix().scale(10.0f, 10.0f, 0.0f);

        float[] currentX = {0};

        synchronized (glyphPages) {
            text.accept((index, style, codePoint) -> {
                char[] chars = Character.toChars(codePoint);

                float currentRed = red;
                float currentGreen = green;
                float currentBlue = blue;

                if (style.getColor() != null) {
                    int rgb = style.getColor().getRgb();

                    if ((rgb & -67108864) == 0) rgb |= -16777216;
                    if (dropShadow) rgb = (rgb & 16579836) >> 2 | rgb & -16777216;

                    currentRed = (float) ((rgb >> 16) & 0xFF) / 255.0F;
                    currentGreen = (float) ((rgb >> 8) & 0xFF) / 255.0F;
                    currentBlue = (float) (rgb & 0xFF) / 255.0F;
                }

                for (char character : chars) {
                    GlyphMap.Glyph glyph = locateGlyph(character);
                    if (glyph != null) {
                        if (glyph.value() != ' ') {
                            glyphPages.computeIfAbsent(glyph.parent().getTexture().getGlTextureView(), integer -> new ObjectArrayList<>()).add(new DrawEntry(currentX[0], 0, currentRed, currentGreen, currentBlue, glyph));
                        }

                        currentX[0] += glyph.width() - 1;
                    }
                }

                return true;
            });

            for (GpuTextureView glId : glyphPages.keySet()) {
                context.getMatrix().scale(1.0f / (this.multiplier * 10), 1.0f / (this.multiplier * 10), 0);
                RenderSystem.setShaderTexture(0, glId);

                List<DrawEntry> objects = glyphPages.get(glId);
                glId.texture().setTextureFilter(FilterMode.LINEAR, true);
                for (DrawEntry object : objects) {
                    GlyphMap.Glyph glyph = object.toDraw;
                    GlyphMap parent = glyph.parent();

                    Matrix4f matrices = context.matrix.peek().getPositionMatrix();
                    VertexConsumer consumer = context.buffer.getBuffer(Renderer3D.GUI_TEXTURED_LAYER.apply(glId));

                    float u1 = (float) glyph.u() / parent.getWidth();
                    float v1 = (float) glyph.v() / parent.getHeight();
                    float u2 = (float) (glyph.u() + glyph.width()) / parent.getWidth();
                    float v2 = (float) (glyph.v() + glyph.height()) / parent.getHeight();

                    consumer.vertex(matrices, object.atX + 0, object.atY + glyph.height(), 0).texture(u1, v2).color(object.r, object.g, object.b, alpha);
                    consumer.vertex(matrices, object.atX + glyph.width(), object.atY + glyph.height(), 0).texture(u2, v2).color(object.r, object.g, object.b, alpha);
                    consumer.vertex(matrices, object.atX + glyph.width(), object.atY + 0, 0).texture(u2, v1).color(object.r, object.g, object.b, alpha);
                    consumer.vertex(matrices, object.atX + 0, object.atY + 0, 0).texture(u1, v1).color(object.r, object.g, object.b, alpha);
                }
            }

            glyphPages.clear();
        }

        context.getMatrix().pop();
    }


    public float getTextWidth(String text) {
        char[] characters = stripControlCodes(text).toCharArray();
        float width = 0;

        for (char ch : characters) {
            GlyphMap.Glyph glyph = locateGlyph(ch);
            if (glyph != null) {
                width += (glyph.width() - 1) / (float) this.multiplier;
            }
        }

        return Math.max(width, 0);
    }

    public float getTextWidth(OrderedText text) {
        float[] dimensions = new float[2];
        text.accept((index, style, codePoint) -> {
            char character = (char) codePoint;

            GlyphMap.Glyph glyph = locateGlyph(character);
            if (glyph != null) {
                dimensions[0] += (glyph.width() - 1) / (float) this.multiplier;
            }

            return true;
        });

        return Math.max(dimensions[0], dimensions[1]);
    }

    public float getHeight() {
        GlyphMap.Glyph glyph = locateGlyph('A');
        if (glyph != null) return glyph.height() / (float) this.multiplier;
        return 0.0f;
    }

    private void init(Font[] fonts, float sizePx) {
        if (initialized) throw new IllegalStateException("Double call to init()");
        LOCK.writeLock().lock();

        try {
            this.previousGameScale = (int) mc.getWindow().getScaleFactor();
            this.multiplier = this.previousGameScale;
            this.fonts = new Font[fonts.length];

            for (int i = 0; i < fonts.length; i++) {
                this.fonts[i] = fonts[i].deriveFont(sizePx * this.multiplier);
            }

            initialized = true;
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private GlyphMap.Glyph locateGlyph(char glyph) {
        LOCK.readLock().lock();
        try {
            for (GlyphMap map : maps) {
                if (map.contains(glyph)) {
                    return map.getGlyph(glyph);
                }
            }
        } finally {
            LOCK.readLock().unlock();
        }

        int base = charsPerPage * (int) Math.floor((double) glyph / (double) charsPerPage);
        GlyphMap map = new GlyphMap(this.fonts, (char) base, (char) (base + charsPerPage), padding);
        LOCK.writeLock().lock();

        try {
            map.generate();
            maps.add(map);
        } finally {
            LOCK.writeLock().unlock();
        }

        return map.getGlyph(glyph);
    }

    @SneakyThrows
    @Override
    public void close() {
        LOCK.writeLock().lock();
        try {
            for (GlyphMap map : maps) map.destroy();
            maps.clear();

            initialized = false;
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public record DrawEntry(float atX, float atY, float r, float g, float b, GlyphMap.Glyph toDraw)
    {

    }
}