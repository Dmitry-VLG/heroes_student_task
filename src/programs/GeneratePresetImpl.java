package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.GeneratePreset;

import java.lang.reflect.Method;
import java.util.*;

public class GeneratePresetImpl implements GeneratePreset {

    private static final int MAX_UNITS_PER_TYPE = 11;

    // Размер поля из ТЗ: WIDTH = 27, HEIGHT = 21 (важно для координат)
    private static final int FIELD_HEIGHT = 21;

    // Сторона компьютера: 3 колонки.
    // По описанию: юниты компьютера (цели игрока) находятся на x = 0..2  :contentReference[oaicite:3]{index=3}
    private static final int COMPUTER_X_OFFSET = 0; // если вдруг окажется наоборот — поменяй на 24
    private static final int SIDE_WIDTH = 3;

    @Override
    public Army generate(List<Unit> unitList, int maxPoints) {
        // Сортируем типы по эффективности:
        // 1) baseAttack/cost по убыванию
        // 2) health/cost по убыванию
        List<Unit> sortedTemplates = new ArrayList<>(unitList);
        sortedTemplates.sort((u1, u2) -> {
            double eff1 = ratio(u1.getBaseAttack(), u1.getCost());
            double eff2 = ratio(u2.getBaseAttack(), u2.getCost());
            int cmp = Double.compare(eff2, eff1);
            if (cmp != 0) return cmp;

            double hEff1 = ratio(u1.getHealth(), u1.getCost());
            double hEff2 = ratio(u2.getHealth(), u2.getCost());
            return Double.compare(hEff2, hEff1);
        });

        Army army = new Army();
        List<Unit> armyUnits = new ArrayList<>();

        Map<String, Integer> unitCountByType = new HashMap<>();
        int currentPoints = 0;
        int unitIndex = 1;

        // Индекс размещения на поле, чтобы не было одинаковых координат
        int placementIndex = 0;

        boolean added = true;
        while (added && currentPoints < maxPoints) {
            added = false;

            for (Unit template : sortedTemplates) {
                String unitType = template.getUnitType();
                int count = unitCountByType.getOrDefault(unitType, 0);

                int cost = template.getCost();
                if (count >= MAX_UNITS_PER_TYPE) continue;
                if (currentPoints + cost > maxPoints) continue;

                // Проверка, что есть место на стороне компьютера (3*21 = 63 клетки)
                int y = placementIndex / SIDE_WIDTH;
                if (y >= FIELD_HEIGHT) {
                    // места на стороне кончилось — прекращаем генерацию
                    added = false;
                    break;
                }
                int x = COMPUTER_X_OFFSET + (placementIndex % SIDE_WIDTH);
                placementIndex++;

                Map<String, Double> attackBonuses =
                        copyMapIfPossible(template, "getAttackBonuses");
                Map<String, Double> defenceBonuses =
                        copyMapIfPossible(template, "getDefenceBonuses");

                Unit newUnit = new Unit(
                        unitType + " " + unitIndex++,
                        unitType,
                        template.getHealth(),
                        template.getBaseAttack(),
                        cost,
                        String.valueOf(template.getAttackType()),
                        attackBonuses,
                        defenceBonuses,
                        x,
                        y
                );

                armyUnits.add(newUnit);
                unitCountByType.put(unitType, count + 1);
                currentPoints += cost;
                added = true;
            }
        }

        army.setUnits(armyUnits);
        army.setPoints(currentPoints);

        // Временно для диагностики (можешь потом убрать):
        // System.out.println("[GeneratePreset] units=" + armyUnits.size() + ", points=" + currentPoints);

        return army;
    }

    private static double ratio(int a, int b) {
        if (b <= 0) return Double.NEGATIVE_INFINITY;
        return (double) a / b;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Double> copyMapIfPossible(Unit unit, String getterName) {
        try {
            Method m = unit.getClass().getMethod(getterName);
            Object v = m.invoke(unit);
            if (v instanceof Map<?, ?> map) {
                return new HashMap<>((Map<String, Double>) map);
            }
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }
}
