import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Collections;
import java.util.Comparator;

public class maxMatch {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java MaxMatch <input_file>");
            return;
        }
        String inputFileName = args[0];
        try {
            List<Graph> graphs = parseGraphs(inputFileName);

            for (int i = 0; i < graphs.size(); i++) {
                Graph graph = graphs.get(i);
                int[] colors = new int[graph.getSize()];

                if (!isBipartite(graph, 0, colors)) {
                    System.out.println("** G" + (i + 1) + ": |V|=" + graph.getSize());
                    System.out.println("Not a bipartite graph");
                    System.out.println(" ");
                    continue;
                }

                Graph bipartiteGraph = createBipartiteGraph(graph, colors);
                int source = graph.getSize(); // Source
                int sink = graph.getSize() + 1; // Sink

                // Start timing
                long startTime = System.currentTimeMillis();

                List<Pair<Integer, Integer>> matches = fordFulkerson(bipartiteGraph, source, sink);

                // End timing
                long endTime = System.currentTimeMillis();

                System.out.println("** G" + (i + 1) + ": |V|=" + graph.getSize());
                // for (Pair<Integer, Integer> pair : matches) {
                // System.out.println("(" + pair.second + " , " + pair.first + ")");
                // }

                // Sort the pairs based on the second Integer in ascending order
                Collections.sort(matches, new Comparator<Pair<Integer, Integer>>() {
                    @Override
                    public int compare(Pair<Integer, Integer> pair1, Pair<Integer, Integer> pair2) {
                        return pair1.second.compareTo(pair2.second);
                    }
                });

                // Print the sorted pairs
                for (Pair<Integer, Integer> pair : matches) {
                    System.out.println("(" + pair.second + ", " + pair.first + ")");
                }

                System.out.println("Matches: " + matches.size() + " pairs" + "(" + (endTime - startTime) + "ms)");
                System.out.println(" ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("*** Asg 8 by Drithi Madagani ***");
    }

    private static boolean isBipartite(Graph graph, int source, int[] colors) {
        Arrays.fill(colors, -1);

        for (int i = 0; i < graph.getSize(); i++) {
            if (colors[i] == -1) {
                if (!bfs(graph, i, colors)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean bfs(Graph graph, int start, int[] colors) {
        Queue<Integer> queue = new LinkedList<>();
        queue.add(start);
        colors[start] = 0;

        while (!queue.isEmpty()) {
            int u = queue.poll();

            for (Edge edge : graph.getEdges()) {
                if (edge.u == u || edge.v == u) {
                    int v = (edge.u == u) ? edge.v : edge.u;

                    if (colors[v] == -1) {
                        colors[v] = 1 - colors[u];
                        queue.add(v);
                    } else if (colors[v] == colors[u]) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static Graph createBipartiteGraph(Graph original, int[] colors) {
        Graph bipartite = new Graph();
        int size = original.getSize();
        int source = size; // Source
        int sink = size + 1; // Sink

        for (Edge edge : original.getEdges()) {
            if (colors[edge.u] != colors[edge.v]) {
                if (colors[edge.u] == 1) {
                    bipartite.addEdge(source, edge.u, 1);
                    bipartite.addEdge(edge.u, edge.v, 1);
                    bipartite.addEdge(edge.v, sink, 1);
                } else {
                    bipartite.addEdge(edge.u, edge.v, 1);
                }
            }
        }

        return bipartite;
    }

    public static List<Pair<Integer, Integer>> fordFulkerson(Graph graph, int source, int sink) {
        Map<Integer, Map<Integer, Double>> residualMap = new HashMap<>();
        for (Edge edge : graph.getEdges()) {
            residualMap.computeIfAbsent(edge.u, k -> new HashMap<>()).put(edge.v, 1.0);
        }

        int[] parent = new int[graph.getSize() + 2];
        List<Pair<Integer, Integer>> matches = new ArrayList<>();

        while (bfs(residualMap, source, sink, parent)) {
            double pathFlow = 1;
            int v = sink;
            List<Pair<Integer, Integer>> currentMatches = new ArrayList<>();

            while (v != source) {
                int u = parent[v];
                residualMap.get(u).put(v, residualMap.get(u).get(v) - pathFlow);
                residualMap.computeIfAbsent(v, k -> new HashMap<>()).merge(u, pathFlow, Double::sum);
                if (u != source && v != sink) {
                    currentMatches.add(new Pair<>(u, v)); // Add the match
                }
                v = u;
            }

            // Reverse the order of matches to have the correct orientation
            Collections.reverse(currentMatches);
            matches.addAll(currentMatches);
        }

        return matches; // Return the list of matches
    }

    static class Pair<T1, T2> {
        T1 first;
        T2 second;

        public Pair(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }
    }

    private static boolean bfs(Map<Integer, Map<Integer, Double>> residualGraph, int source, int sink, int[] parent) {
        BitSet visited = new BitSet();
        Deque<Integer> deque = new ArrayDeque<>();

        Arrays.fill(parent, -1);
        deque.offer(source);
        visited.set(source);

        while (!deque.isEmpty()) {
            int u = deque.poll();
            Map<Integer, Double> edges = residualGraph.get(u);

            if (edges != null) {
                for (Map.Entry<Integer, Double> entry : edges.entrySet()) {
                    int v = entry.getKey();
                    double capacity = entry.getValue();

                    if (!visited.get(v) && capacity > 0) {
                        deque.offer(v);
                        parent[v] = u;
                        visited.set(v);

                        if (v == sink) {
                            return true;
                        }
                    }
                }
            }
        }

        return visited.get(sink);
    }

    private static List<Graph> parseGraphs(String path) throws IOException {
        List<Graph> graphs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            boolean inGraphSection = false;
            Graph currentGraph = null;
            int vertexCount = 0;

            while ((line = reader.readLine()) != null) {
                if (line.matches("\\*\\* G\\d+:.*")) {
                    if (currentGraph != null) {
                        currentGraph.setSize(vertexCount);
                        graphs.add(currentGraph);
                    }
                    currentGraph = new Graph();
                    inGraphSection = true;
                    vertexCount = extractVertexCount(line);
                } else if (inGraphSection && !line.startsWith("-")) {
                    parseGraphEdges(currentGraph, line);
                } else if (inGraphSection && line.startsWith("-")) {
                    inGraphSection = false;
                }
            }

            if (currentGraph != null) {
                currentGraph.setSize(vertexCount);
                graphs.add(currentGraph);
            }
        }
        return graphs;
    }

    private static int extractVertexCount(String line) {
        String[] parts = line.split("\\|V\\|=");
        if (parts.length > 1) {
            String countStr = parts[1].split(",")[0].trim();
            try {
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                // Handle parsing errors
            }
        }
        return 0; // Default value if parsing fails
    }

    private static void parseGraphEdges(Graph graph, String line) {
        if (line.contains("(")) {
            String[] parts = line.substring(line.indexOf('(') + 1, line.indexOf(')')).split(",");
            try {
                int u = Integer.parseInt(parts[0].trim());
                int v = Integer.parseInt(parts[1].trim());
                graph.addEdge(u, v, 1);
                graph.addEdge(v, u, 1); // Since the graph is undirected
            } catch (NumberFormatException e) {
                // Handle parsing errors
            }
        }
    }

    static class Graph {
        private List<Edge> edges;
        private int size;

        public void addEdge(int u, int v, int weight) {
            edges.add(new Edge(u, v)); // Assuming Edge constructor is modified accordingly
            size = Math.max(size, Math.max(u, v) + 1);
        }

        public int getSize() {
            return size;
        }

        public List<Edge> getEdges() {
            return edges;
        }

        public Graph() {
            edges = new ArrayList<>();
        }

        public void setSize(int size) {
            this.size = size;
        }
    }

    static class Edge {
        int u;
        int v;

        Edge(int u, int v) {
            this.u = u;
            this.v = v;
        }
    }
}
