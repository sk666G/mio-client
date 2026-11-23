/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

import net.minecraft.client.KeyMapping;

public class SoftwareKeyMapping extends KeyMapping {
    private boolean down;

    public SoftwareKeyMapping() {
        super("schizo-wrapper-keybind", 0, Category.MISC);

        ALL.remove("schizo-wrapper-keybind");
    }

    @Override
    public boolean isDown() {
        return down;
    }

    @Override
    public void setDown(boolean down) {
        this.down = down;
    }

}
