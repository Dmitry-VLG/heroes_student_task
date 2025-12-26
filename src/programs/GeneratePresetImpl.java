package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.GeneratePreset;
import com.battle.heroes.army.programs.Program;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class GeneratePresetImpl implements GeneratePreset {

    private static final int MAX_UNITS_PER_TYPE = 11;

    private static final int HEIGHT = 21;
    private static final int SIDE_WIDTH = 3;
    private static final int COMPUTER_X_OFFSET = 0; // враг слева: x=0..2

    private static final boolean DEBUG = false;

    @Override
    public Army generate(List<Unit> unitList, int maxPoints) {
        List<Unit> templates = (unitList == null) ? Collections.emptyList() : new ArrayList<>(unitList);

        // 1) baseAttack / cost (desc)
        // 2) health / cost (desc)
        // Доп. стабилизаторы (чтобы поведение было детерминированным при равенстве):
        // 3) cost (asc)
        // 4) unitType (asc)
        templates.sort((a, b) -> {
            int cmp = Double.compare(attackEff(b), attackEff(a));
            if (cmp != 0) return cmp;

            cmp = Double.compare(healthEff(b), healthEff(a));
            if (cmp != 0) return cmp;

            cmp = Integer.compare(a.getCost(), b.getCost());
            if (cmp != 0) return cmp;

            return String.valueOf(a.getUnitType()).compareTo(String.valueOf(b.getUnitType()));
        });

        // Клетки на стороне врага (3*21=63) — перемешиваем только размещение
        List<int[]> cells = new ArrayList<>(SIDE_WIDTH * HEIGHT);
        for (int y = 0; y < HEIGHT; y++) {
            for (int dx = 0; dx < SIDE_WIDTH; dx++) {
                cells.add(new int[]{COMPUTER_X_OFFSET + dx, y});
            }
        }
        Collections.shuffle(cells, new Random(System.nanoTime()));
        int cellIndex = 0;

        Map<String, Integer> countByType = new HashMap<>();
        List<Unit> result = new ArrayList<>();
        int points = 0;
        int unitIndex = 1;

        boolean added;
        do {
            added = false;

            for (Unit t : templates) {
                if (t == null) continue;

                String type = t.getUnitType();
                int count = countByType.getOrDefault(type, 0);
                if (count >= MAX_UNITS_PER_TYPE) continue;

                int cost = t.getCost();
                if (points + cost > maxPoints) continue;

                if (cellIndex >= cells.size()) {
                    // места больше нет
                    break;
                }

                int[] cell = cells.get(cellIndex++);
                int x = cell[0];
                int y = cell[1];

                Map<String, Double> attackBonuses = readMapFieldOrEmpty(t, "attackBonuses");
                Map<String, Double> defenceBonuses = readMapFieldOrEmpty(t, "defenceBonuses");

                Unit newUnit = new Unit(
                        type + " " + unitIndex++,
                        type,
                        t.getHealth(),
                        t.getBaseAttack(),
                        cost,
                        String.valueOf(t.getAttackType()),
                        attackBonuses,
                        defenceBonuses,
                        x,
                        y
                );

                Program newProgram = cloneProgramForUnit(t.getProgram(), newUnit);
                newUnit.setProgram(newProgram);

                result.add(newUnit);
                countByType.put(type, count + 1);
                points += cost;
                added = true;

                if (DEBUG) {
                    System.out.println("Added " + result.size() + " unit");
                }

                if (cellIndex >= cells.size()) break;
            }
        } while (added && points < maxPoints && cellIndex < cells.size());

        if (DEBUG) {
            System.out.println("Used points: " + points);
        }

        Army army = new Army();
        army.setUnits(result);
        army.setPoints(points);
        return army;
    }

    private static double attackEff(Unit u) {
        int cost = u.getCost();
        return cost <= 0 ? Double.NEGATIVE_INFINITY : (double) u.getBaseAttack() / cost;
    }

    private static double healthEff(Unit u) {
        int cost = u.getCost();
        return cost <= 0 ? Double.NEGATIVE_INFINITY : (double) u.getHealth() / cost;
    }

    /**
     * Создаёт новый Program того же класса, копирует в него поля (кроме unit),
     * затем привязывает unit к newUnit.
     * <p>
     * Если создать/скопировать невозможно — возвращает исходный program (fallback).
     */
    private static Program cloneProgramForUnit(Program templateProgram, Unit newUnit) {
        if (templateProgram == null) return null;

        try {
            Class<?> clazz = templateProgram.getClass();

            // 1) Создать новый экземпляр Program через no-arg конструктор
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object newProgObj = ctor.newInstance();

            // 2) Скопировать все поля кроме "unit"
            copyAllFields(templateProgram, newProgObj, "unit");

            // 3) Установить unit = newUnit (если такое поле есть)
            setFieldIfExists(newProgObj, "unit", newUnit);

            return (Program) newProgObj;
        } catch (Exception ignored) {
            // fallback: если невозможно создать копию — возвращаем исходный program
            return templateProgram;
        }
    }

    private static void copyAllFields(Object src, Object dst, String... excludedNames) throws IllegalAccessException {
        Set<String> excluded = new HashSet<>(Arrays.asList(excludedNames));

        Class<?> c = src.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (excluded.contains(f.getName())) continue;
                f.setAccessible(true);
                Object v = f.get(src);
                f.set(dst, v);
            }
            c = c.getSuperclass();
        }
    }

    private static void setFieldIfExists(Object obj, String fieldName, Object value) {
        Class<?> c = obj.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (Exception ignored) {
                c = c.getSuperclass();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Double> readMapFieldOrEmpty(Unit u, String fieldName) {
        try {
            Field f = u.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(u);
            if (v instanceof Map<?, ?> map) {
                return new HashMap<>((Map<String, Double>) map);
            }
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }
}
