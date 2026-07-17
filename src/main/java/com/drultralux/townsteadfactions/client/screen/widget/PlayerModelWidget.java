package com.drultralux.townsteadfactions.client.screen.widget;

import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PlayerModelWidget extends DraggableWidget {

    public PlayerModelWidget(int x, int y) {
        // Reduced height boundaries to align with the text column anchors perfectly
        super(x, y, 46, 62);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x66000000);
        graphics.renderOutline(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF444444);

        if (this.isMinimized) return;

        int centerX = this.x + (this.width / 2);
        int centerY = this.y + this.height - 6;
        float renderScaleSize = 24.0F;

        float lookX = (float)(centerX) - mouseX;
        float lookY = (float)(centerY - 35) - mouseY;
        float yawAngle = (float)Math.atan(lookX / 40.0F);
        float pitchAngle = (float)Math.atan(lookY / 40.0F);

        Quaternionf bodyRotation = new Quaternionf().rotationXYZ(0.0F, yawAngle, (float) Math.PI);
        Quaternionf headRotation = new Quaternionf().rotationXYZ(pitchAngle, yawAngle, (float) Math.PI);

        InventoryScreen.renderEntityInInventory(
                graphics,
                (float) centerX,
                (float) centerY,
                renderScaleSize,
                new Vector3f(0.0F, 0.0F, 0.0F),
                bodyRotation,
                headRotation,
                mc.player
        );
    }
}