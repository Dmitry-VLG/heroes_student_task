package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.SuitableForAttackUnitsFinder;

import java.util.ArrayList;
import java.util.List;

public class SuitableForAttackUnitsFinderImpl implements SuitableForAttackUnitsFinder {

    @Override
    public List<Unit> getSuitableUnits(List<List<Unit>> unitsByRow, boolean isLeftArmyTarget) {
        List<Unit> suitableUnits = new ArrayList<>();
        if (unitsByRow == null) return suitableUnits;

        for (List<Unit> row : unitsByRow) {
            if (row == null || row.isEmpty()) continue;

            Unit best = null;

            for (Unit unit : row) {
                if (unit == null || !unit.isAlive()) continue;

                if (best == null) {
                    best = unit;
                    continue;
                }

                int x = unit.getxCoordinate();
                int bestX = best.getxCoordinate();

                // Если атакуем ЛЕВУЮ армию (компьютера на x=0..2), атакующий справа,
                // значит доступен самый "правый" из ряда -> max X.
                // Если атакуем ПРАВУЮ армию (игрока на x=24..26), атакующий слева,
                // значит доступен самый "левый" -> min X.
                if (isLeftArmyTarget) {
                    if (x > bestX) best = unit;
                } else {
                    if (x < bestX) best = unit;
                }
            }

            if (best != null) suitableUnits.add(best);
        }

        return suitableUnits;
    }
}
