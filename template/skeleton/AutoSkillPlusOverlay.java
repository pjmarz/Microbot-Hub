// TEMPLATE FILE — see TEMPLATE.md. Replace "Skill" / "skillplus" tokens.

package net.runelite.client.plugins.microbot.skillplus;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class AutoSkillPlusOverlay extends OverlayPanel {

    @Inject
    AutoSkillPlusOverlay(AutoSkillPlusPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("AutoSkillPlus V" + AutoSkillPlusPlugin.version)
                    .color(Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // Microbot.status is set every tick by the script's updateStatus() method. When
            // a wrong-target-vs-location combo is detected (see Pattern 5 in the script), the
            // status string starts with "WRONG TARGET:" — this overlay shows that verbatim so
            // the user sees what's broken without checking logs.
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(Microbot.status)
                    .build());

            // TODO(plus): add domain-specific overlay lines if useful — XP/hr, items per hour,
            // current bank trip ETA, etc. Keep the overlay compact; status string is the main
            // signal.

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
