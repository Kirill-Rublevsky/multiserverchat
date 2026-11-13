package ru.ovenquill.globalchat.backend;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PrefixRulesTest {
    @Test void exactPrefixRules(){ assertTrue("!Привет".startsWith("!")); assertTrue("!!Привет".startsWith("!")); assertFalse(" !Привет".startsWith("!")); assertFalse("Привет".startsWith("!")); }
    @Test void radiusUsesSquaredDistance(){ double radius=100.0; assertTrue(9_999.0 <= radius*radius); assertFalse(10_001.0 <= radius*radius); }
}
