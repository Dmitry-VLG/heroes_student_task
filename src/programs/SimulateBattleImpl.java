package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.PrintBattleLog;
import com.battle.heroes.army.programs.SimulateBattle;

import java.util.*;
import java.util.stream.Collectors;

public class SimulateBattleImpl implements SimulateBattle {

    private PrintBattleLog printBattleLog;

    // Оставляем сеттер: игра/тесты могут инжектить логгер через него
    public void setPrintBattleLog(PrintBattleLog printBattleLog) {
        this.printBattleLog = printBattleLog;
    }

    private static final Comparator<Unit> BY_ATTACK_DESC =
            Comparator.comparingInt(Unit::getBaseAttack).reversed();

    @Override
    public void simulate(Army playerArmy, Army computerArmy) throws InterruptedException {
        List<Unit> playerUnits = safeList(playerArmy);
        List<Unit> computerUnits = safeList(computerArmy);

        int round = 1;

        // Завершаем, когда у одной из армий нет живых юнитов, способных сделать ход
        while (hasAliveAndCanMove(playerUnits) && hasAliveAndCanMove(computerUnits)) {

            // кто уже сходил в этом раунде (чтобы “восстановление очередей” не возвращало их обратно)
            Set<Unit> actedThisRound = Collections.newSetFromMap(new IdentityHashMap<>());

            Deque<Unit> playerQueue = buildQueue(playerUnits, actedThisRound);
            Deque<Unit> computerQueue = buildQueue(computerUnits, actedThisRound);

            // Первый ход — у самого сильного (по атаке) среди двух армий
            boolean playerTurn = chooseFirstTurn(playerQueue, computerQueue);

            boolean anyMoveThisRound = false;

            // Раунд идёт, пока хотя бы у одной стороны остались юниты, которые ещё не ходили в этом раунде
            while (!playerQueue.isEmpty() || !computerQueue.isEmpty()) {

                // если бой уже фактически закончился — выходим
                if (!hasAliveAndCanMove(playerUnits) || !hasAliveAndCanMove(computerUnits)) {
                    return;
                }

                Unit attacker = pollNextAttacker(playerTurn, playerQueue, computerQueue);
                if (attacker == null) {
                    // выбранная сторона “ждёт”, ходит другая
                    playerTurn = !playerTurn;
                    continue;
                }

                // фиксируем, что этот юнит свой ход в раунде уже сделал (даже если target == null)
                actedThisRound.add(attacker);
                anyMoveThisRound = true;

                boolean attackerWasAlive = attacker.isAlive();
                Unit target = null;

                // атакует через программу поведения
                if (attackerWasAlive && attacker.getProgram() != null) {
                    target = attacker.getProgram().attack();
                }

                // По ТЗ: после каждой атаки — лог через printBattleLog (target может быть null) :contentReference[oaicite:7]{index=7}
                logAttack(attacker, target);

                boolean deathHappened =
                        (target != null && !target.isAlive()) ||
                                (attackerWasAlive && !attacker.isAlive());

                // По ТЗ: погибшие убираются из очередей в момент смерти, очереди пересчитываются :contentReference[oaicite:8]{index=8}
                if (deathHappened) {
                    playerQueue = buildQueue(playerUnits, actedThisRound);
                    computerQueue = buildQueue(computerUnits, actedThisRound);
                }

                // Чередование ходов
                playerTurn = !playerTurn;
            }

            // защита от бесконечного цикла (на случай странной ситуации с program == null у всех)
            if (!anyMoveThisRound) {
                return;
            }

            // Эти строки не обязательны ТЗ, но соответствуют привычному выводу, который ты показывал
            System.out.println("\nRound " + round + " is over!");
            System.out.println("Player army has " + countAlive(playerUnits) + " units");
            System.out.println("Computer army has " + countAlive(computerUnits) + " units\n");

            round++;
        }
    }

    private static List<Unit> safeList(Army army) {
        if (army == null || army.getUnits() == null) return new ArrayList<>();
        return army.getUnits();
    }

    private static boolean hasAliveAndCanMove(List<Unit> units) {
        for (Unit u : units) {
            if (u != null && u.isAlive() && u.getProgram() != null) {
                return true;
            }
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

    private static Deque<Unit> buildQueue(List<Unit> units, Set<Unit> actedThisRound) {
        List<Unit> candidates = units.stream()
                .filter(Objects::nonNull)
                .filter(Unit::isAlive)
                .filter(u -> u.getProgram() != null)       // “способен сделать ход”
                .filter(u -> !actedThisRound.contains(u))  // ещё не ходил в этом раунде
                .sorted(BY_ATTACK_DESC)
                .collect(Collectors.toList());

        return new ArrayDeque<>(candidates);
    }

    private static boolean chooseFirstTurn(Deque<Unit> playerQueue, Deque<Unit> computerQueue) {
        if (computerQueue.isEmpty()) return true;
        if (playerQueue.isEmpty()) return false;

        Unit p = playerQueue.peekFirst();
        Unit c = computerQueue.peekFirst();

        int pa = (p == null) ? Integer.MIN_VALUE : p.getBaseAttack();
        int ca = (c == null) ? Integer.MIN_VALUE : c.getBaseAttack();

        // если равны — пусть начинает игрок (можно и наоборот, но важно, чтобы было детерминировано)
        return pa >= ca;
    }

    private static Unit pollNextAttacker(boolean playerTurn,
                                         Deque<Unit> playerQueue,
                                         Deque<Unit> computerQueue) {
        if (playerTurn) {
            if (!playerQueue.isEmpty()) return playerQueue.pollFirst();
            if (!computerQueue.isEmpty()) return computerQueue.pollFirst(); // игрок “ждёт”
        } else {
            if (!computerQueue.isEmpty()) return computerQueue.pollFirst();
            if (!playerQueue.isEmpty()) return playerQueue.pollFirst(); // компьютер “ждёт”
        }
        return null;
    }

    private void logAttack(Unit attacker, Unit target) {
        if (printBattleLog != null) {
            try {
                printBattleLog.printBattleLog(attacker, target);
                return;
            } catch (RuntimeException ignored) {
                // если вдруг реализация printBattleLog не принимает null target — не падаем
            }
        }

        // Если нет логгера или он “не принял” target == null
        if (target == null) {
            System.out.println("Unit can not find target for attack!");
        }
    }
}
