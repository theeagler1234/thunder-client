package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Panorama;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TitleScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.literal("Fig Client");
   private static final Component COPYRIGHT_TEXT = Component.translatable("title.credits");
   private static final String DEMO_LEVEL_ID = "Demo_World";
   private boolean fading;
   private long fadeInStart;
   private final LogoRenderer logoRenderer;

   public TitleScreen() {
      this(false);
   }

   public TitleScreen(final boolean fading) {
      this(fading, null);
   }

   public TitleScreen(final boolean fading, final @Nullable LogoRenderer logoRenderer) {
      super(TITLE);
      this.fading = fading;
      this.logoRenderer = Objects.requireNonNullElseGet(logoRenderer, () -> new LogoRenderer(false));
      this.minecraft.gameRenderer.panorama().startSpin();
   }

   public static void registerTextures(final TextureManager textureManager) {
      textureManager.registerForNextReload(LogoRenderer.MINECRAFT_LOGO);
      textureManager.registerForNextReload(LogoRenderer.MINECRAFT_EDITION);
      textureManager.registerForNextReload(Panorama.PANORAMA_OVERLAY);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected void init() {
      int copyrightWidth = this.font.width(COPYRIGHT_TEXT);
      int copyrightX = this.width - copyrightWidth - 2;
      int spacing = 24;
      int menuButtonX = Math.max(24, Math.min(64, this.width / 16));
      int topPos = this.height / 2 - 42;
      if (this.minecraft.isDemo()) {
         topPos = this.createDemoMenuOptions(topPos, 24, menuButtonX);
      } else {
         topPos = this.createNormalMenuOptions(topPos, 24, menuButtonX);
      }

      int numberOfButtons = this.minecraft.isDemo() ? 2 : 3;
      int currentButton = 0;
      int utilityRowWidth = numberOfButtons * 20 + (numberOfButtons - 1) * 4;
      int utilityRowLeft = this.width - utilityRowWidth - 16;
      if (!this.minecraft.isDemo()) {
         SpriteIconButton profile = this.addRenderableWidget(
            CommonButtons.profile(
               20,
               var1x -> this.minecraft.gui.setScreen(new net.lax1dude.eaglercraft.v1_8.profile.EaglerProfileScreen26(this)),
               true
            )
         );
         profile.setPosition(this.getHorizontalPosition(++currentButton, numberOfButtons, 20, utilityRowLeft, utilityRowWidth), 18);
      }
      SpriteIconButton language = this.addRenderableWidget(
         CommonButtons.language(
            20, var1x -> this.minecraft.gui.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), true
         )
      );
      language.setPosition(this.getHorizontalPosition(++currentButton, numberOfButtons, 20, utilityRowLeft, utilityRowWidth), 18);
      SpriteIconButton accessibility = this.addRenderableWidget(
         CommonButtons.accessibility(20, var1x -> this.minecraft.gui.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), true)
      );
      accessibility.setPosition(this.getHorizontalPosition(++currentButton, numberOfButtons, 20, utilityRowLeft, utilityRowWidth), 18);
      topPos += 34;
      this.addRenderableWidget(
         new PlainTextButton(menuButtonX, topPos, 220, 24, Component.literal("Fig Mods"), var1x -> this.minecraft.gui.setScreen(
            new net.minecraft.client.gui.screens.options.FigModulesScreen(this, this.minecraft.options)
         ), this.font)
      );
      topPos += 26;
      this.addRenderableWidget(
         new PlainTextButton(menuButtonX, topPos, 220, 24, Component.literal("Relays"), var1x -> this.minecraft.gui.setScreen(
            new net.lax1dude.eaglercraft.v1_8.sp.gui.RelaySettingsScreen(this)
         ), this.font)
      );
      topPos += 26;
      boolean eaglerHosted = net.lax1dude.eaglercraft.v1_8.minecraft.EaglerHosted.isActive();
      this.addRenderableWidget(
         new PlainTextButton(menuButtonX, topPos, 220, 24,
            eaglerHosted ? Component.translatableWithFallback("menu.credits", "Credits") : Component.translatable("menu.quit"), var1x -> {
            if (eaglerHosted) {
               this.minecraft.gui.setScreen(new EaglerCreditsScreen(this));
            } else {
               this.minecraft.stop();
            }
         }, this.font)
      );
      this.addRenderableWidget(
         new PlainTextButton(
            copyrightX,
            this.height - 10,
            copyrightWidth,
            10,
            COPYRIGHT_TEXT,
            var1x -> this.minecraft.gui.setScreen(new CreditsAndAttributionScreen(this)),
            this.font
         )
      );
      // Keep the title screen cold. A stopped integrated-server worker owns a
      // private Wasm-GC heap, and starting fresh mesh workers here immediately
      // after Save and Quit defeats deterministic world teardown. The selected
      // play mode starts only the workers it actually needs below.
      // Eagler headless autotest: __eaglerAutoTest='sp' (set by a CDP probe before boot)
      // auto-creates a world to validate the integrated-server path without UI input.
      // Use a NORMAL world (openFresh), not the FLAT testWorld — a flat world skips
      // structure/feature generation and so does NOT exercise the gen steps that a real
      // player's normal world hits (e.g. the gap #14 chunk-gen crash).
      String startupServer = this.minecraft.gui.screen() == this ? pendingStartupServer() : null;
      if (startupServer != null) {
         this.minecraft.execute(() -> {
            if (this.minecraft.gui.screen() == this && consumeStartupServer(startupServer)) {
               QuickPlay.connectStartupServer(this.minecraft, this, startupServer);
            }
         });
      } else if (!autoTestFired && net.lax1dude.eaglercraft.v1_8.internal.PlatformBootSignal.autoTestSp()) {
	         autoTestFired = true;
	         net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(this.minecraft, () -> {});
	      }
   }

   // Eagler headless autotest latch (init() re-runs on resize)
   private static boolean autoTestFired = false;
   private static boolean startupServerLoaded = false;
   private static boolean startupServerConsumed = false;
   private static @Nullable String startupServerPending = null;

   private static @Nullable String pendingStartupServer() {
      if (startupServerConsumed) {
         return null;
      }
      if (!startupServerLoaded) {
         startupServerLoaded = true;
         String value = net.lax1dude.eaglercraft.v1_8.EagRuntime.getConfiguration().getServerToJoin();
         startupServerPending = value == null || value.isBlank() ? null : value.trim();
      }
      return startupServerPending;
   }

   private static boolean consumeStartupServer(final String expected) {
      if (startupServerConsumed || startupServerPending == null || !startupServerPending.equals(expected)) {
         return false;
      }
      startupServerConsumed = true;
      startupServerPending = null;
      return true;
   }

   private int getHorizontalPosition(final int currentButton, final int numberOfButtons, final int buttonWidth,
                                     final int parentLeft, final int parentWidth) {
      int totalWidth = numberOfButtons * buttonWidth + (numberOfButtons - 1) * 4;
      return parentLeft + (parentWidth - totalWidth) / 2 + (currentButton - 1) * (buttonWidth + 4);
   }

   private int createNormalMenuOptions(int topPos, final int spacing, final int menuButtonX) {
      PlainTextButton singleplayerButton = this.addRenderableWidget(
         new PlainTextButton(menuButtonX, topPos, 220, 24, Component.translatable("menu.singleplayer"), var1 -> {
	            if (net.lax1dude.eaglercraft.v1_8.minecraft.EaglerHosted.isActive()) {
	               net.lax1dude.eaglercraft.v1_8.mesh.MeshWorkerRuntime.prewarm();
	               net.lax1dude.eaglercraft.v1_8.sp.SingleplayerServerController26.prewarmServerWorker();
	            }
	            if (net.lax1dude.eaglercraft.v1_8.minecraft.EaglerHosted.isActive()
	                  && net.lax1dude.eaglercraft.v1_8.sp.internal.ClientPlatformSingleplayer.isServerWorkerModeRequested()
	                  && net.lax1dude.eaglercraft.v1_8.sp.SingleplayerServerController26.getState()
	                     != net.lax1dude.eaglercraft.v1_8.sp.SingleplayerServerController26.STATE_WORKER_IDLE) {
	               this.minecraft.gui.setScreen(
	                  new net.lax1dude.eaglercraft.v1_8.sp.gui.EaglerIntegratedServerStartupScreen(this)
	               );
	            } else {
	               this.minecraft.gui.setScreen(new SelectWorldScreen(this));
	            }
            }, this.font)
      );
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         this.addRenderableWidget(
            new PlainTextButton(singleplayerButton.getX() + singleplayerButton.getWidth() + 2, topPos, 20, 20,
               Component.literal("TW"), var1 -> CreateWorldScreen.testWorld(this.minecraft, () -> this.minecraft.gui.setScreen(this)), this.font)
         );
      }

      Component multiplayerDisabledReason = this.getMultiplayerDisabledReason();
      boolean multiplayerAllowed = multiplayerDisabledReason == null;
      Tooltip tooltip = multiplayerDisabledReason != null ? Tooltip.create(multiplayerDisabledReason) : null;
      int var7;
      PlainTextButton multiplayerButton = this.addRenderableWidget(new PlainTextButton(menuButtonX, var7 = topPos + spacing, 220, 24,
         Component.translatable("menu.multiplayer"), button -> {
         if (net.lax1dude.eaglercraft.v1_8.minecraft.EaglerHosted.isActive()) {
            net.lax1dude.eaglercraft.v1_8.sp.SingleplayerServerController26.prepareForRemoteMultiplayer();
         }
         Screen screen = this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this);
         this.minecraft.gui.setScreen(screen);
      }, this.font));
      multiplayerButton.setTooltip(tooltip);
      multiplayerButton.active = multiplayerAllowed;
      return var7;
   }

   private @Nullable Component getMultiplayerDisabledReason() {
      if (this.minecraft.allowsMultiplayer()) {
         return null;
      } else if (this.minecraft.isNameBanned()) {
         return Component.translatable("title.multiplayer.disabled.banned.name");
      } else {
         BanDetails multiplayerBan = this.minecraft.multiplayerBan();
         if (multiplayerBan != null) {
            return multiplayerBan.expires() != null
               ? Component.translatable("title.multiplayer.disabled.banned.temporary")
               : Component.translatable("title.multiplayer.disabled.banned.permanent");
         } else {
            return Component.translatable("title.multiplayer.disabled");
         }
      }
   }

   private int createDemoMenuOptions(int topPos, final int spacing, final int menuButtonX) {
      boolean demoWorldPresent = this.checkDemoWorldPresence();
      this.addRenderableWidget(
         new PlainTextButton(menuButtonX + 10, topPos, 200, 20,
            Component.translatable("menu.playdemo"),
               button -> {
                  if (demoWorldPresent) {
                     this.minecraft.createWorldOpenFlows().openWorld("Demo_World", () -> this.minecraft.gui.setScreen(this));
                  } else {
                     this.minecraft
                        .createWorldOpenFlows()
                        .createFreshLevel(
                           "Demo_World", MinecraftServer.DEMO_SETTINGS, WorldOptions.DEMO_OPTIONS, WorldPresets::createNormalWorldDimensions, this
                        );
                  }
               }, this.font)
      );
      int var5;
      PlainTextButton resetDemoButton = this.addRenderableWidget(
         new PlainTextButton(menuButtonX + 10, var5 = topPos + spacing, 200, 20,
            Component.translatable("menu.resetdemo"),
            button -> {
                  LevelStorageSource levelSource = this.minecraft.getLevelSource();

                  try (LevelStorageSource.LevelStorageAccess levelAccess = levelSource.createAccess("Demo_World")) {
                     if (levelAccess.hasWorldData()) {
                        this.minecraft
                           .gui
                           .setScreen(
                              new ConfirmScreen(
                                 this::confirmDemo,
                                 Component.translatable("selectWorld.deleteQuestion"),
                                 Component.translatable("selectWorld.deleteWarning", MinecraftServer.DEMO_SETTINGS.levelName()),
                                 Component.translatable("selectWorld.deleteButton"),
                                 CommonComponents.GUI_CANCEL
                              )
                           );
                     }
                  } catch (IOException e) {
                     SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
                     LOGGER.warn("Failed to access demo world", e);
                  }
            }, this.font)
      );
      resetDemoButton.active = demoWorldPresent;
      return var5;
   }

   private boolean checkDemoWorldPresence() {
      try (LevelStorageSource.LevelStorageAccess levelSource = this.minecraft.getLevelSource().createAccess("Demo_World")) {
         return levelSource.hasWorldData();
      } catch (IOException e) {
         SystemToast.onWorldAccessFailure(this.minecraft, "Demo_World");
         LOGGER.warn("Failed to read demo world data", e);
         return false;
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      if (this.fadeInStart == 0L && this.fading) {
         this.fadeInStart = Util.getMillis();
      }

      float widgetFade = 1.0F;
      if (this.fading) {
         float fade = (float)(Util.getMillis() - this.fadeInStart) / 2000.0F;
         if (fade > 1.0F) {
            this.fading = false;
         } else {
            fade = Mth.clamp(fade, 0.0F, 1.0F);
            widgetFade = Mth.clampedMap(fade, 0.5F, 1.0F, 0.0F, 1.0F);
         }

         this.fadeWidgets(widgetFade);
      }

      this.minecraft.gameRenderer.panorama().extractRenderState(graphics, this.width, this.height);
      graphics.fillGradient(0, this.height / 3, this.width, this.height, 0x00000000, 0xB0183442);
      int brandX = Math.max(24, Math.min(64, this.width / 16));
      int brandY = Math.max(30, this.height / 4 - 30);
      graphics.pose().pushMatrix();
      graphics.pose().translate(brandX, brandY);
      graphics.pose().scale(2.6F, 2.6F);
      graphics.text(this.font, Component.literal("FIG CLIENT").withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD),
         0, 0, ARGB.white(widgetFade));
      graphics.pose().popMatrix();
      super.extractRenderState(graphics, mouseX, mouseY, a);
      String versionString = "Minecraft " + SharedConstants.getCurrentVersion().name();
      if (this.minecraft.isDemo()) {
         versionString = versionString + " Demo";
      }

      if (Minecraft.checkModStatus().shouldReportAsModified()) {
         versionString = versionString + I18n.get("menu.modded");
      }

      if (net.lax1dude.eaglercraft.v1_8.minecraft.EaglerHosted.isActive()) {
         // Minimal client branding: keep the Eagler credit line, but make the
         // running client visibly identify itself as Fig Client on the title screen.
         graphics.text(this.font, Component.literal("Fig Client"), 2, this.height - 30, ARGB.white(widgetFade));
         graphics.text(this.font, Component.translatableWithFallback("eagler.menu.brand", "Eaglercraft %s",
                  net.lax1dude.eaglercraft.v1_8.EaglercraftVersion.projectForkVersion),
            2, this.height - 20, ARGB.white(widgetFade));
         graphics.text(this.font, Component.translatableWithFallback("eagler.menu.rewrittenBy", "Rewritten by %s",
                  net.lax1dude.eaglercraft.v1_8.EaglercraftVersion.projectForkVendor),
            2, this.height - 10, ARGB.white(widgetFade));
      } else {
         graphics.text(this.font, Component.literal("Fig Client"), 2, this.height - 20, ARGB.white(widgetFade));
         graphics.text(this.font, versionString, 2, this.height - 10, ARGB.white(widgetFade));
      }
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
   }

   @Override
   protected void extractPanorama(final GuiGraphicsExtractor graphics, final float a) {
      super.extractPanorama(graphics, a);
      graphics.fill(0, 0, this.width, this.height, 0x302B8FC0);
   }

   private void confirmDemo(final boolean result) {
      if (result) {
         try (LevelStorageSource.LevelStorageAccess levelSource = this.minecraft.getLevelSource().createAccess("Demo_World")) {
            levelSource.deleteLevel();
         } catch (IOException e) {
            SystemToast.onWorldDeleteFailure(this.minecraft, "Demo_World");
            LOGGER.warn("Failed to delete demo world", e);
         }
      }

      this.minecraft.gui.setScreen(this);
   }

   @Override
   public boolean canInterruptWithAnotherScreen() {
      return true;
   }
}
