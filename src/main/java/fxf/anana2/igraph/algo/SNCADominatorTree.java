package fxf.anana2.igraph.algo;

import java.util.stream.Collectors;
import fxf.anana2.igraph.FlowGraph;
import fxf.anana2.igraph.ParentTree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntStack;

class SNCADominatorTree implements ParentTree {
    private final FlowGraph graph;
    private int[] idom;
    private int[] ancestor;
    private int[] semi;
    private int[] label;
    private Int2IntMap preorder;
    private Int2IntMap vertex;
    private IntSet[] pred;

    public static SNCADominatorTree from(FlowGraph graph) {
        return new SNCADominatorTree(graph).run();
    }

    SNCADominatorTree(FlowGraph graph) {
        this.graph = graph;
        this.idom = new int[graph.size()];
        this.ancestor = new int[graph.size()];
        this.semi = new int[graph.size()];
        this.label = new int[graph.size()];
        this.preorder = new Int2IntOpenHashMap(graph.size());
        this.vertex = new Int2IntOpenHashMap(graph.size() + 1);
        this.vertex.put(-1, -1);
        this.pred = new IntSet[graph.size()];
        for (int i = 0; i < pred.length; i++) {
            pred[i] = new IntOpenHashSet();
        }
    }

    @Override
    public int size() {
        return graph.size();
    }

    @Override
    public int root() {
        return vertex.get(0);
    }

    @Override
    public final int parent(int v) {
        return vertex.get(idom[preorder.get(v)]);
    }

    private SNCADominatorTree run() {
        // initialize buffers
        for (int v = 0; v < size(); v++) {
            ancestor[v] = -1;
            label[v] = semi[v] = v;
        }

        dfs();

        // step 1
        // compute semi dominators

        // for v in vertices in reverse preorder
        for (int w = size() - 1; w > 0; w--) {
            idom[w] = ancestor[w];

            for (int v : pred[w]) {

                if (v < 0) {
                    continue;
                }

                if (v > w) {
                    rcompress(v);
                    v = label[v];
                }


                if (semi[v] < semi[w]) {
                    semi[w] = semi[v];
                }
            }

            label[w] = semi[w];
        }

        // step 2
        // compute idom

        // for v in vertices in preorder
        idom[0] = -1;
        for (int v = 1; v < size(); v++) {

            while (idom[v] > semi[v]) {
                idom[v] = idom[idom[v]];
            }
        }

        return this;
    }

    void dfs() {
        int probe = -1;

        IntStack stack = new IntArrayList();
        stack.push(graph.source());
        stack.push(probe);

        while (!stack.isEmpty()) {
            int u = stack.popInt();
            int v = stack.popInt(); // pop node to be visited

            if (preorder.containsKey(v)) {
                pred[preorder.get(v)].add(u);
                continue;
            }

            probe++;

            vertex.put(probe, v);
            preorder.put(v, probe);
            ancestor[probe] = u;
            pred[probe].add(u);
             

            // for each successors w of v in G
            for (int w : graph.succ(v)) {
                stack.push(w);
                stack.push(probe);
            }

        }
    }

    void rcompress(int v) {
        int u = ancestor[v];
        if (u < 0 || ancestor[u] < 0) {
            return;
        }

        rcompress(u);

        if (label[u] < label[v]) {
            label[v] = label[u];
        }
        ancestor[v] = ancestor[u];
    }

    void compress(int v) {
        IntStack stack = new IntArrayList();

        do {
            stack.push(v);
            v = ancestor[v];
        } while (v >= 0 && ancestor[v] >= 0);

        v = stack.popInt();
        while (!stack.isEmpty()) {
            int u = v;
            v = stack.popInt();
            if (label[u] < label[v]) {
                label[v] = label[u];
            }
            ancestor[v] = ancestor[u];
        } ;
    }

    @Override
    public String toString() {
        return preorder.keySet().intStream()
            .sorted()
            .mapToObj(i -> String.format("(%d,%d)", i, parent(i)))
            .collect(Collectors.joining(",", "[", "]"));
    }
}
