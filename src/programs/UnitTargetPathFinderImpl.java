package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.Edge;
import com.battle.heroes.army.programs.UnitTargetPathFinder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnitTargetPathFinderImpl implements UnitTargetPathFinder {

    private static final int WIDTH = 27;
    private static final int HEIGHT = 21;

    // 8 направлений (включая диагонали)
    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    @Override
    public List<Edge> getTargetPath(Unit attackUnit, Unit targetUnit, List<Unit> existingUnitList) {
        List<Edge> empty = new ArrayList<>();
        if (attackUnit == null || targetUnit == null) return empty;

        int startX = attackUnit.getxCoordinate();
        int startY = attackUnit.getyCoordinate();
        int endX = targetUnit.getxCoordinate();
        int endY = targetUnit.getyCoordinate();

        if (!inBounds(startX, startY) || !inBounds(endX, endY)) return empty;

        // Если уже стоим на цели
        if (startX == endX && startY == endY) {
            List<Edge> path = new ArrayList<>();
            path.add(new Edge(startX, startY));
            return path;
        }

        // blocked[x][y] = занято живым юнитом (кроме атакующего и цели)
        boolean[][] blocked = new boolean[WIDTH][HEIGHT];
        if (existingUnitList != null) {
            for (Unit u : existingUnitList) {
                if (u == null) continue;
                if (!u.isAlive()) continue;
                if (u == attackUnit || u == targetUnit) continue;

                int x = u.getxCoordinate();
                int y = u.getyCoordinate();
                if (inBounds(x, y)) {
                    blocked[x][y] = true;
                }
            }
        }

        // BFS
        boolean[][] visited = new boolean[WIDTH][HEIGHT];
        int[][] prevX = new int[WIDTH][HEIGHT];
        int[][] prevY = new int[WIDTH][HEIGHT];

        // -1 значит "предка нет"
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                prevX[x][y] = -1;
                prevY[x][y] = -1;
            }
        }

        ArrayDeque<int[]> q = new ArrayDeque<>();
        visited[startX][startY] = true;
        q.addLast(new int[]{startX, startY});

        boolean found = false;

        while (!q.isEmpty()) {
            int[] cur = q.pollFirst();
            int x = cur[0];
            int y = cur[1];

            if (x == endX && y == endY) {
                found = true;
                break;
            }

            for (int[] d : DIRECTIONS) {
                int nx = x + d[0];
                int ny = y + d[1];

                if (!inBounds(nx, ny)) continue;
                if (visited[nx][ny]) continue;

                // клетка занята, но целевая клетка допускается (там стоит targetUnit)
                if (blocked[nx][ny] && !(nx == endX && ny == endY)) continue;

                visited[nx][ny] = true;
                prevX[nx][ny] = x;
                prevY[nx][ny] = y;
                q.addLast(new int[]{nx, ny});
            }
        }

        if (!found) return empty;

        // Восстановление пути от цели к старту
        List<Edge> path = new ArrayList<>();
        int cx = endX;
        int cy = endY;

        while (!(cx == startX && cy == startY)) {
            path.add(new Edge(cx, cy));
            int px = prevX[cx][cy];
            int py = prevY[cx][cy];

            // на всякий случай защита от неконсистентности
            if (px == -1 || py == -1) return empty;

            cx = px;
            cy = py;
        }

        path.add(new Edge(startX, startY));
        Collections.reverse(path);
        return path;
    }

    private static boolean inBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }
}
