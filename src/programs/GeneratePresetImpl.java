package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.GeneratePreset;

import java.util.*;

public class GeneratePresetImpl implements GeneratePreset {

    private static final int MAX_UNITS_PER_TYPE = 11;

    @Override
    public Army generate(List<Unit> unitList, int maxPoints) {
        // Ваше решение

        // Сортируем юнитов по эффективности: атака/стоимость (по убыванию)
        // При равной эффективности атаки — учитываем здоровье/стоимость
        List<Unit> sortedUnits = new ArrayList<>(unitList);
        sortedUnits.sort((u1, u2) -> {
            double efficiency1 = (double) u1.getBaseAttack() / u1.getCost();
            double efficiency2 = (double) u2.getBaseAttack() / u2.getCost();
            if (Double.compare(efficiency2, efficiency1) != 0) {
                return Double.compare(efficiency2, efficiency1);
            }
            // Вторичный критерий: здоровье/стоимость
            double healthEff1 = (double) u1.getHealth() / u1.getCost();
            double healthEff2 = (double) u2.getHealth() / u2.getCost();
            return Double.compare(healthEff2, healthEff1);
        });

        Army army = new Army();
        List<Unit> armyUnits = new ArrayList<>();

        // Счётчик юнитов каждого типа
        Map<String, Integer> unitCountByType = new HashMap<>();

        int currentPoints = 0;
        int unitIndex = 1;

        // Жадный алгоритм: добавляем юнитов пока можем
        boolean added = true;
        while (added && currentPoints < maxPoints) {
            added = false;

            for (Unit templateUnit : sortedUnits) {
                String unitType = templateUnit.getUnitType();
                int count = unitCountByType.getOrDefault(unitType, 0);

                // Проверяем ограничения: лимит юнитов типа и доступные очки
                if (count < MAX_UNITS_PER_TYPE &&
                        currentPoints + templateUnit.getCost() <= maxPoints) {

                    // Создаём нового юнита на основе шаблона
                    Unit newUnit = new Unit(
                            templateUnit.getUnitType() + " " + unitIndex++,
                            templateUnit.getUnitType(),
                            templateUnit.getHealth(),
                            templateUnit.getBaseAttack(),
                            templateUnit.getCost(),
                            templateUnit.getAttackType()
                    );

                    armyUnits.add(newUnit);
                    unitCountByType.put(unitType, count + 1);
                    currentPoints += templateUnit.getCost();
                    added = true;
                }
            }
        }

        army.setUnits(armyUnits);
        army.setPoints(currentPoints);

        return army;
    }
}