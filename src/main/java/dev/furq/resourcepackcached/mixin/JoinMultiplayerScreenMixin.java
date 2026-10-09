package dev.furq.resourcepackcached.mixin;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {

    @Unique
    private static final long rpc$confirmTimeoutMs = 5000L;

    @Unique
    private Button rpc$resetButton;

    @Unique
    private boolean rpc$placed;

    @Unique
    private long rpc$confirmUntil = 0L;

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private void rpc$placeButton() {
        if (this.rpc$resetButton == null) return;
        int x = this.width - 130;
        int y = 10;
        boolean moved;
        int guard = 0;
        do {
            moved = false;
            for (GuiEventListener child : this.children()) {
                if (child == this.rpc$resetButton || !(child instanceof Button widget)) continue;
                if (x < widget.getX() + widget.getWidth() && x + 120 > widget.getX()
                        && y < widget.getY() + widget.getHeight() && y + 20 > widget.getY()) {
                    y = widget.getY() + widget.getHeight() + 4;
                    moved = true;
                }
            }
            guard++;
        } while (moved && guard < 10 && y + 20 <= this.height - 4);
        this.rpc$resetButton.setX(x);
        this.rpc$resetButton.setY(Math.min(y, Math.max(10, this.height - 24)));
    }

    @Unique
    private void rpc$disarmConfirm() {
        this.rpc$confirmUntil = 0L;
        if (this.rpc$resetButton != null) {
            this.rpc$resetButton.setMessage(Component.literal("Reset Cached Packs"));
        }
    }

    @Unique
    private void rpc$onResetPressed() {
        if (this.rpc$confirmUntil >= System.currentTimeMillis()) {
            CachingUtils.resetCachedPacks();
            this.rpc$disarmConfirm();
        } else {
            this.rpc$confirmUntil = System.currentTimeMillis() + rpc$confirmTimeoutMs;
            if (this.rpc$resetButton != null) {
                this.rpc$resetButton.setMessage(Component.literal("Confirm reset?"));
            }
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        rpc$resetButton = Button.builder(Component.literal("Reset Cached Packs"), button -> rpc$onResetPressed())
                .tooltip(Tooltip.create(Component.literal("Click twice to delete cached server packs and unload them")))
                .bounds(this.width - 130, 10, 120, 20)
                .build();
        this.addRenderableWidget(rpc$resetButton);
        this.rpc$placeButton();
        this.rpc$confirmUntil = 0L;
        this.rpc$placed = false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!this.rpc$placed) {
            this.rpc$placed = true;
            this.rpc$placeButton();
        }
        if (this.rpc$confirmUntil != 0L && this.rpc$confirmUntil < System.currentTimeMillis()) {
            this.rpc$disarmConfirm();
        }
    }

    @Inject(method = "repositionElements", at = @At("TAIL"), require = 0)
    private void onRepositionElements(CallbackInfo ci) {
        this.rpc$placeButton();
    }
}
