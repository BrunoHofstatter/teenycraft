package bruhof.teenycraft.group;

public record GroupComboStatBonus(int health, int power, int dodge, int luck) {
    public static final GroupComboStatBonus NONE = new GroupComboStatBonus(0, 0, 0, 0);

    public GroupComboStatBonus plus(GroupComboStatBonus other) {
        if (other == null) {
            return this;
        }
        return new GroupComboStatBonus(
                health + other.health,
                power + other.power,
                dodge + other.dodge,
                luck + other.luck
        );
    }
}
