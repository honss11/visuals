package dev.likevisuals;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class R3 {
    public static Matrix4f m;

    public static void begin(MatrixStack ms, Vec3d cam, boolean depth) {
        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        m = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    public static void end(MatrixStack ms) {
        ms.pop();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static final class Batch {
        private final BufferBuilder b;
        private Batch(VertexFormat.DrawMode mode) { b = Tessellator.getInstance().begin(mode, VertexFormats.POSITION_COLOR); }
        public void v(double x, double y, double z, int argb) { b.vertex(m, (float) x, (float) y, (float) z).color(argb); }
        public void v(Vec3d p, int argb) { v(p.x, p.y, p.z, argb); }
        public void tri(Vec3d a, Vec3d c, Vec3d d, int col) { v(a, col); v(c, col); v(d, col); }
        public void quad(Vec3d a, Vec3d c, Vec3d d, Vec3d e, int col) { v(a, col); v(c, col); v(d, col); v(e, col); }
        public void quad(Vec3d a, Vec3d c, Vec3d d, Vec3d e, int c1, int c2) { v(a, c1); v(c, c1); v(d, c2); v(e, c2); }
        public void line(Vec3d a, Vec3d c, int col) { v(a, col); v(c, col); }
        public void draw() {
            BuiltBuffer bb = b.endNullable();
            if (bb != null) BufferRenderer.drawWithGlobalProgram(bb);
        }
    }

    public static Batch tris() { return new Batch(VertexFormat.DrawMode.TRIANGLES); }
    public static Batch quads() { return new Batch(VertexFormat.DrawMode.QUADS); }
    public static Batch lines(float width) { RenderSystem.lineWidth(width); return new Batch(VertexFormat.DrawMode.DEBUG_LINES); }

    public static void boxLines(Batch b, Box x, int c) {
        Vec3d[] p = new Vec3d[8];
        for (int i = 0; i < 8; i++) p[i] = new Vec3d((i & 1) == 0 ? x.minX : x.maxX, (i & 2) == 0 ? x.minY : x.maxY, (i & 4) == 0 ? x.minZ : x.maxZ);
        int[][] e = {{0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},{0,4},{1,5},{2,6},{3,7}};
        for (int[] k : e) b.line(p[k[0]], p[k[1]], c);
    }

    public static void boxFill(Batch b, Box x, int c) {
        Vec3d a = new Vec3d(x.minX, x.minY, x.minZ), bb = new Vec3d(x.maxX, x.minY, x.minZ), cc = new Vec3d(x.maxX, x.minY, x.maxZ), d = new Vec3d(x.minX, x.minY, x.maxZ);
        Vec3d e = new Vec3d(x.minX, x.maxY, x.minZ), f = new Vec3d(x.maxX, x.maxY, x.minZ), g = new Vec3d(x.maxX, x.maxY, x.maxZ), h = new Vec3d(x.minX, x.maxY, x.maxZ);
        b.quad(a, bb, cc, d, c); b.quad(e, f, g, h, c);
        b.quad(a, bb, f, e, c); b.quad(d, cc, g, h, c);
        b.quad(a, d, h, e, c); b.quad(bb, cc, g, f, c);
    }

    /** Flat ring as quads (inner r1, outer r2) on a horizontal plane at y. */
    public static void ringFill(Batch q, Vec3d c, double r1, double r2, int seg, int col1, int col2) {
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            Vec3d p0 = c.add(Math.cos(a0) * r1, 0, Math.sin(a0) * r1), p1 = c.add(Math.cos(a1) * r1, 0, Math.sin(a1) * r1);
            Vec3d p2 = c.add(Math.cos(a1) * r2, 0, Math.sin(a1) * r2), p3 = c.add(Math.cos(a0) * r2, 0, Math.sin(a0) * r2);
            q.quad(p0, p1, p2, p3, col1, col2);
        }
    }

    public static void discFill(Batch t, Vec3d c, double r, int seg, int col) {
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            t.tri(c, c.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), c.add(Math.cos(a1) * r, 0, Math.sin(a1) * r), col);
        }
    }

    public static void ringLines(Batch l, Vec3d c, double r, int seg, int col) {
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            l.line(c.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), c.add(Math.cos(a1) * r, 0, Math.sin(a1) * r), col);
        }
    }

    /** Cone with apex above base centre. */
    public static void cone(Batch t, Vec3d base, double r, double h, int seg, int cBase, int cTop) {
        Vec3d apex = base.add(0, h, 0);
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            t.v(apex, cTop); t.v(base.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), cBase); t.v(base.add(Math.cos(a1) * r, 0, Math.sin(a1) * r), cBase);
        }
    }

    public static Vec3d lerpPos(net.minecraft.entity.Entity e, float td) {
        return new Vec3d(net.minecraft.util.math.MathHelper.lerp(td, e.prevX, e.getX()), net.minecraft.util.math.MathHelper.lerp(td, e.prevY, e.getY()), net.minecraft.util.math.MathHelper.lerp(td, e.prevZ, e.getZ()));
    }
}
