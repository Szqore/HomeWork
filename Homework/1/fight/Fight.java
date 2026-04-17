import java.util.Scanner;
import java.util.Random;

abstract class RpgHero {
    protected final String name;
    protected int health;
    protected final int maxHealth;
    protected int armorShield;
    protected final int maxArmor;
    protected boolean isDefending = false;
    protected Scanner scanner = new Scanner(System.in);
    protected Random rand = new Random();

    public RpgHero(String name, int health, int armor) {
        this.name = name;
        this.health = health;
        this.maxHealth = health;
        this.armorShield = armor;
        this.maxArmor = armor;
    }

    public String getName() { return name; }
    public boolean isAlive() { return health > 0; }
    public void prepareTurn() { this.isDefending = false; }

    public void setDefending(boolean state) {
        this.isDefending = state;
        if (state && this instanceof RpgElf) {
            this.armorShield = Math.min(maxArmor, armorShield + 8);
            System.out.println("ВЯЧЕСЛАВ: Эльф подпитывает Магический щит! (+8 к щиту)");
        }
    }

    public void takeDamage(int dmg, RpgHero attacker) {
        // ПЕРЕКАТ РЫЦАРЯ
        if (isDefending && this instanceof RpgHuman && rand.nextInt(100) < 30) {
            System.out.println("ВЯЧЕСЛАВ: ОПА! ПЕРЕКАТ! Рыцарь полностью уклонился от удара!");
            return;
        }

        int finalDmg = isDefending ? (this instanceof RpgOrc && ((RpgOrc)this).isOnFists() ? dmg / 3 : dmg / 2) : dmg;

        if (isDefending && this instanceof RpgHuman) {
            System.out.println("ВЯЧЕСЛАВ: Контрудар щитом! (-10 HP врагу)");
            attacker.health -= 10;
        }

        String armorName = (this instanceof RpgElf) ? "Магический щит" : "Броню";

        if (armorShield > 0) {
            if (finalDmg <= armorShield) {
                armorShield -= finalDmg;
                System.out.println("ВЯЧЕСЛАВ: Противник принял урон на " + armorName + "!");
            } else {
                int left = finalDmg - armorShield;
                armorShield = 0;
                health -= left;
                System.out.println("ВЯЧЕСЛАВ: " + armorName.toUpperCase() + " ПРОБИТ(А)! Противник принял урон телом.");
            }
        } else {
            health -= finalDmg;
            System.out.println("ВЯЧЕСЛАВ: Противник принял урон телом.");
        }
        if (health < 0) health = 0;
    }

    public abstract void playerAction(RpgHero enemy);
    public abstract void enemyAi(RpgHero player);
    public abstract String getStatus();
}

// ========== РЫЦАРЬ (DARK SOULS) ==========
class RpgHuman extends RpgHero {
    public RpgHuman(String name) { super(name, 110, 50); }
    @Override
    public void playerAction(RpgHero enemy) {
        System.out.println("ВЫБЕРИ УДАР:");
        System.out.println("1. Обычный удар (25 ед)");
        System.out.println("2. СИЛОВОЙ ВЫПАД (40 ед, РИСК!)");
        int choice = scanner.nextInt();
        if (choice == 2) {
            if (enemy.isDefending) {
                System.out.println("ВЯЧЕСЛАВ: ПАРИРОВАНИЕ! Ты ударил в блок и сам получил по шапке!");
                this.health -= 20;
            } else {
                System.out.println("ВЯЧЕСЛАВ: Ты атаковал силовым выпадом!");
                enemy.takeDamage(40, this);
            }
        } else {
            System.out.println("ВЯЧЕСЛАВ: Ты атаковал обычным ударом!");
            enemy.takeDamage(25, this);
        }
    }
    @Override
    public void enemyAi(RpgHero player) {
        System.out.println("ВЯЧЕСЛАВ: Противник атакует мечом!");
        player.takeDamage(25, this);
    }
    @Override
    public String getStatus() { return "HP: " + health + " | Броня: " + armorShield; }
}

// ========== ЭЛЬФ (ЗЕЛЬЯ) ==========
class RpgElf extends RpgHero {
    private int dmgPots = 3, healPots = 2, imbaPots = 3, armPots = 2;
    public RpgElf(String name) { super(name, 85, 25); }
    public boolean noPots() { return dmgPots<=0 && healPots<=0 && imbaPots<=0 && armPots<=0; }

