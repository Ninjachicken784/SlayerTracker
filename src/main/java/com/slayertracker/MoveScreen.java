package com.slayertracker;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag the text box where you want it. */
public class MoveScreen extends Screen {
	private boolean dragging;
	private int grabX;
	private int grabY;

	public MoveScreen() {
		super(Component.literal("Move Slayer GUI"));
	}

	@Override
	protected void init() {
		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
				.pos(this.width / 2 - 50, this.height - 28).size(100, 20).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, 0x66000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		this.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, "Drag the text to move it", this.width / 2, 10, 0xFFFFFFFF);
		Hud.draw(graphics, this.minecraft, Hud.x(this.minecraft, this.width), Hud.y(this.minecraft, this.height));
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int x = Hud.x(this.minecraft, this.width);
		int y = Hud.y(this.minecraft, this.height);
		if (event.button() == 0 && event.x() >= x && event.x() <= x + Hud.width(this.minecraft)
				&& event.y() >= y && event.y() <= y + Hud.height(this.minecraft)) {
			this.dragging = true;
			this.grabX = (int) event.x() - x;
			this.grabY = (int) event.y() - y;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.dragging) {
			Config.get().hudX = Math.max(0, (int) event.x() - this.grabX);
			Config.get().hudY = Math.max(0, (int) event.y() - this.grabY);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.dragging) {
			this.dragging = false;
			Config.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		Config.save();
		super.onClose();
	}
}
