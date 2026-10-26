// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics

import net.ciyuan.lynxrelics.core.RelicRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RelicRulesTest {
    @Test fun `mixed sets use piece thresholds without including elytra or armor`() {
        assertEquals(16.2, RelicRules.pressureGain(18.0, 2), 0.0001)
        assertEquals(18.0, RelicRules.pressureGain(18.0, 1))
        assertEquals(6, RelicRules.breathCost(8, true))
        assertEquals(2, RelicRules.breathCost(2, true))
    }
    @Test fun `smithing retains damage ratio rounded against free repairs`() {
        assertEquals(500, RelicRules.damageAfterUpgrade(400, 1000, 1250))
        assertEquals(2538, RelicRules.damageAfterUpgrade(2030, 2031, 2539))
        assertEquals(0, RelicRules.damageAfterUpgrade(0, 2031, 2539))
    }
    @Test fun `dragon healing never exceeds life or remaining encounter budget`() {
        assertEquals(1f, RelicRules.heal(2999f, 0f, 2f, 450f))
        assertEquals(0f, RelicRules.heal(2500f, 450f, 2f, 450f))
        assertEquals(1f, RelicRules.heal(2500f, 449f, 2f, 450f))
        assertEquals(1, RelicRules.phase(2100.1f)); assertEquals(2, RelicRules.phase(2100f)); assertEquals(3, RelicRules.phase(900f))
    }
}
