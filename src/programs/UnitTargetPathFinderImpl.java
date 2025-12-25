package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.Edge;
import com.battle.heroes.army.programs.UnitTargetPathFinder;

import java.util.*;

public class UnitTargetPathFinderImpl implements UnitTargetPathFinder {

    private static final int WIDTH = 27;
    private static final int HEIGHT = 21;

    // Направления движения: 8 направлений (включая диагонали)
    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},   // вверх, вниз, влево, вправо
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}  // диагонали
    };

    @Override
    public List<Edge> getTargetPath(Unit attackUnit, Unit targetUnit, List<Unit> existingUnitList) {
        // Ваше решение

        // Получаем координаты атакующего и целевого юнитов
        int startX = attackUnit.getxCoordinate();
        int startY = attackUnit.getyCoordinate();
        int endX = targetUnit.getxCoordinate();
        int endY = targetUnit.getyCoordinate();

        // Создаём множество занятых клеток (препятствия)
        Set<String> occupied = new HashSet<>();
        for (Unit unit : existingUnitList) {
            if (unit.isAlive() && unit != attackUnit && unit != targetUnit) {
                occupied.add(unit.getxCoordinate() + "," + unit.getyCoordinate());
            }
        }

        // Матрица расстояний и посещённых клеток
        int[][] distance = new int[WIDTH][HEIGHT];
        for (int[] row : distance) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        // Матрица для восстановления пути (хранит предыдущую клетку)
        Edge[][] previous = new Edge[WIDTH][HEIGHT];

        // Приоритетная очередь для алгоритма Дейкстры (BFS с приоритетом)
        PriorityQueue<int[]> queue = new PriorityQueue<>(Comparator.comparingInt(a -> a[2]));

        // Начинаем с позиции атакующего юнита
        distance[startX][startY] = 0;
        queue.offer(new int[]{startX, startY, 0});

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];
            int dist = current[2];

            // Если достигли цели — прекращаем поиск
            if (x == endX && y == endY) {
                break;
            }

            // Пропускаем, если уже нашли более короткий путь
            if (dist > distance[x][y]) {
                continue;
            }

            // Проверяем все соседние клетки
            for (int[] dir : DIRECTIONS) {
                int newX = x + dir[0];
                int newY = y + dir[1];

                // Проверяем границы поля
                if (newX < 0 || newX >= WIDTH || newY < 0 || newY >= HEIGHT) {
                    continue;
                }

                // Проверяем, не занята ли клетка (кроме целевой)
                String key = newX + "," + newY;
                if (occupied.contains(key)) {
                    continue;
                }

                // Вычисляем новое расстояние
                int newDist = dist + 1;

                // Если нашли более короткий путь
                if (newDist < distance[newX][newY]) {
                    distance[newX][newY] = newDist;
                    previous[newX][newY] = new Edge(x, y);
                    queue.offer(new int[]{newX, newY, newDist});
                }
            }
        }

        // Восстанавливаем путь от цели к началу
        List<Edge> path = new ArrayList<>();

        // Если путь не найден
        if (distance[endX][endY] == Integer.MAX_VALUE) {
            return path; // Возвращаем пустой список
        }

        // Восстанавливаем путь в обратном порядке
        int currentX = endX;
        int currentY = endY;

        while (currentX != startX || currentY != startY) {
            path.add(new Edge(currentX, currentY));
            Edge prev = previous[currentX][currentY];
            currentX = prev.getX();
            currentY = prev.getY();
        }

        // Добавляем начальную позицию
        path.add(new Edge(startX, startY));

        // Разворачиваем путь (от начала к цели)
        Collections.reverse(path);

        return path;
    }
}