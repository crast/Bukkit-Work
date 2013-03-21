package org.bukkit.metadata;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.TestPlugin;

public class StaticFixedSpeedTest {
    private static Plugin fooPlugin = new TestPlugin("foo");

    /**
     * @param args
     */
    public static void main(String[] args) {
        MetadataValue fixedMeta = new FixedMetadataValue(fooPlugin, new Integer(42));
        MetadataValue fixedMetaEx = new FixedMetadataValueEx(fooPlugin, new Integer(42));
        MetadataValue staticMeta = new StaticMetadataValue(fooPlugin, new Integer(42));
        int iterations = 10000;
        long elapsed = 0;
        while (elapsed < 10000) {
            elapsed = speedTest("FixedMetadataValueEx", fixedMetaEx, iterations);
            elapsed = speedTest("FixedMetadataValue", fixedMeta, iterations);
            elapsed += speedTest("StaticMetadataValue", staticMeta, iterations);
            iterations = iterations * 10;
        }
    }

    public static long speedTest(String name, MetadataValue mval, int iterations) {
        System.out.println("Testing value " + name + ": " + iterations + " iterations");
        long start_time = System.currentTimeMillis();
        while (--iterations > 0) {
            if (mval.asBoolean() != true) {
                System.out.println("Bool error");
            }
            if (mval.asInt() != 42) {
                System.out.println("Int error");
            }
            if (mval.asLong() != 42L) {
                System.out.println("Long error");
            }
        }
        long elapsed = System.currentTimeMillis() - start_time;
        System.out.println(" => Elapsed " + elapsed + " ms");
        return elapsed;
    }
}
