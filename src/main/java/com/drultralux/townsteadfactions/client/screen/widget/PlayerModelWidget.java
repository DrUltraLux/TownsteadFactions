package com.drultralux.townsteadfactions.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A draggable widget that renders a static preview of the local player's
 * entity model, facing the camera and locked in place. Modeled directly
 * on MCA's own {@code VillagerEditorScreen} preview code: rather than
 * relying only on a rotation quaternion, the player entity's own body,
 * head, and look rotation fields are temporarily forced to face forward,
 * then restored afterward so nothing else in the game is affected.
 */
public class PlayerModelWidget extends DraggableWidget {

    /**
     * Creates the player model widget at the given position with a fixed
     * default size.
     *
     * @param x the x position of the widget
     * @param y the y position of the widget
     */
    public PlayerModelWidget(int x, int y) {
        super(x, y, 46, 62);
    }

    /**
     * Renders the widget's background and, unless minimized, a static
     * preview of the local player's model facing directly toward the
     * camera.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position, unused (the model no longer tracks the cursor)
     * @param mouseY the current mouse y position, unused (the model no longer tracks the cursor)
     * @param partialTicks the partial tick time, for frame interpolation
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x66000000);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF444444);

        if (this.isMinimized) return;

        int centerX = this.x + (this.width / 2);
        int centerY = this.y + this.height - 6;
        float renderScaleSize = 24.0F;

        // Save the player's real rotation state, so we can restore it after rendering
        // the preview — otherwise we'd visibly snap the real in-world player model too.
        float previousBodyRot = mc.player.yBodyRot;
        float previousBodyRotO = mc.player.yBodyRotO;
        float previousYRot = mc.player.getYRot();
        float previousYRotO = mc.player.yRotO;
        float previousXRot = mc.player.getXRot();
        float previousXRotO = mc.player.xRotO;
        float previousHeadRot = mc.player.yHeadRot;
        float previousHeadRotO = mc.player.yHeadRotO;

        // Force the entity's own pose to face forward, exactly as MCA's editor preview does.
        mc.player.yBodyRot = 180.0F;
        mc.player.yBodyRotO = 180.0F;
        mc.player.setYRot(180.0F);
        mc.player.yRotO = 180.0F;
        mc.player.setXRot(0.0F);
        mc.player.xRotO = 0.0F;
        mc.player.yHeadRot = 180.0F;
        mc.player.yHeadRotO = 180.0F;

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);

        InventoryScreen.renderEntityInInventory(
                graphics,
                (float) centerX,
                (float) centerY,
                renderScaleSize,
                new Vector3f(0.0F, 0.0F, 0.0F),
                pose,
                null,
                mc.player
        );

        // Restore the player's real rotation state.
        mc.player.yBodyRot = previousBodyRot;
        mc.player.yBodyRotO = previousBodyRotO;
        mc.player.setYRot(previousYRot);
        mc.player.yRotO = previousYRotO;
        mc.player.setXRot(previousXRot);
        mc.player.xRotO = previousXRotO;
        mc.player.yHeadRot = previousHeadRot;
        mc.player.yHeadRotO = previousHeadRotO;
    }
}