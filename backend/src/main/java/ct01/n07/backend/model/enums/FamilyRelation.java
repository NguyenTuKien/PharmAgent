package ct01.n07.backend.model.enums;

public enum FamilyRelation {
    FATHER("Bố"),
    MOTHER("Mẹ"),
    PATERNAL_GRANDFATHER("Ông Nội"),
    PATERNAL_GRANDMOTHER("Bà Nội"),
    MATERNAL_GRANDFATHER("Ông Ngoại"),
    MATERNAL_GRANDMOTHER("Bà Ngoại"),
    OTHER("Khác");

    private final String label;

    FamilyRelation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
