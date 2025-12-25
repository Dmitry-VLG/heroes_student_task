package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.SuitableForAttackUnitsFinder;

import java.util.ArrayList;
import java.util.List;

public class SuitableForAttackUnitsFinderImpl implements SuitableForAttackUnitsFinder {

    @Override
    public List<Unit> getSuitableUnits(List<List<Unit>> unitsByRow, boolean isLeftArmyTarget) {
        // Ваше решение
        List<Unit> suitableUnits = new ArrayList<>();

        // Проходим по каждому ряду юнитов
        for (List<Unit> row : unitsByRow) {
            if (row == null || row.isEmpty()) {
                continue;
            }

            // Находим крайнего юнита в ряду в зависимости от направления атаки
            Unit suitableUnit = null;

            for (Unit unit : row) {
                if (unit == null || !unit.isAlive()) {
                    continue;
                }

                if (suitableUnit == null) {
                    suitableUnit = unit;
                } else {
                    if (isLeftArmyTarget) {
                        // Атакуем левую армию — ищем юнита с минимальной координатой Y (не закрыт слева)
                        if (unit.getxCoordinate() < suitableUnit.getxCoordinate()) {
                            suitableUnit = unit;
                        }
                    } else {
                        // Атакуем правую армию — ищем юнита с максимальной координатой Y (не закрыт справа)
                        if (unit.getxCoordinate() > suitableUnit.getxCoordinate()) {
                            suitableUnit = unit;
                        }
                    }
                }
            }

            // Добавляем найденного подходящего юнита в результат
            if (suitableUnit != null) {
                suitableUnits.add(suitableUnit);
            }
        }

        return suitableUnits;
    }
}