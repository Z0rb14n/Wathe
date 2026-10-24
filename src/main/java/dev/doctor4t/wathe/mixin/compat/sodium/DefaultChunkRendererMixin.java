package dev.doctor4t.wathe.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.util.SceneryTransform;
import dev.doctor4t.wathe.compat.SodiumShaderInterface;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBufferUsage;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlMutableBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(value = DefaultChunkRenderer.class)
public abstract class DefaultChunkRendererMixin {
    @Unique
    private static ByteBuffer wathe_buffer = MemoryUtil.memAlloc(RenderRegion.REGION_SIZE * 16);
    @Unique
    private static GlMutableBuffer glBuffer;

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;getCachedBatch(Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;"), remap = false)
    private MultiDrawBatch wathe$refreshMovingDraws(RenderRegion region, TerrainRenderPass pass,
                                                   Operation<MultiDrawBatch> original) {
        if (WatheClient.isTrainMoving()) {
            // Visible sections change as templates move, even with a stationary camera.
            region.clearCachedBatchFor(pass);
        }
        return original.call(region, pass);
    }

    @ModifyExpressionValue(method = "fillCommandBuffer", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataUnsafe;getSliceMask(J)I"), remap = false)
    private static int wathe$cullTranslatedSections(int original,
                                                   @Local(argsOnly = true) RenderRegion region,
                                                   @Local(argsOnly = true) CameraTransform camera,
                                                   @Local(name = "sectionIndex") int sectionIndex) {
        if (WatheClient.isTrainMoving()) {
            var client = MinecraftClient.getInstance();
            var position = wathe$sceneryPosition(region, camera, sectionIndex);
            if (!SceneryTransform.isWithinViewDistance(position,
                    camera.intX + (double) camera.fracX, camera.intY + (double) camera.fracY,
                    camera.intZ + (double) camera.fracZ, client.options.getViewDistance().getValue())) {
                return 0;
            }
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "render(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderListIterable;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Z)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/caffeinemc/mods/sodium/client/gui/SodiumOptions$PerformanceSettings;useBlockFaceCulling:Z"
            ),
            remap = false
    )
    private boolean wathe$disable_culling(boolean original) {
        if (WatheClient.isTrainMoving()) {
            return false;
        }
        return original;
    }

    @Inject(method = "render(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderListIterable;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Z)V", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;executeDrawBatch(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/gl/tessellation/GlTessellation;Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;)V"),
            remap = false)
    private void modifyChunkRenderBefore(ChunkRenderMatrices matrices,
                                         CommandList commandList,
                                         ChunkRenderListIterable renderLists,
                                         TerrainRenderPass renderPass,
                                         CameraTransform camera,
                                         boolean useTranslucentSorting,
                                         CallbackInfo ci,
                                         @Local(ordinal = 0) ChunkShaderInterface shader,
                                         @Local(ordinal = 0) RenderRegion region,
                                         @Local(ordinal = 0) ChunkRenderList renderList) {
        if (wathe_buffer == null) {
            wathe_buffer = MemoryUtil.memAlloc(RenderRegion.REGION_SIZE * 16);
        }

        // Sodium caches draw commands across frames. Offsets depend on the current
        // region, camera and animation time, so refresh them even for cached batches.
        MemoryUtil.memSet(wathe_buffer, 0);
        if (WatheClient.isTrainMoving()) {
            var sections = renderList.sectionsWithGeometryIterator(renderPass.isTranslucent());
            if (sections != null) {
                while (sections.hasNext()) {
                    wathe$offsetScenery(region, camera, sections.nextByteAsInt());
                }
            }
        }

        glBuffer = commandList.createMutableBuffer();
        commandList.uploadData(glBuffer, wathe_buffer, GlBufferUsage.STREAM_DRAW);

        ((SodiumShaderInterface) shader).wathe$set(glBuffer);
    }

    @Inject(method = "render(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderListIterable;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Z)V", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;executeDrawBatch(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;Lnet/caffeinemc/mods/sodium/client/gl/tessellation/GlTessellation;Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;)V",
            shift = At.Shift.AFTER),
            remap = false)
    private void modifyChunkRenderAfter(ChunkRenderMatrices matrices,
                                        CommandList commandList,
                                        ChunkRenderListIterable renderLists,
                                        TerrainRenderPass renderPass,
                                        CameraTransform camera,
                                         boolean useTranslucentSorting,
                                         CallbackInfo ci) {
        commandList.deleteBuffer(glBuffer);
        glBuffer = null;
    }

    @Unique
    private static void wathe$offsetScenery(RenderRegion region, CameraTransform camera, int sectionIndex) {
        var position = wathe$sceneryPosition(region, camera, sectionIndex);
        wathe_buffer.putFloat(sectionIndex * 16, position.x() - (region.getOriginX() + LocalSectionIndex.unpackX(sectionIndex) * 16));
        wathe_buffer.putFloat(sectionIndex * 16 + 4, position.y() - (region.getOriginY() + LocalSectionIndex.unpackY(sectionIndex) * 16));
        wathe_buffer.putFloat(sectionIndex * 16 + 8, position.z() - (region.getOriginZ() + LocalSectionIndex.unpackZ(sectionIndex) * 16));
    }

    @Unique
    private static SceneryTransform.Position wathe$sceneryPosition(RenderRegion region, CameraTransform camera, int sectionIndex) {
        float time = WatheClient.trainComponent.getTime()
                + MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
        return SceneryTransform.transform(
                region.getOriginX() + LocalSectionIndex.unpackX(sectionIndex) * 16,
                region.getOriginY() + LocalSectionIndex.unpackY(sectionIndex) * 16,
                region.getOriginZ() + LocalSectionIndex.unpackZ(sectionIndex) * 16,
                camera.fracX, ChunkSectionPos.getSectionCoord(camera.intZ), time, WatheClient.getTrainSpeed());
    }
}
