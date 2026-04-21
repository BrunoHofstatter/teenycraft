package bruhof.teenycraft.screen;

import bruhof.teenycraft.capability.TitanManagerStorageSection;

public enum TitanManagerTab {
    FIGURES("Figures", TitanManagerStorageSection.FIGURES),
    CHIPS("Chips", TitanManagerStorageSection.CHIPS),
    ACCESSORIES("Accessories", TitanManagerStorageSection.ACCESSORIES);

    private final String label;
    private final TitanManagerStorageSection storageSection;

    TitanManagerTab(String label, TitanManagerStorageSection storageSection) {
        this.label = label;
        this.storageSection = storageSection;
    }

    public String getLabel() {
        return label;
    }

    public TitanManagerStorageSection getStorageSection() {
        return storageSection;
    }
}
