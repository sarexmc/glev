package com.supernova.client;

import com.supernova.menu.ShipMenu;
import com.supernova.network.TakeoffPayload;
import com.supernova.registry.ModDimensions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/** Panel de control de la nave: combustible, destino, tripulacion y boton de encendido/regreso. */
public class ShipScreen extends AbstractContainerScreen<ShipMenu> {
    private NeonButton launch;
    private float anim;

    public ShipScreen(ShipMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    private boolean onGleba() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().equals(ModDimensions.GLEBA);
    }

    @Override
    protected void init() {
        super.init();
        String label = onGleba() ? "REGRESAR A LA TIERRA" : "ENCENDER MOTORES · VIAJAR A GLEBA";
        launch = addRenderableWidget(new NeonButton(leftPos + 8, topPos + 92, 160, 18, label, () -> {
            ClientPlayNetworking.send(new TakeoffPayload());
            onClose();
        }));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        launch.active = menu.fuel() >= 50;
        anim++;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        g.fill(x, y, x + w, y + h, 0xF20A0A14);
        g.fillGradient(x, y, x + w, y + 22, 0xFF18182C, 0xFF0A0A14);
        g.fill(x, y + 22, x + w, y + 23, 0xFF7FE8FF);
        g.fill(x, y, x + w, y + 1, 0xFFFF4F7A);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF7FE8FF);
        g.fill(x, y, x + 1, y + h, 0xFF7FE8FF);
        g.fill(x + w - 1, y, x + w, y + h, 0xFFFF4F7A);
        for (Slot s : menu.slots) {
            boolean fuelSlot = s.index == 0;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, fuelSlot ? 0xFF7FE8FF : 0xFF2A2A3C);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, fuelSlot ? 0xFF10101C : 0xFF181826);
        }
        // medidor de combustible (10 segmentos con brillo)
        int fuel = menu.fuel();
        for (int i = 0; i < 10; i++) {
            boolean on = fuel >= (10 - i) * 10 - 4;
            int sy = y + 29 + i * 6;
            g.fill(x + 57, sy, x + 71, sy + 5, on ? 0xFFFF4F7A : 0x40FF4F7A);
            if (on) g.fill(x + 57, sy, x + 71, sy + 1, 0x80FFFFFF);
        }
        g.fill(x + 55, y + 29 + 30, x + 73, y + 29 + 31, 0xFF7FE8FF);   // marca del 50 %
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        ShipHud.text(g, "NAVE · PANEL DE VUELO", 8, 7, ShipHud.CYAN, 0.85f, 0);
        ShipHud.text(g, onGleba() ? "DESTINO: TIERRA" : "DESTINO: GLEBA", 168, 8, ShipHud.PINK, 0.65f, 2);
        ShipHud.text(g, "TANQUE DE DILITIO", 8, 28, ShipHud.CYAN_DIM, 0.55f, 0);
        ShipHud.text(g, "COMBUSTIBLE", 84, 28, ShipHud.CYAN_DIM, 0.6f, 0);
        ShipHud.text(g, menu.fuel() + "%", 84, 38, menu.fuel() >= 50 ? ShipHud.WHITE : ShipHud.PINK, 1.6f, 0);
        ShipHud.text(g, "MINIMO PARA DESPEGAR  50%", 84, 56, ShipHud.CYAN_DIM, 0.55f, 0);
        int crew = 1;
        if (mc.level != null && mc.player != null)
            crew = Math.min(4, mc.level.getEntitiesOfClass(Player.class, mc.player.getBoundingBox().inflate(6)).size());
        ShipHud.text(g, "TRIPULACION  " + crew + "/4", 84, 66, ShipHud.CYAN_DIM, 0.55f, 0);
        boolean ok = menu.fuel() >= 50;
        ShipHud.text(g, ok ? "SISTEMAS LISTOS · MANTEN CTRL" : "INSERTA TANQUES DE DILITIO", 84, 77, ok ? 0xFF7CFF9A : ShipHud.PINK, 0.5f, 0);
    }

    /** Boton con estilo neon (pulsa cuando esta listo). */
    private class NeonButton extends AbstractButton {
        private final Runnable action;

        NeonButton(int x, int y, int w, int h, String label, Runnable action) {
            super(x, y, w, h, Component.literal(label));
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int x0 = getX(), y0 = getY(), x1 = x0 + getWidth(), y1 = y0 + getHeight();
            boolean hov = isHoveredOrFocused() && active;
            float pulse = active ? 0.55f + 0.45f * (float) Math.sin((anim + pt) * 0.18f) : 0f;
            int border = !active ? 0xFF3A3A4A : hov ? 0xFFFFFFFF : Gfx2D.argb(0.65f + 0.35f * pulse, 80, 232, 255);
            int fillTop = !active ? 0xFF16161F : hov ? 0xFF2A5A70 : 0xFF143246;
            g.fillGradient(x0, y0, x1, y1, fillTop, !active ? 0xFF0E0E16 : 0xFF0A1A28);
            g.fill(x0, y0, x1, y0 + 1, border);
            g.fill(x0, y1 - 1, x1, y1, border);
            g.fill(x0, y0, x0 + 1, y1, border);
            g.fill(x1 - 1, y0, x1, y1, border);
            if (active) Gfx2D.fill(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, 255, 79, 122, 0.10f * pulse);
            ShipHud.text(g, getMessage().getString(), (x0 + x1) / 2f, y0 + 5, active ? ShipHud.WHITE : 0xFF77778A, 0.7f, 1);
        }
    }
}