    @Override
    public void playerAction(RpgHero enemy) {
        if (noPots()) {
            System.out.println("ВЯЧЕСЛАВ: Зелий нет! 1. ХАРАКИРИ | 2. Ждать смерти");
            if (scanner.nextInt() == 1) { health = 0; System.out.println("ВЯЧЕСЛАВ: ХАРАКИРИ!"); }
            return;
        }
        System.out.println("ВЫБЕРИ ЗЕЛЬЕ:");
        System.out.println("1. Зелье Урона (25 ед, ост: " + dmgPots + ")");
        System.out.println("2. Зелье ХП (25 ед, ост: " + healPots + ")");
        System.out.println("3. ИМБА-ЗЕЛЬЕ (45 ед, РИСК, ост: " + imbaPots + ")");
        System.out.println("4. Восстановление щита (25 ед, ост: " + armPots + ")");

        int c = scanner.nextInt();
        if (c==1 && dmgPots>0) { System.out.println("ВЯЧЕСЛАВ: Ты атаковал зельем урона!"); enemy.takeDamage(25, this); dmgPots--; }
        else if (c==2 && healPots>0) { health = Math.min(maxHealth, health+25); System.out.println("ВЯЧЕСЛАВ: Ты выпил хилку!"); healPots--; }
        else if (c==3 && imbaPots>0) {
            if (rand.nextInt(100)<20) { health-=35; System.out.println("ВЯЧЕСЛАВ: ТЫ АТАКОВАЛ... ОЙ! ВЗРЫВ В РУКАХ!"); }
            else { System.out.println("ВЯЧЕСЛАВ: Ты атаковал ИМБА-ЗЕЛЬЕМ!"); enemy.takeDamage(45, this); }
            imbaPots--;
        } else if (c==4 && armPots>0) { armorShield = Math.min(maxArmor, armorShield+25); System.out.println("ВЯЧЕСЛАВ: Щит заряжен!"); armPots--; }
    }
    @Override
    public void enemyAi(RpgHero player) {
        System.out.println("ВЯЧЕСЛАВ: Противник кидает зелье!");
        player.takeDamage(25, this);
    }
    @Override
    public String getStatus() { return "HP: " + health + " | Маг. Щит: " + armorShield + " | Зелья: " + (dmgPots+healPots+imbaPots+armPots); }
}

// ========== ОРК (ПУШКИ) ==========
class RpgOrc extends RpgHero {
    private int mega = 3, arrows = 4, dagger = 2;
    public RpgOrc(String name) { super(name, 150, 20); }
    public boolean isOnFists() { return mega <= 0 && arrows <= 0 && dagger <= 0; }

    @Override
    public void playerAction(RpgHero enemy) {
        System.out.println("ВЫБЕРИ ОРУЖИЕ:");
        if (mega > 0) System.out.println("1. Мега-пушка (ост: " + mega + ")");
        if (arrows > 0) System.out.println("2. Лук со стрелами (ост: " + arrows + ")");
        if (dagger > 0) System.out.println("3. Кинжал (ост: " + dagger + ")");
        if (isOnFists()) System.out.println("4. КУЛАКИ (Защита x3!)");

        int c = scanner.nextInt();
        if (c == 1 && mega > 0) {
            if (rand.nextInt(100) < 25) { System.out.println("ВЯЧЕСЛАВ: Ты атаковал... ОЙ! ПУШКА ВЗОРВАЛАСЬ!"); health -= 40; }
            else { System.out.println("ВЯЧЕСЛАВ: Ты атаковал из Мега-пушки!"); enemy.takeDamage(55, this); }
            mega--;
        } else if (c == 2 && arrows > 0) { System.out.println("ВЯЧЕСЛАВ: Ты атаковал из лука!"); enemy.takeDamage(20, this); arrows--; }
        else if (c == 3 && dagger > 0) { System.out.println("ВЯЧЕСЛАВ: Ты атаковал кинжалом!"); enemy.takeDamage(15, this); dagger--; }
        else { System.out.println("ВЯЧЕСЛАВ: Ты атакуешь кулаками!"); enemy.takeDamage(7, this); }
    }
    @Override
    public void enemyAi(RpgHero player) {
        System.out.println("ВЯЧЕСЛАВ: Противник атакует!");
        player.takeDamage(20, this);
    }
    @Override
    public String getStatus() { return "HP: " + health + " | Броня: " + armorShield + (isOnFists() ? " | [КУЛАКИ]" : ""); }
}
// ========== Main ==========
public class Fight {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in); Random r = new Random();
        System.out.println("ВЯЧЕСЛАВ: РОССИЯ 23! ВЫБЕРИ БОЙЦА:");
        System.out.println("1.Рыцарь (Dark Souls) | 2.Эльф (Зелья) | 3.Орк (Пушки)");
        int c = sc.nextInt();
        RpgHero p = (c == 1) ? new RpgHuman("Игрок") : (c == 2) ? new RpgElf("Игрок") : new RpgOrc("Игрок");
        RpgHero e; int ec; do { ec = r.nextInt(3) + 1; } while (ec == c);
        e = (ec == 1) ? new RpgHuman("Бот-Рыцарь") : (ec == 2) ? new RpgElf("Бот-Эльф") : new RpgOrc("Бот-Орк");
        System.out.println("\nВЯЧЕСЛАВ: Твой противник — " + e.getName() + "!");

        while (p.isAlive() && e.isAlive()) {
            System.out.println("\n================================");
            System.out.println("ТЫ:    " + p.getStatus());
            System.out.println("ВРАГ:  " + e.getStatus());
            System.out.println("================================");
            p.prepareTurn();
            System.out.println("ХОД ИГРОКА: 1. Действие | 2. Защита");
            if (sc.nextInt() == 1) p.playerAction(e);
            else { p.setDefending(true); System.out.println("ВЯЧЕСЛАВ: Ты выбрал защиту!"); }
            if (!e.isAlive()) break;
            System.out.println("\nВЯЧЕСЛАВ: Противник атакует...");
            e.prepareTurn();
            if (r.nextInt(10) < 2) { e.setDefending(true); System.out.println("ВЯЧЕСЛАВ: Противник выбрал защиту!"); }
            else e.enemyAi(p);
        }
        System.out.println(p.isAlive() ? "\nВЯЧЕСЛАВ: ПОЗДРАВЛЮ С ПОБЕДОЙ ! ТЕПЕРЬ ТЫ  СМОЖЕШЬ ВЕРНУТСЯ СВОЕЙ ГЕЙНАЛ КОМПАНИЮ И ОТДОХНУТЬ!" : "\nВЯЧЕСЛАВ: YOU DIED. ГГ!");
    }
}
