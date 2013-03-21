package org.bukkit.metadata;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.Callable;

/**
 * A test to see if we can get the same improvements over FixedMetadataValue without adding a new class
 */
public class FixedMetadataValueEx extends LazyMetadataValue {
    private static final BogusCallable bogusCallable = new BogusCallable();
    private final Object internalValue;
    /**
     * Initializes a FixedMetadataValue with an Object
     *
     * @param owningPlugin the {@link Plugin} that created this metadata value.
     * @param value the value assigned to this metadata value.
     */
    public FixedMetadataValueEx(Plugin owningPlugin, final Object value) {
        super(owningPlugin);
        this.internalValue = value;
    }

    @Override
    public void invalidate() {

    }

    @Override
    public Object value() {
        return internalValue;
    }
}
class BogusCallable implements Callable<Object> {
        public Object call() throws Exception {
            return null;
        }
}
