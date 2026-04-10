package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.audio.OpenAudioMcService;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SoundOptionsScreen.class)
public class NraSoundOptionsScreenMixin {

  /**
   * Redirects the first addBig() call in addOptions() (the Master Volume entry). We call the
   * original, then append the OpenAudioMC volume slider immediately after — but only when
   * connected. Using @Redirect avoids needing @Shadow on the inherited `list` field.
   */
  @Redirect(
      method = "addOptions",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/components/OptionsList;addBig(Lnet/minecraft/client/OptionInstance;)V",
              ordinal = 0))
  private void nra$redirectMasterVolume(OptionsList optionsList, OptionInstance<?> option) {
    optionsList.addBig(option);

    OpenAudioMcService service = OpenAudioMcService.getInstance();
    if (!service.isConnected()) {
      return;
    }
    int currentVol = service.getCurrentVolume();
    double initialValue = currentVol >= 0 ? currentVol / 100.0 : 1.0;

    OptionInstance<Double> volumeOption =
        new OptionInstance<>(
            "options.nra.openaudiomc_volume",
            OptionInstance.noTooltip(),
            (caption, value) ->
                Component.translatable("options.percent_value", caption, (int) (value * 100)),
            OptionInstance.UnitDouble.INSTANCE,
            initialValue,
            newValue -> service.setVolumeFromSlider((int) Math.round(newValue * 100)));

    optionsList.addBig(volumeOption);
  }
}
