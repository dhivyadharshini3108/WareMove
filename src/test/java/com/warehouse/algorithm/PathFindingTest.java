package com.warehouse.algorithm;

import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathFindingTest {

    private Warehouse warehouse;
    private BFSPathFinder bfs;
    private AStarPathFinder astar;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse(6, 6);
        bfs = new BFSPathFinder();
        astar = new AStarPathFinder();
    }

    private void assertValidPathSteps(List<Position> path, Position expectedStart, Position expectedDest) {
        assertNotNull(path);
        assertFalse(path.isEmpty(), "Path should not be empty");
        assertEquals(expectedStart, path.get(0), "Path must begin at start position");
        assertEquals(expectedDest, path.get(path.size() - 1), "Path must end at destination position");

        for (int i = 0; i < path.size() - 1; i++) {
            Position current = path.get(i);
            Position next = path.get(i + 1);
            assertEquals(1, current.manhattanDistance(next),
                    "Consecutive steps must be orthogonally adjacent: " + current + " -> " + next);
        }
    }

    @Test
    @DisplayName("Straight-line path finding for BFS and A*")
    void testStraightLinePath() {
        Position start = new Position(0, 0);
        Position dest = new Position(0, 4);

        List<Position> bfsPath = bfs.findPath(warehouse, start, dest);
        List<Position> astarPath = astar.findPath(warehouse, start, dest);

        assertValidPathSteps(bfsPath, start, dest);
        assertValidPathSteps(astarPath, start, dest);

        assertEquals(5, bfsPath.size(), "Straight line path of length 4 steps should contain 5 nodes");
        assertEquals(5, astarPath.size());
        assertEquals(bfsPath.size(), astarPath.size());
    }

    @Test
    @DisplayName("Paths navigating around obstacle walls")
    void testPathAroundObstacles() {
        // Create a vertical wall from (2, 0) to (2, 3)
        for (int y = 0; y <= 3; y++) {
            warehouse.setObstacle(2, y, true);
        }

        Position start = new Position(1, 1);
        Position dest = new Position(3, 1);

        List<Position> bfsPath = bfs.findPath(warehouse, start, dest);
        List<Position> astarPath = astar.findPath(warehouse, start, dest);

        assertValidPathSteps(bfsPath, start, dest);
        assertValidPathSteps(astarPath, start, dest);

        // Verify no position in either path is an obstacle
        for (Position p : bfsPath) {
            assertFalse(warehouse.getCell(p).isObstacle(), "Path node cannot be an obstacle: " + p);
        }
        for (Position p : astarPath) {
            assertFalse(warehouse.getCell(p).isObstacle(), "Path node cannot be an obstacle: " + p);
        }

        // Both algorithms must find shortest paths of identical length
        assertEquals(bfsPath.size(), astarPath.size(), "A* and BFS path lengths must agree");
    }

    @Test
    @DisplayName("Start equals destination returns single-element path")
    void testStartEqualsDestination() {
        Position pos = new Position(2, 3);

        List<Position> bfsPath = bfs.findPath(warehouse, pos, pos);
        List<Position> astarPath = astar.findPath(warehouse, pos, pos);

        assertEquals(List.of(pos), bfsPath);
        assertEquals(List.of(pos), astarPath);
    }

    @Test
    @DisplayName("Unreachable destination returns empty list")
    void testUnreachableDestination() {
        // Enclose destination (4, 4) with obstacles
        warehouse.setObstacle(3, 4, true);
        warehouse.setObstacle(5, 4, true);
        warehouse.setObstacle(4, 3, true);
        warehouse.setObstacle(4, 5, true);

        Position start = new Position(0, 0);
        Position dest = new Position(4, 4);

        List<Position> bfsPath = bfs.findPath(warehouse, start, dest);
        List<Position> astarPath = astar.findPath(warehouse, start, dest);

        assertTrue(bfsPath.isEmpty(), "BFS should return empty list for unreachable target");
        assertTrue(astarPath.isEmpty(), "A* should return empty list for unreachable target");
    }

    @Test
    @DisplayName("Invalid or blocked start and destination positions")
    void testInvalidOrBlockedPositions() {
        Position validPos = new Position(1, 1);
        Position outOfBounds = new Position(-1, 0);
        Position outOfBounds2 = new Position(10, 10);

        // Null inputs
        assertTrue(bfs.findPath(null, validPos, validPos).isEmpty());
        assertTrue(bfs.findPath(warehouse, null, validPos).isEmpty());
        assertTrue(bfs.findPath(warehouse, validPos, null).isEmpty());
        assertTrue(astar.findPath(null, validPos, validPos).isEmpty());
        assertTrue(astar.findPath(warehouse, null, validPos).isEmpty());
        assertTrue(astar.findPath(warehouse, validPos, null).isEmpty());

        // Out of bounds
        assertTrue(bfs.findPath(warehouse, outOfBounds, validPos).isEmpty());
        assertTrue(bfs.findPath(warehouse, validPos, outOfBounds2).isEmpty());
        assertTrue(astar.findPath(warehouse, outOfBounds, validPos).isEmpty());
        assertTrue(astar.findPath(warehouse, validPos, outOfBounds2).isEmpty());

        // Blocked start or blocked destination
        Position blocked = new Position(2, 2);
        warehouse.setObstacle(blocked, true);

        assertTrue(bfs.findPath(warehouse, blocked, validPos).isEmpty());
        assertTrue(bfs.findPath(warehouse, validPos, blocked).isEmpty());
        assertTrue(astar.findPath(warehouse, blocked, validPos).isEmpty());
        assertTrue(astar.findPath(warehouse, validPos, blocked).isEmpty());
    }

    @Test
    @DisplayName("A* and BFS shortest-path lengths agree on multiple unweighted grid scenarios")
    void testShortestPathAgreement() {
        // Add random/maze-like obstacles
        warehouse.setObstacle(1, 1, true);
        warehouse.setObstacle(1, 2, true);
        warehouse.setObstacle(3, 2, true);
        warehouse.setObstacle(3, 3, true);
        warehouse.setObstacle(3, 4, true);

        Position[][] testPairs = {
                {new Position(0, 0), new Position(5, 5)},
                {new Position(0, 3), new Position(5, 1)},
                {new Position(2, 0), new Position(4, 5)},
                {new Position(0, 5), new Position(5, 0)}
        };

        for (Position[] pair : testPairs) {
            Position start = pair[0];
            Position dest = pair[1];

            List<Position> bfsResult = bfs.findPath(warehouse, start, dest);
            List<Position> astarResult = astar.findPath(warehouse, start, dest);

            if (bfsResult.isEmpty()) {
                assertTrue(astarResult.isEmpty(), "A* must also find no path if BFS finds no path");
            } else {
                assertValidPathSteps(bfsResult, start, dest);
                assertValidPathSteps(astarResult, start, dest);
                assertEquals(bfsResult.size(), astarResult.size(),
                        "BFS and A* must return identical shortest path length between " + start + " and " + dest);
            }
        }
    }
}
