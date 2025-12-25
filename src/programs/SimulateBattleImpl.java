package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.PrintBattleLog;
import com.battle.heroes.army.programs.SimulateBattle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimulateBattleImpl implements SimulateBattle {

    private PrintBattleLog printBattleLog;

    public void setPrintBattleLog(PrintBattleLog printBattleLog) {
        this.printBattleLog = printBattleLog;
    }

    @Override
    public void simulate(Army playerArmy, Army computerArmy) throws InterruptedException {
        List<Unit> playerUnits = playerArmy.getUnits();
        List<Unit> computerUnits = computerArmy.getUnits();

        int round = 1;

        while (hasAliveUnits(playerUnits) && hasAliveUnits(computerUnits)) {

            List<Unit> allUnits = new ArrayList<>();
            addAlive(allUnits, playerUnits);
            addAlive(allUnits, computerUnits);

            allUnits.sort(Comparator.comparingInt(Unit::getBaseAttack).reversed());

            for (Unit unit : allUnits) {
                if (!unit.isAlive()) continue;
                if (unit.getProgram() == null) continue;

                Unit target = unit.getProgram().attack();

                if (target == null) {
                    System.out.println("Unit can not find target for attack!");
                } else if (printBattleLog != null) {
                    printBattleLog.printBattleLog(unit, target);
                }
            }

            System.out.println("\nRound " + round + " is over!");
            System.out.println("Player army has " + countAlive(playerUnits) + " units");
            System.out.println("Computer army has " + countAlive(computerUnits) + " units\n");

            round++;
        }
    }

    private static void addAlive(List<Unit> into, List<Unit> from) {
        for (Unit u : from) {
            if (u != null && u.isAlive()) into.add(u);
        }
    }

    private static boolean hasAliveUnits(List<Unit> units) {
        for (Unit u : units) {
            if (u != null && u.isAlive()) return true;
        }
        return false;
    }

    private static int countAlive(List<Unit> units) {
        int c = 0;
        for (Unit u : units) {
            if (u != null && u.isAlive()) c++;
        }
        return c;
    }
}
