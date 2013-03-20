package org.bukkit.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.TestPlugin;
import org.junit.Test;

public class MetadataStoreTest {
    private Plugin pluginX = new TestPlugin("x");
    private Plugin pluginY = new TestPlugin("y");

    StringMetadataStore subject = new StringMetadataStore();

    @Test
    public void testMetadataStore() {
        subject.setMetadata("subject", "key", new FixedMetadataValue(pluginX, 10));

        assertTrue(subject.hasMetadata("subject", "key"));
        List<MetadataValue> values = subject.getMetadata("subject", "key");
        assertEquals(10, values.get(0).value());
    }

    @Test
    public void testMetadataNotPresent() {
        assertFalse(subject.hasMetadata("subject", "key"));
        List<MetadataValue> values = subject.getMetadata("subject", "key");
        assertTrue(values.isEmpty());
    }

    @Test
    public void testInvalidateAll() {
        final Counter counter = new Counter();

        subject.setMetadata("subject", "key", new LazyMetadataValue(pluginX, new Callable<Object>() {
            public Object call() throws Exception {
                counter.increment();
                return 10;
            }
        }));

        assertTrue(subject.hasMetadata("subject", "key"));
        subject.getMetadata("subject", "key").get(0).value();
        subject.invalidateAll(pluginX);
        subject.getMetadata("subject", "key").get(0).value();
        assertEquals(2, counter.value());
    }

    @Test
    public void testInvalidateAllButActuallyNothing() {
        final Counter counter = new Counter();

        subject.setMetadata("subject", "key", new LazyMetadataValue(pluginX, new Callable<Object>() {
            public Object call() throws Exception {
                counter.increment();
                return 10;
            }
        }));

        assertTrue(subject.hasMetadata("subject", "key"));
        subject.getMetadata("subject", "key").get(0).value();
        subject.invalidateAll(pluginY);
        subject.getMetadata("subject", "key").get(0).value();
        assertEquals(1, counter.value());
    }

    @Test
    public void testMetadataReplace() {
        subject.setMetadata("subject", "key", new FixedMetadataValue(pluginX, 10));
        subject.setMetadata("subject", "key", new FixedMetadataValue(pluginY, 10));
        subject.setMetadata("subject", "key", new FixedMetadataValue(pluginX, 20));

        for (MetadataValue mv : subject.getMetadata("subject", "key")) {
            if (mv.getOwningPlugin().equals(pluginX)) {
                assertEquals(20, mv.value());
            }
            if (mv.getOwningPlugin().equals(pluginY)) {
                assertEquals(10, mv.value());
            }
        }
    }

    @Test
    public void testMetadataRemove() {
        subject.setMetadata("subject", "key", new FixedMetadataValue(pluginX, 10));
        subject.setMetadata("subject", "key", new FixedMetadataValue(pluginY, 20));
        subject.removeMetadata("subject", "key", pluginX);

        assertTrue(subject.hasMetadata("subject", "key"));
        assertEquals(1, subject.getMetadata("subject", "key").size());
        assertEquals(20, subject.getMetadata("subject", "key").get(0).value());
    }

    @Test
    public void testMetadataRemoveLast() {
        subject.setMetadata("subject", "key", new FixedMetadataValue(pluginX, 10));
        subject.removeMetadata("subject", "key", pluginX);

        assertFalse(subject.hasMetadata("subject", "key"));
        assertEquals(0, subject.getMetadata("subject", "key").size());
    }

    @Test
    public void testMetadataRemoveForNonExistingPlugin() {
        subject.setMetadata("subject", "key", new FixedMetadataValue(pluginX, 10));
        subject.removeMetadata("subject", "key", pluginY);

        assertTrue(subject.hasMetadata("subject", "key"));
        assertEquals(1, subject.getMetadata("subject", "key").size());
        assertEquals(10, subject.getMetadata("subject", "key").get(0).value());
    }

    @Test
    public void testHasMetadata() {
        subject.setMetadata("subject", "key", new FixedMetadataValue(pluginX, 10));
        assertTrue(subject.hasMetadata("subject", "key"));
        assertFalse(subject.hasMetadata("subject", "otherKey"));
    }

    @Test
    public void testRegisterUnregisterProvider() {
        StringMetadataProvider p = new StringMetadataProvider();
        // Test adding provider once
        assertEquals(true, subject.registerProvider("uppercased", p));
        // Test adding provider again
        assertEquals(false, subject.registerProvider("uppercased", p));
        // Now removal
        assertEquals(true, subject.unregisterProvider("uppercased"));
        assertEquals(false, subject.unregisterProvider("uppercased"));
    }

    @Test
    public void testProviderUsage() {
        assertEquals(subject.getMetadata("foobar", "uppercased").size(), 0);
        StringMetadataProvider p = new StringMetadataProvider();
        subject.registerProvider("uppercased", p);
        assertEquals(0, p.counter.value());
        List<MetadataValue> values = subject.getMetadata("foobar", "uppercased");
        assertEquals(1, values.size());
        assertEquals("FOOBAR", values.get(0).asString());
        assertEquals(1, p.counter.value());
        // Check we still get a single value only
        values = subject.getMetadata("foobar", "uppercased");
        assertEquals(1, values.size());
        assertEquals("FOOBAR", values.get(0).asString());
        // Check the provider wasn't called twice.
        assertEquals(1, p.counter.value());
        // Try a new previously un-seen value through the provider.
        assertEquals(1, subject.getMetadata("hello", "uppercased").size());
        assertEquals(2, p.counter.value());
    }

    private class StringMetadataStore extends MetadataStoreBase<String> implements MetadataStore<String> {
        @Override
        protected String disambiguate(String subject, String metadataKey) {
            return subject + ":" + metadataKey;
        }
    }

    private class StringMetadataProvider implements MetadataProvider<String> {
        public final Counter counter = new Counter();

        public Plugin getOwningPlugin() {
            return pluginX;
        }

        public MetadataValue getValue(String subject, String key) {
            counter.increment();
            if (subject.equals("nodata")) {
                return null;
            } else {
                return new FixedMetadataValue(pluginX, subject.toUpperCase());
            }
        }

    }

    private class Counter {
        int c = 0;

        public void increment() {
            c++;
        }

        public int value() {
            return c;
        }
    }
}
