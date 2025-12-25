package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.PrintBattleLog;
import com.battle.heroes.army.programs.SimulateBattle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimulateBattleImpl implements SimulateBattle {

    private PrintBattleLog printBattleLog; // Позволяет логировать. Использовать после каждой атаки юнита

    @Override
    public void simulate(Army playerArmy, Army computerArmy) throws InterruptedException {
        // Получаем списки юнитов обеих армий
        List<Unit> playerUnits = playerArmy.getUnits();
        List<Unit> computerUnits = computerArmy.getUnits();

        // Бой продолжается, пока в обеих армиях есть живые юниты
        while (hasAliveUnits(playerUnits) && hasAliveUnits(computerUnits)) {

            // Формируем общий список живых юнитов для текущего раунда
            List<Unit> allUnits = new ArrayList<>();
            for (Unit unit : playerUnits) {
                if (unit.isAlive()) {
                    allUnits.add(unit);
                }
            }
            for (Unit unit : computerUnits) {
                if (unit.isAlive()) {
                    allUnits.add(unit);
                }
            }

            // Сортируем по убыванию атаки (первыми ходят самые сильные)
            allUnits.sort(Comparator.comparingInt(Unit::getBaseAttack).reversed());

            // Каждый юнит делает ход
            for (Unit unit : allUnits) {
                // Проверяем, что юнит ещё жив (мог погибнуть в этом раунде)
                if (!unit.isAlive()) {
                    continue;
                }

                // Проверяем, есть ли ещё живые противники
                boolean isPlayerUnit = playerUnits.contains(unit);
                List<Unit> enemyUnits = isPlayerUnit ? computerUnits : playerUnits;

                if (!hasAliveUnits(enemyUnits)) {
                    break; // Противников не осталось
                }

                // Юнит атакует
                Unit target = unit.getProgram().attack();

                // Логируем результат атаки
                printBattleLog.printBattleLog(unit, target);
            }
        }
    }

    /**
     * Проверяет, есть ли живые юниты в списке
     */
    private boolean hasAliveUnits(List<Unit> units) {
        for (Unit unit : units) {
            if (unit.isAlive()) {
                return true;
            }
        }
        return false;
    }
}