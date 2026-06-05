package net.runelite.client.plugins.microbot.craftingplus;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ButtonComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.http.api.item.ItemPrice;

import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoCraftingPlusOverlay extends OverlayPanel {
    private static final Color TITLE_COLOR = new Color(0, 170, 0);
    private static final Color HEADER_COLOR = new Color(140, 220, 140);
    private static final Color NORMAL_TEXT_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 235, 145);

    private final AutoCraftingPlusPlugin plugin;
    private final Client client;
    private final AutoCraftingPlusConfig config;

    public final ButtonComponent pauseButton;

    @Inject
    AutoCraftingPlusOverlay(AutoCraftingPlusPlugin plugin, Client client, AutoCraftingPlusConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();

        pauseButton = new ButtonComponent("Pause");
        pauseButton.setPreferredSize(new Dimension(100, 25));
        pauseButton.setParentOverlay(this);
        pauseButton.setFont(FontManager.getRunescapeBoldFont());
        pauseButton.setOnClick(() -> {
            Microbot.log("AutoCraftingPlus: pause button click received -- toggling pauseAllScripts");
            Microbot.pauseAllScripts.set(!Microbot.pauseAllScripts.get());
            if (Microbot.pauseAllScripts.get()) {
                Rs2Walker.setTarget(null);
            }
        });
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(240, 300));

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("AutoCraftingPlus v" + AutoCraftingPlusPlugin.version)
                    .color(TITLE_COLOR)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(Microbot.status == null ? "Idle" : Microbot.status)
                    .rightColor(HIGHLIGHT_COLOR)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().left("").build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Statistics")
                    .leftColor(HEADER_COLOR)
                    .build());

            AutoCraftingPlusScript script = plugin.getScript();
            if (script != null && script.getStartTimeMillis() > 0) {
                int currentLevel = client.getRealSkillLevel(Skill.CRAFTING);
                int currentXp = client.getSkillExperience(Skill.CRAFTING);
                int xpGained = currentXp - script.getStartSkillXp();
                long runtimeMillis = System.currentTimeMillis() - script.getStartTimeMillis();
                long xpPerHour = (runtimeMillis > 1000) ? (xpGained * 3600000L / runtimeMillis) : 0;

                int levelDelta = currentLevel - script.getStartSkillLevel();
                String levelStr = currentLevel + (levelDelta > 0 ? " (+" + levelDelta + ")" : "");

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Crafting level:")
                        .right(levelStr)
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("XP gained:")
                        .right(NumberFormat.getInstance().format(xpGained))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("XP/hr:")
                        .right(NumberFormat.getInstance().format(xpPerHour))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Cut cycles:")
                        .right(String.valueOf(script.getActionsCompleted()))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                // GP/hr: NET per item = product GE price minus the materials it consumes, branched
                // on the active activity. The counter is batches (one full inventory crafted per
                // increment), not items, so items = batches * a per-activity batch size estimate ->
                // the "~" marks it an estimate. Can be negative. Guards runtime 0 and price 0; any
                // item with no GE price drops that side to 0 (shows GP/hr 0 rather than mispricing).
                //   LEATHER: product - leather. 1 leather/item, ~26 per inventory (less needle+thread).
                //   GEM_CUTTING: cut gem - uncut gem. 1:1, ~27 per inventory (less chisel).
                //   JEWELLERY: product - bar (- cut gem for gem jewellery). 1 bar (+1 gem)/item;
                //     batch size mirrors the script's withdraw cap (13 with a gem, else 27).
                long gpPerHour = computeGpPerHour(runtimeMillis);
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("GP/hr:")
                        .right("~" + NumberFormat.getInstance().format(gpPerHour))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Runtime:")
                        .right(formatDuration(Duration.ofMillis(runtimeMillis)))
                        .rightColor(NORMAL_TEXT_COLOR)
                        .build());

                if (config.targetLevel() > 0) {
                    int toGo = Math.max(0, config.targetLevel() - currentLevel);
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Target:")
                            .right(config.targetLevel() + (toGo > 0 ? " (" + toGo + " to go)" : " (reached)"))
                            .rightColor(HIGHLIGHT_COLOR)
                            .build());
                }
            } else {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("(not running)")
                        .leftColor(NORMAL_TEXT_COLOR)
                        .build());
            }

            pauseButton.setText(Microbot.pauseAllScripts.get() ? "Resume" : "Pause");
            panelComponent.getChildren().add(pauseButton);

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    private String formatDuration(Duration duration) {
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    /**
     * NET GP/hr for the active activity. batches = the script's cycle counter; itemsCrafted =
     * batches * a per-activity batch-size estimate. Returns 0 (rather than mispricing) when the
     * product cannot be priced.
     */
    private long computeGpPerHour(long runtimeMillis) {
        AutoCraftingPlusScript script = plugin.getScript();
        if (script == null || runtimeMillis <= 1000) {
            return 0;
        }
        long batches = script.getActionsCompleted();
        if (batches <= 0) {
            return 0;
        }

        long netPerItem = 0;
        long itemsPerBatch = 0;

        switch (config.activity()) {
            case LEATHER: {
                // Prefer the script's resolved pick (progressive / dragonhide) over the config item.
                DragonLeather dragon = script.getActiveDragonLeather();
                if (dragon != null && dragon != DragonLeather.NONE) {
                    int productPrice = Microbot.getItemManager().getItemPrice(dragon.getItemId());
                    int leatherPrice = Microbot.getItemManager().getItemPrice(dragon.getLeatherId());
                    if (productPrice <= 0) return 0;
                    int perCraft = dragon.getLeatherPerCraft(); // body 3, chaps 2, else 1
                    netPerItem = (long) productPrice - (long) leatherPrice * perCraft;
                    itemsPerBatch = 27 / perCraft; // full inventory of dragon leather, less tools
                    break;
                }
                Leather product = script.getActiveSoftLeather() != null
                        ? script.getActiveSoftLeather() : config.leatherProduct();
                int productPrice = price(product.getProductName());
                int leatherPrice = Microbot.getItemManager().getItemPrice(product.getMaterialId());
                if (productPrice <= 0) return 0;
                netPerItem = (long) productPrice - leatherPrice; // 1 leather per item
                itemsPerBatch = 26; // full inventory of leather, less needle + thread
                break;
            }
            case GEM_CUTTING: {
                Gems gem = script.getActiveGem() != null ? script.getActiveGem() : config.gemType();
                String gemName = gem.getName();
                int cutPrice = price(gemName);
                int uncutPrice = price("Uncut " + gemName);
                if (cutPrice <= 0) return 0;
                netPerItem = (long) cutPrice - uncutPrice; // 1 uncut -> 1 cut
                itemsPerBatch = 27; // full inventory of uncut gems, less chisel
                break;
            }
            case JEWELLERY: {
                Jewelry jewelry = config.jewellery();
                boolean needsGem = jewelry.getGem() != Gem.NONE;
                int productPrice = Microbot.getItemManager().getItemPrice(jewelry.getItemID());
                int barPrice = Microbot.getItemManager().getItemPrice(jewelry.getJewelryType().getItemID());
                int gemPrice = needsGem ? Microbot.getItemManager().getItemPrice(jewelry.getGem().getCutItemID()) : 0;
                if (productPrice <= 0) return 0;
                netPerItem = (long) productPrice - barPrice - gemPrice; // 1 bar (+1 cut gem) per item
                itemsPerBatch = needsGem ? 13 : 27; // mirrors the script's per-trip withdraw cap
                break;
            }
        }

        long itemsCrafted = batches * itemsPerBatch;
        return netPerItem * itemsCrafted * 3600000L / runtimeMillis;
    }

    // Cache resolved item ids by display name so the name search runs only when a new name is seen,
    // not every render frame. search() scans the whole item database; getItemPrice by id is cheap.
    // Used for leather products and cut/uncut gems, whose enums carry only names (no item ids).
    private final Map<String, Integer> nameIdCache = new HashMap<>();

    /** GE price of the item with this exact display name, name-resolved once then cached. 0 if none. */
    private int price(String name) {
        if (name == null || name.isEmpty()) {
            return 0;
        }
        Integer id = nameIdCache.get(name);
        if (id == null) {
            id = resolveId(name);
            nameIdCache.put(name, id);
        }
        return id > 0 ? Microbot.getItemManager().getItemPrice(id) : 0;
    }

    private int resolveId(String name) {
        List<ItemPrice> matches = Microbot.getItemManager().search(name);
        if (matches == null || matches.isEmpty()) {
            return 0;
        }
        for (ItemPrice match : matches) {
            if (match.getName() != null && match.getName().equalsIgnoreCase(name)) {
                return match.getId();
            }
        }
        return 0;
    }
}
