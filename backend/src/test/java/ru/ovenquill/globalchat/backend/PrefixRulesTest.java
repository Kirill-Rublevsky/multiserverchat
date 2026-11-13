package ru.ovenquill.globalchat.backend;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PrefixRulesTest {
    @Test void exactPrefixRules(){ assertTrue("!Привет".startsWith("!")); assertTrue("!!Привет".startsWith("!")); assertFalse(" !Привет".startsWith("!")); assertFalse("Привет".startsWith("!")); }
}
