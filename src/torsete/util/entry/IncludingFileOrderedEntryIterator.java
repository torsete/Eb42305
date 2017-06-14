package torsete.util.entry;

import java.io.File;


class IncludingFileOrderedEntryIterator<K, V> extends IncludingOrderedEntryIterator<K, V> {
    private boolean isTabsInKeyEnabled;

    public IncludingFileOrderedEntryIterator<K, V> open() {
        setIteratorFactoryFunction((parentSource, source) -> {
            String sourceString = parentSource == null ? source.toString() : new File(parentSource.toString()).getParent() + File.separator + source;
            File file = new File(sourceString);
            return new ReaderOrderedEntryIterator<K, V>()
                    .setFile(file)
                    .enableTabsInKey(isTabsInKeyEnabled);
        });
        super.open();
        return this;

    }

    public IncludingFileOrderedEntryIterator<K, V> setSource(V source) {
        super.setSource(source);
        return this;
    }


    public IncludingFileOrderedEntryIterator<K, V> enableTabsInKey(boolean isTabsInKeyEnabled) {
        this.isTabsInKeyEnabled = isTabsInKeyEnabled;
        return this;
    }


}
